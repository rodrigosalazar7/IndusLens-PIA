package com.identificador.industrial.ui.pantallas

import com.identificador.industrial.ui.theme.Espaciado
import com.identificador.industrial.ui.theme.TamanosControles
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.foundation.verticalScroll
import com.identificador.industrial.ui.componentes.BotonPrincipal
import androidx.compose.material3.MaterialTheme
import com.identificador.industrial.ui.componentes.BotonSecundario
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
    onMedir: () -> Unit
) {
    val estado by identificacion.estado.collectAsStateWithLifecycle()
    val foto = identificacion.fotoUri

    PantallaBase(titulo = "Resultado", onVolver = onVolver) { modifier ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(Espaciado.pantalla),
            verticalArrangement = Arrangement.spacedBy(Espaciado.normal)
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
                    onElegirPieza = onElegirPieza
                )

                is EstadoIdentificacion.NoIdentificado -> SinAcierto(
                    razon = actual.razon,
                    textoLeido = actual.textoLeido,
                    pistas = actual.pistas,
                    onRepetirFoto = onRepetirFoto,
                    onElegirPieza = onElegirPieza
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
                BotonSecundario(
                    onClick = onMedir,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = TamanosControles.alturaMinima)
                ) {
                    Text("Medir la pieza en la foto")
                }
                Spacer(Modifier.height(Espaciado.pequeno))
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
            .clip(MaterialTheme.shapes.large)
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
    Row(horizontalArrangement = Arrangement.spacedBy(Espaciado.pequeno)) {
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
        Spacer(Modifier.height(Espaciado.medio))
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
            Spacer(Modifier.height(Espaciado.medio))
            Text(
                text = "Medidas",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(Espaciado.minimo))
            Text(
                text = medidas,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }

    BotonPrincipal(
        onClick = { onVerDetalle(material.id) },
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = TamanosControles.alturaMinima)
    ) {
        Text("Ver detalle del material", style = MaterialTheme.typography.labelLarge)
    }

    BotonSecundario(
        onClick = onVerSimilares,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = TamanosControles.alturaMinima)
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
    onElegirPieza: () -> Unit
) {
    Insignia(texto = "Sin certeza", color = AmarilloAviso)

    Text(
        text = "No se pudo confirmar la pieza",
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground
    )

    Text(
        text = "No se leyo ningun numero de parte utilizable, y el mayor " +
            "parecido visual es del $mejorPorcentaje%, insuficiente para darla " +
            "por segura. " +
            if (cuantas == 1) "Hay una pieza candidata." else "Hay $cuantas piezas candidatas.",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    BotonPrincipal(
        onClick = onVerSimilares,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = TamanosControles.alturaMinima)
    ) {
        Text(
            text = if (cuantas == 1) "Ver la pieza parecida" else "Ver las $cuantas piezas parecidas",
            style = MaterialTheme.typography.labelLarge
        )
    }

    PistasIA(pistas)

    BotonSecundario(
        onClick = onElegirPieza,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = TamanosControles.alturaMinima)
    ) {
        Text("Decirle yo que pieza es")
    }

    BotonSecundario(
        onClick = onRepetirFoto,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = TamanosControles.alturaMinima)
    ) {
        Text("Repetir la fotografia")
    }
}

/**
 * Lo que el modelo generico cree ver. Se muestra como pista, nunca como
 * afirmacion: acierta con tornilleria o cadenas, pero se pierde con
 * rodamientos o contactores.
 */
@Composable
private fun PistasIA(pistas: List<Pista>) {
    Tarjeta {
        Text(
            text = "Lo que ve la camara",
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
            Spacer(Modifier.height(Espaciado.pequeno))
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
                    text = "${(pista.confianza * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(Espaciado.pequeno))
        Text(
            text = "Reconoce formas comunes, no numeros de parte. Es una pista " +
                "para acotar el catalogo, no una identificacion.",
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
    onElegirPieza: () -> Unit
) {
    Insignia(texto = "No identificada", color = AmarilloAviso)

    Text(
        text = when (razon) {
            RazonFallo.SIN_TEXTO -> "No se leyo ningun texto en la pieza"
            RazonFallo.SIN_COINCIDENCIA -> "Se leyo texto, pero no coincide con el catalogo"
            RazonFallo.SIN_HUELLAS -> "Todavia no hay piezas que comparar"
        },
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground
    )

    Text(
        text = when (razon) {
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
            Spacer(Modifier.height(Espaciado.pequeno))
            Text(
                text = textoLeido.trim(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }

    PistasIA(pistas)

    // Accion principal: que la persona resuelva y de paso ensene a la app.
    BotonPrincipal(
        onClick = onElegirPieza,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = TamanosControles.alturaMinima)
    ) {
        Text("Decirle yo que pieza es", style = MaterialTheme.typography.labelLarge)
    }

    BotonSecundario(
        onClick = onRepetirFoto,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = TamanosControles.alturaMinima)
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
    BotonPrincipal(
        onClick = onRepetirFoto,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = TamanosControles.alturaMinima)
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
                shape = MaterialTheme.shapes.large
            )
            .padding(18.dp)
    ) {
        contenido()
    }
}
