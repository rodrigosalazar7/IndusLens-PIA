package com.identificador.industrial.ui.pantallas

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.identificador.industrial.datos.RepositorioBusquedas
import com.identificador.industrial.datos.RepositorioMateriales
import com.identificador.industrial.datos.RepositorioVisual
import com.identificador.industrial.datos.modelo.Coincidencia
import com.identificador.industrial.datos.modelo.Material
import com.identificador.industrial.datos.modelo.MetodoIdentificacion
import com.identificador.industrial.ia.ClasificadorGenerico
import com.identificador.industrial.ia.Embebedor
import com.identificador.industrial.ia.ExtractorNumeroParte
import com.identificador.industrial.ia.Fotos
import com.identificador.industrial.ia.LectorTexto
import com.identificador.industrial.ia.Pista
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class PasoAnalisis(val etiqueta: String) {
    LEYENDO("Leyendo el texto de la pieza"),
    BUSCANDO("Buscando el numero de parte en el catalogo"),
    COMPARANDO("Comparando la forma con el catalogo")
}

enum class RazonFallo {
    /** No habia ningun texto legible y tampoco huellas visuales que comparar. */
    SIN_TEXTO,

    /** Se leyo texto, pero ningun candidato correspondia a un material. */
    SIN_COINCIDENCIA,

    /** El catalogo aun no tiene ninguna pieza con fotografia de referencia. */
    SIN_HUELLAS
}

sealed interface EstadoIdentificacion {
    data object Inactivo : EstadoIdentificacion

    data class Analizando(val paso: PasoAnalisis) : EstadoIdentificacion

    data class Identificado(
        val material: Material,
        val metodo: MetodoIdentificacion,
        val confianza: Float,
        val textoLeido: String,
        /** Solo cuando se identifico leyendo la pieza. */
        val numeroReconocido: String? = null
    ) : EstadoIdentificacion

    /** Hay parecidos, pero ninguno lo bastante claro para darlo por seguro. */
    data class Similares(
        val coincidencias: List<Coincidencia>,
        val textoLeido: String,
        val pistas: List<Pista> = emptyList()
    ) : EstadoIdentificacion

    data class NoIdentificado(
        val razon: RazonFallo,
        val textoLeido: String,
        /** Que cree el modelo generico que es, cuando no se reconocio la pieza. */
        val pistas: List<Pista> = emptyList()
    ) : EstadoIdentificacion

    data class Fallo(val mensaje: String) : EstadoIdentificacion
}

/**
 * Coordina el recorrido camara -> analisis -> resultado.
 *
 * La estrategia es en cascada, de mas fiable a menos:
 *
 *   1. Leer el texto de la pieza (OCR) y buscar el numero de parte.
 *   2. Si no hubo suerte, calcular la huella visual y comparar con el catalogo.
 *   3. Si el parecido es muy alto se da por identificada; si es dudoso se
 *      ofrecen las candidatas y decide la persona.
 *
 * El orden importa: un numero de parte leido correctamente identifica la pieza
 * sin ambiguedad, mientras que el parecido visual entre un rodamiento 6205 y
 * un 6203 es practicamente total. Por eso el parecido nunca sobrescribe una
 * lectura de texto acertada.
 */
class IdentificacionViewModel(
    private val repositorioMateriales: RepositorioMateriales,
    private val repositorioBusquedas: RepositorioBusquedas,
    private val repositorioVisual: RepositorioVisual,
    /**
     * Se recibe como funcion, no como objeto ya creado.
     *
     * Crear el Embebedor carga el modelo de MobileNet desde el APK, y eso
     * tarda mas de un segundo. Si se pasara ya construido, esa carga ocurriria
     * al crear el ViewModel, es decir en el hilo principal durante el arranque
     * de la app, congelando la interfaz. Asi se difiere hasta que se necesita
     * de verdad, y ademas ocurre en un hilo de fondo.
     */
    private val obtenerEmbebedor: () -> Embebedor,
    /** Igual que el embebedor: se difiere para no cargar el modelo al arrancar. */
    private val obtenerClasificador: () -> ClasificadorGenerico
) : ViewModel() {

    private val lector = LectorTexto()

    var fotoUri by mutableStateOf<Uri?>(null)
        private set

    /** Ultimas candidatas visuales, para la pantalla de piezas similares. */
    var coincidencias by mutableStateOf<List<Coincidencia>>(emptyList())
        private set

    private val _estado = MutableStateFlow<EstadoIdentificacion>(EstadoIdentificacion.Inactivo)
    val estado: StateFlow<EstadoIdentificacion> = _estado.asStateFlow()

    fun registrarFoto(uri: Uri) {
        fotoUri = uri
        coincidencias = emptyList()
        _estado.value = EstadoIdentificacion.Inactivo
    }

    private var trabajo: Job? = null

    fun analizar(contexto: Context, usuarioId: String) {
        val uri = fotoUri ?: run {
            _estado.value = EstadoIdentificacion.Fallo("No hay ninguna fotografia que analizar")
            return
        }

        // Se cancela y se reemplaza cualquier analisis anterior, en lugar de
        // ignorar la peticion nueva. Antes se hacia al reves y tenia un fallo
        // grave: si un analisis moria a medias, el estado se quedaba en
        // "analizando" y ningun intento posterior podia arrancar. La pantalla
        // se quedaba girando para siempre.
        trabajo?.cancel()
        trabajo = viewModelScope.launch {
            try {
                // El limite de tiempo garantiza que la pantalla de espera
                // siempre termina en algo, pase lo que pase por debajo.
                withTimeout(LIMITE_ANALISIS_MS) {
                    ejecutar(contexto, usuarioId, uri)
                }
            } catch (e: TimeoutCancellationException) {
                _estado.value = EstadoIdentificacion.Fallo(
                    "El analisis tardo demasiado y se detuvo. Intentalo otra vez " +
                        "con la pieza mas cerca y mejor iluminada."
                )
            } catch (e: CancellationException) {
                // Cancelacion normal (otra foto, o se salio de la pantalla).
                throw e
            } catch (e: Throwable) {
                // A proposito se capturan Throwable y no solo Exception: los
                // fallos nativos de TensorFlow Lite y la falta de memoria
                // llegan como Error, que no es una Exception. Si se dejan
                // escapar, la corrutina muere en silencio y la pantalla de
                // espera se queda colgada sin explicar nada.
                Log.e(ETIQUETA, "Fallo al analizar la fotografia", e)
                _estado.value = EstadoIdentificacion.Fallo(
                    e.message ?: "No se pudo analizar la fotografia"
                )
            }
        }
    }

    private suspend fun ejecutar(contexto: Context, usuarioId: String, uri: Uri) {
        // --- Paso 1: leer el texto de la pieza ---
        _estado.value = EstadoIdentificacion.Analizando(PasoAnalisis.LEYENDO)
        val texto = lector.leer(contexto, uri)

        // Pausa breve solo para que el cambio de paso sea perceptible.
        delay(300)

        // --- Paso 2: buscar el numero de parte ---
        _estado.value = EstadoIdentificacion.Analizando(PasoAnalisis.BUSCANDO)
        val candidatos = ExtractorNumeroParte.candidatos(texto)
        val porTexto = candidatos.firstNotNullOfOrNull { candidato ->
            repositorioMateriales.buscarPorNumeroParte(candidato)
                ?.let { material -> material to candidato }
        }

        if (porTexto != null) {
            val (material, numero) = porTexto
            registrar(material.id, usuarioId, MetodoIdentificacion.OCR, CONFIANZA_OCR, texto, uri)
            _estado.value = EstadoIdentificacion.Identificado(
                material = material,
                metodo = MetodoIdentificacion.OCR,
                confianza = CONFIANZA_OCR,
                textoLeido = texto,
                numeroReconocido = numero
            )
            return
        }

        // --- Paso 3: comparar la forma con el catalogo ---
        delay(250)
        _estado.value = EstadoIdentificacion.Analizando(PasoAnalisis.COMPARANDO)

        // Se carga UNA vez la imagen recortada al marco guia y se reutiliza
        // para comparar y para clasificar. Recortar es imprescindible: sin
        // ello el modelo describe la nave o la mesa en vez de la pieza.
        val imagen = withContext(Dispatchers.Default) {
            Fotos.cargarParaAnalisis(contexto, uri)
        }

        val parecidas = if (imagen == null) emptyList() else withContext(Dispatchers.Default) {
            val vector = obtenerEmbebedor().vector(imagen)
            repositorioVisual.buscarParecidos(vector, limite = 5)
        }
        coincidencias = parecidas

        // Parecido suficientemente alto: se da por identificada y se termina.
        if (parecidas.isNotEmpty() && parecidas.first().similitud >= UMBRAL_CERTEZA) {
            val mejor = parecidas.first()
            registrar(
                mejor.material.id, usuarioId, MetodoIdentificacion.VISUAL,
                mejor.similitud, texto, uri
            )
            _estado.value = EstadoIdentificacion.Identificado(
                material = mejor.material,
                metodo = MetodoIdentificacion.VISUAL,
                confianza = mejor.similitud,
                textoLeido = texto
            )
            return
        }

        // --- Paso 4: pista generica ---
        // La pieza no se reconoce. Antes de rendirse se pregunta al modelo
        // generico que tipo de objeto ve, para poder acotar el catalogo a una
        // familia y que la persona elija entre pocas piezas en vez de cientos.
        // Si esto falla no importa: es una ayuda, no un requisito.
        val pistas = if (imagen == null) emptyList() else try {
            withContext(Dispatchers.Default) { obtenerClasificador().analizar(imagen) }
        } catch (e: Throwable) {
            Log.w(ETIQUETA, "No se pudo obtener la pista generica", e)
            emptyList()
        }

        _estado.value = if (parecidas.isNotEmpty()) {
            registrar(null, usuarioId, MetodoIdentificacion.VISUAL, parecidas.first().similitud, texto, uri)
            EstadoIdentificacion.Similares(parecidas, texto, pistas)
        } else {
            registrar(null, usuarioId, MetodoIdentificacion.VISUAL, 0f, texto, uri)
            EstadoIdentificacion.NoIdentificado(
                razon = when {
                    repositorioVisual.cuantasHuellas() == 0 -> RazonFallo.SIN_HUELLAS
                    texto.isBlank() -> RazonFallo.SIN_TEXTO
                    else -> RazonFallo.SIN_COINCIDENCIA
                },
                textoLeido = texto,
                pistas = pistas
            )
        }
    }

    /**
     * La persona senala en el catalogo cual era la pieza.
     *
     * Ademas de resolver la busqueda, la fotografia se guarda como vista de
     * referencia de ese material. Esto es lo que hace que la app mejore sola
     * con el uso: cada correccion humana le ensena una pieza mas, y la
     * siguiente vez la reconoce sin ayuda.
     */
    fun confirmarPieza(contexto: Context, materialId: String, usuarioId: String) {
        val uri = fotoUri ?: return

        viewModelScope.launch {
            try {
                val material = repositorioMateriales.obtenerPorId(materialId) ?: return@launch

                withContext(Dispatchers.Default) {
                    // Mismo recorte que en la identificacion. Si aqui se
                    // guardara la foto entera y alli se comparara el recorte,
                    // los vectores no serian comparables y la pieza aprendida
                    // no se reconoceria nunca.
                    val imagen = Fotos.cargarParaAnalisis(contexto, uri)
                    if (imagen != null) {
                        val vector = obtenerEmbebedor().vector(imagen)
                        repositorioVisual.guardarHuella(materialId, vector)
                    }
                }

                registrar(materialId, usuarioId, MetodoIdentificacion.MANUAL, 1f, null, uri)
                _estado.value = EstadoIdentificacion.Identificado(
                    material = material,
                    metodo = MetodoIdentificacion.MANUAL,
                    confianza = 1f,
                    textoLeido = ""
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                // Que falle el aprendizaje no debe impedir resolver la busqueda:
                // lo importante para quien esta en el almacen es saber que pieza
                // es y donde esta.
                Log.e(ETIQUETA, "No se pudo aprender la pieza confirmada", e)
                repositorioMateriales.obtenerPorId(materialId)?.let { material ->
                    _estado.value = EstadoIdentificacion.Identificado(
                        material = material,
                        metodo = MetodoIdentificacion.MANUAL,
                        confianza = 1f,
                        textoLeido = ""
                    )
                }
            }
        }
    }

    private suspend fun registrar(
        materialId: String?,
        usuarioId: String,
        metodo: MetodoIdentificacion,
        confianza: Float,
        texto: String?,
        uri: Uri
    ) {
        repositorioBusquedas.registrar(
            materialId = materialId,
            usuarioId = usuarioId,
            metodo = metodo,
            confianza = confianza,
            textoDetectado = texto,
            fotoPath = uri.toString()
        )
    }

    fun reiniciar() {
        fotoUri = null
        coincidencias = emptyList()
        _estado.value = EstadoIdentificacion.Inactivo
    }

    override fun onCleared() {
        super.onCleared()
        // El lector es propio de este ViewModel y se cierra. El embebedor no:
        // pertenece a la aplicacion y lo comparten otras pantallas.
        // El lector es propio de este ViewModel. El embebedor y el clasificador
        // no: pertenecen a la aplicacion y los comparten otras pantallas.
        lector.cerrar()
    }

    companion object {

        private const val ETIQUETA = "Identificacion"

        /**
         * Tope de tiempo para todo el analisis.
         *
         * Es una red de seguridad: aunque el OCR o el modelo se quedaran
         * bloqueados, la pantalla de espera termina siempre en algo que el
         * usuario puede entender y reintentar. El primer analisis en un
         * emulador puede tardar bastante mas que en un telefono porque debe
         * cargar y compilar los modelos nativos; sesenta segundos evita
         * cancelarlo prematuramente sin permitir una espera infinita.
         */
        private const val LIMITE_ANALISIS_MS = 60_000L

        /** Una lectura correcta del numero de parte no deja lugar a dudas. */
        const val CONFIANZA_OCR = 0.99f

        /**
         * A partir de aqui el parecido se considera identificacion.
         *
         * El valor es conservador a proposito. Equivocarse en un almacen no
         * cuesta un clic: cuesta que alguien monte la pieza incorrecta. Ante
         * la duda es preferible mostrar varias candidatas y que decida quien
         * tiene la pieza en la mano.
         */
        const val UMBRAL_CERTEZA = 0.88f
    }
}
