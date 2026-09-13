package com.identificador.industrial.ui.pantallas

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.identificador.industrial.datos.modelo.Material
import com.identificador.industrial.datos.modelo.MetodoIdentificacion
import com.identificador.industrial.ia.Fotos
import com.identificador.industrial.ia.OrigenPista
import com.identificador.industrial.ia.Pista
import com.identificador.industrial.ui.componentes.Insignia
import com.identificador.industrial.ui.componentes.PantallaBase
import com.identificador.industrial.ui.theme.AmarilloAviso
import com.identificador.industrial.ui.theme.VerdeExito
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Pantalla 5: resultado de identificacion. */
@Composable
fun PantallaResultado(
    identificacion: IdentificacionViewModel,
    onVolver: () -> Unit,
    onVerDetalle: (String) -> Unit,
    onVerSimilares: () -> Unit,
    onRepetirFoto: () -> Unit,
    onElegirPieza: () -> Unit,
    onEsPiezaNueva: () -> Unit,
    onMedir: () -> Unit
) {
    val estado by identificacion.estado.collectAsStateWithLifecycle()
    val foto = identificacion.fotoUri

    PantallaBase(titulo = "Resultado", onVolver = onVolver) { modifier ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (foto != null) FotoTomada(foto)

            when (val actual = estado) {
                is EstadoIdentificacion.Identificado -> Acierto(
                    material = actual.material,
                    metodo = actual.metodo,
                    confianza = actual.confianza,
                    numeroReconocido = actual.numeroReconocido,
                    onVerDetalle = onVerDetalle,
                    onVerSimilares = onVerSimilares
                )

                is EstadoIdentificacion.Similares -> HayParecidas(
                    cuantas = actual.coincidencias.size,
                    mejorPorcentaje = actual.coincidencias.first().porcentaje,
                    pistas = actual.pistas,
                    onVerSimilares = onVerSimilares,
                    onRepetirFoto = onRepetirFoto,
                    onElegirPieza = onElegirPieza,
                    onEsPiezaNueva = onEsPiezaNueva
                )

                is EstadoIdentificacion.NoIdentificado -> SinAcierto(
                    razon = actual.razon,
                    textoLeido = actual.textoLeido,
                    pistas = actual.pistas,
                    onRepetirFoto = onRepetirFoto,
                    onElegirPieza = onElegirPieza,
                    onEsPiezaNueva = onEsPiezaNueva
                )

                is EstadoIdentificacion.Fallo -> Aviso(
                    titulo = "No se pudo analizar",
                    cuerpo = actual.mensaje,
                    onRepetirFoto = onRepetirFoto
                )

                else -> Aviso(
                    titulo = "Sin analisis",
                    cuerpo = "Todavia no se ha analizado ninguna fotografia.",
                    onRepetirFoto = onRepetirFoto
                )
            }

            if (foto != null) {
                OutlinedButton(
                    onClick = onMedir,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text("Medir la pieza en la foto")
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun FotoTomada(uri: Uri) {
    val contexto = LocalContext.current
    // Se decodifica fuera del hilo principal: una foto de camara son varios
    // megapixeles y hacerlo en la interfaz produce un tiron visible.
    val mapa by produceState<Bitmap?>(initialValue = null, uri) {
        value = withContext(Dispatchers.IO) { Fotos.cargarReducida(contexto, uri) }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        mapa?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = "Fotografia tomada de la pieza",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun Acierto(
    material: Material,
    metodo: MetodoIdentificacion,
    confianza: Float,
    numeroReconocido: String?,
    onVerDetalle: (String) -> Unit,
    onVerSimilares: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Insignia(texto = "Identificada", color = VerdeExito)
        Insignia(
            texto = "${(confianza * 100).toInt()}% de confianza",
            color = if (metodo == MetodoIdentificacion.OCR) {
                MaterialTheme.colorScheme.secondary
            } else {
                AmarilloAviso
            }
        )
    }

    Text(
        text = material.nombre,
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onBackground
    )

    Tarjeta {
        Text(
            text = "Numero de parte",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = material.numeroParte,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(12.dp))
        // Se explica siempre COMO se llego a esta pieza. Un parecido visual y
        // una lectura del numero grabado no merecen la misma confianza, y quien
        // va a tomar el material tiene derecho a saber cual de las dos fue.
        Text(
            text = if (numeroReconocido != null) {
                "Se reconocio leyendo \"$numeroReconocido\" en la pieza"
            } else {
                "Se reconocio por parecido visual, sin leer ningun numero. " +
                    "Confirma el numero de parte antes de tomarla."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = if (numeroReconocido != null) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                AmarilloAviso
            }
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "${material.fabricante}  ·  ${material.ubicacion.codigo}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        // Las medidas salen del catalogo, no de medir la foto: son el dato
        // del fabricante y por tanto exactas.
        material.medidas?.let { medidas ->
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Medidas",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = medidas,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }

    Button(
        onClick = { onVerDetalle(material.id) },
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        Text("Ver detalle del material", style = MaterialTheme.typography.labelLarge)
    }

    OutlinedButton(
        onClick = onVerSimilares,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
    ) {
        Text("No es esta, ver piezas similares")
    }
}

/**
 * Ninguna candidata alcanza el umbral de certeza, pero hay parecidos.
 * Se ofrece la lista en vez de arriesgar una respuesta que podria ser falsa.
 */
@Composable
private fun HayParecidas(
    cuantas: Int,
    mejorPorcentaje: Int,
    pistas: List<Pista>,
    onVerSimilares: () -> Unit,
    onRepetirFoto: () -> Unit,
    onElegirPieza: () -> Unit,
    onEsPiezaNueva: () -> Unit
) {
    val objetoReconocido = pistas.firstOrNull()

    Insignia(
        texto = if (objetoReconocido == null) "Sin certeza" else "Objeto reconocido por IA",
        color = if (objetoReconocido == null) AmarilloAviso else MaterialTheme.colorScheme.secondary
    )

    Text(
        text = objetoReconocido?.let { pista ->
            "La IA reconoce: ${pista.etiqueta.replaceFirstChar { it.uppercase() }}"
        } ?: "No se pudo confirmar la pieza",
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground
    )

    Text(
        text = if (objetoReconocido != null) {
            "El objeto general si fue reconocido, pero todavia no alcanza para " +
                "confirmar una pieza exacta del inventario. El mayor parecido " +
                "del catalogo es $mejorPorcentaje%."
        } else {
            "No se leyo ningun numero de parte utilizable, y el mayor " +
                "parecido visual es del $mejorPorcentaje%, insuficiente para " +
                "darla por segura. " + if (cuantas == 1) {
                "Hay una pieza candidata."
            } else {
                "Hay $cuantas piezas candidatas."
            }
        },
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Button(
        onClick = onVerSimilares,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        Text(
            text = if (cuantas == 1) "Ver la pieza parecida" else "Ver las $cuantas piezas parecidas",
            style = MaterialTheme.typography.labelLarge
        )
    }

    PistasIA(pistas)

    OutlinedButton(
        onClick = onElegirPieza,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
    ) {
        Text("Decirle yo que pieza es")
    }

    // No esta en el catalogo, pero la foto ya sirve como su primera huella:
    // se guarda al dar de alta, sin tener que repetirla.
    OutlinedButton(
        onClick = onEsPiezaNueva,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
    ) {
        Text("No esta en el catalogo, darla de alta")
    }

    OutlinedButton(
        onClick = onRepetirFoto,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
    ) {
        Text("Repetir la fotografia")
    }
}

/**
 * Reconocimiento general real. Puede venir de Firebase AI si la persona
 * autorizo el envio del recorte, o del modelo EfficientNet dentro del equipo.
 */
@Composable
private fun PistasIA(pistas: List<Pista>) {
    Tarjeta {
        Text(
            text = "Reconocimiento general",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(10.dp))

        if (pistas.isEmpty()) {
            // Preferible admitirlo a soltar etiquetas al 15% que no significan
            // nada. El modelo generico solo conoce formas corrientes: tornillos,
            // cadenas, herramientas. Ante una tuerca o un rodamiento, no sabe.
            Text(
                text = "No reconozco de que tipo es esta pieza.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "El modelo general distingue formas corrientes como " +
                    "tornillos, cadenas o herramientas. Con tuercas, rodamientos " +
                    "o material electrico se pierde. Senala cual es y la aprendo.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@Tarjeta
        }
        pistas.forEach { pista ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = pista.etiqueta,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (pista.origen == OrigenPista.FIREBASE_AI) {
                        "IA en linea"
                    } else {
                        "${(pista.confianza * 100).toInt()}%"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Es reconocimiento real de la fotografia. Identifica el tipo " +
                "de objeto, pero no inventa marca, medida ni numero de parte.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SinAcierto(
    razon: RazonFallo,
    textoLeido: String,
    pistas: List<Pista>,
    onRepetirFoto: () -> Unit,
    onElegirPieza: () -> Unit,
    onEsPiezaNueva: () -> Unit
) {
    val objetoReconocido = pistas.firstOrNull()

    Insignia(
        texto = if (objetoReconocido == null) "No identificada" else "Objeto reconocido por IA",
        color = if (objetoReconocido == null) AmarilloAviso else MaterialTheme.colorScheme.secondary
    )

    Text(
        text = objetoReconocido?.let { pista ->
            "La IA reconoce: ${pista.etiqueta.replaceFirstChar { it.uppercase() }}"
        } ?: when (razon) {
            RazonFallo.SIN_TEXTO -> "No se leyo ningun texto en la pieza"
            RazonFallo.SIN_COINCIDENCIA -> "Se leyo texto, pero no coincide con el catalogo"
            RazonFallo.SIN_HUELLAS -> "Todavia no hay piezas que comparar"
        },
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground
    )

    Text(
        text = if (objetoReconocido != null) {
            val familia = objetoReconocido.categoria?.etiqueta?.let { " Familia: $it." }.orEmpty()
            if (objetoReconocido.origen == OrigenPista.FIREBASE_AI) {
                "Firebase AI reconocio el objeto usando Internet.$familia Como " +
                    "aun no coincide con una pieza exacta del catalogo, puedes " +
                    "seleccionarla para que IndusLens aprenda esta fotografia."
            } else {
                "El modelo visual dentro del telefono identifico el objeto con " +
                    "${(objetoReconocido.confianza * 100).toInt()}% de confianza.$familia " +
                    "Como aun no coincide con una pieza exacta del catalogo, puedes " +
                    "seleccionarla para que IndusLens aprenda esta fotografia."
            }
        } else when (razon) {
            RazonFallo.SIN_TEXTO ->
                "La pieza puede no tener marcas legibles, o la foto quedo lejos, " +
                    "movida o con poca luz. Acerca el numero de parte y vuelve a intentarlo."

            RazonFallo.SIN_COINCIDENCIA ->
                "Puede que la pieza no este dada de alta en el catalogo, o que el " +
                    "numero se haya leido con algun caracter equivocado."

            RazonFallo.SIN_HUELLAS ->
                "Ninguna pieza del catalogo tiene todavia fotografia de referencia, " +
                    "asi que no hay con que comparar. Senala cual es y la aprendere: " +
                    "la proxima vez la reconocera sola."
        },
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    if (textoLeido.isNotBlank()) {
        Tarjeta {
            Text(
                text = "Texto leido en la pieza",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = textoLeido.trim(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }

    PistasIA(pistas)

    // Accion principal: que la persona resuelva y de paso ensene a la app.
    Button(
        onClick = onElegirPieza,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        Text("Decirle yo que pieza es", style = MaterialTheme.typography.labelLarge)
    }

    // Cuando la pieza sencillamente no esta en el catalogo (por ejemplo, una
    // herramienta que la IA general si reconoce pero que nadie ha dado de
    // alta), esta es la salida: se registra y la foto que ya se tomo queda
    // como su primera huella, sin repetirla.
    OutlinedButton(
        onClick = onEsPiezaNueva,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
    ) {
        Text("No esta en el catalogo, darla de alta")
    }

    OutlinedButton(
        onClick = onRepetirFoto,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
    ) {
        Text("Repetir la fotografia")
    }
}

@Composable
private fun Aviso(titulo: String, cuerpo: String, onRepetirFoto: () -> Unit) {
    Text(
        text = titulo,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground
    )
    Text(
        text = cuerpo,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Start
    )
    Button(
        onClick = onRepetirFoto,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        Text("Tomar una fotografia")
    }
}

@Composable
private fun Tarjeta(contenido: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(18.dp)
    ) {
        contenido()
    }
}
