package com.identificador.industrial.ui.pantallas

import com.identificador.industrial.ui.theme.Espaciado
import com.identificador.industrial.ui.theme.TamanosControles
import androidx.compose.foundation.layout.heightIn
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
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
import com.identificador.industrial.ui.componentes.BotonPrincipal
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import com.identificador.industrial.ui.componentes.BotonSecundario
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.produceState
import com.identificador.industrial.ia.Fotos
import com.identificador.industrial.ia.Medicion
import com.identificador.industrial.ia.Referencia
import com.identificador.industrial.ui.componentes.Insignia
import com.identificador.industrial.ui.componentes.PantallaBase
import com.identificador.industrial.ui.theme.AmarilloAviso
import com.identificador.industrial.ui.theme.VerdeExito
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private enum class PasoMedicion { REFERENCIA, PIEZA }

/**
 * Mide una pieza sobre la fotografia usando un objeto de tamano conocido.
 *
 * Primero se marca el patron (una moneda, una tarjeta) y despues la pieza.
 * Las dos medidas se toman en la misma imagen y en las mismas coordenadas de
 * pantalla, asi que la proporcion entre ellas es exacta y no hace falta
 * convertir a pixeles de la fotografia original: al dividir, la escala del
 * dibujado se cancela sola.
 */
@Composable
fun PantallaMedir(
    identificacion: IdentificacionViewModel,
    onVolver: () -> Unit
) {
    val contexto = LocalContext.current
    val uri = identificacion.fotoUri

    val mapa by produceState<Bitmap?>(initialValue = null, uri) {
        value = if (uri == null) null
        else withContext(Dispatchers.IO) { Fotos.cargarReducida(contexto, uri, 1024) }
    }

    var referencia by remember { mutableStateOf(Referencia.MONEDA_10_MXN) }
    var paso by remember { mutableStateOf(PasoMedicion.REFERENCIA) }
    var pixelesReferencia by remember { mutableStateOf<Float?>(null) }

    var puntoA by remember { mutableStateOf(Offset(300f, 700f)) }
    var puntoB by remember { mutableStateOf(Offset(700f, 700f)) }
    var arrastrandoA by remember { mutableStateOf(true) }

    val largoActual = (puntoB - puntoA).getDistance()
    val medida = pixelesReferencia?.let {
        Medicion.calcular(it, largoActual, referencia)
    }

    PantallaBase(titulo = "Medir la pieza", onVolver = onVolver) { modifier ->
        Column(modifier = modifier.fillMaxSize()) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.Black)
                    .pointerInput(paso) {
                        detectDragGestures(
                            onDragStart = { posicion ->
                                // Se mueve el marcador mas cercano al dedo.
                                arrastrandoA = (posicion - puntoA).getDistance() <=
                                    (posicion - puntoB).getDistance()
                            },
                            onDrag = { cambio, desplazamiento ->
                                cambio.consume()
                                if (arrastrandoA) puntoA += desplazamiento
                                else puntoB += desplazamiento
                            }
                        )
                    }
            ) {
                mapa?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "Fotografia de la pieza",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                val color = if (paso == PasoMedicion.REFERENCIA) AmarilloAviso else VerdeExito

                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawLine(
                        color = color,
                        start = puntoA,
                        end = puntoB,
                        strokeWidth = 5f
                    )
                    listOf(puntoA, puntoB).forEach { punto ->
                        drawCircle(color = Color.White, radius = 34f, center = punto)
                        drawCircle(color = color, radius = 26f, center = punto)
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(18.dp)
            ) {
                when (paso) {
                    PasoMedicion.REFERENCIA -> {
                        Insignia(texto = "Paso 1 de 2", color = AmarilloAviso)
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "Marca el objeto de referencia",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Arrastra los dos circulos hasta los extremos de la " +
                                "moneda. Debe estar apoyada junto a la pieza, en el mismo " +
                                "plano: si esta mas cerca de la camara, la medida saldra mal.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(Modifier.height(Espaciado.medio))
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(Espaciado.pequeno)
                        ) {
                            Referencia.entries.forEach { opcion ->
                                FilterChip(
                                    selected = referencia == opcion,
                                    onClick = { referencia = opcion },
                                    label = { Text(opcion.etiqueta) }
                                )
                            }
                        }

                        Spacer(Modifier.height(14.dp))
                        BotonPrincipal(
                            onClick = {
                                pixelesReferencia = largoActual
                                paso = PasoMedicion.PIEZA
                                // Se separan los marcadores para que se vea que
                                // ahora toca marcar otra cosa.
                                puntoA = Offset(puntoA.x, puntoA.y + 260f)
                                puntoB = Offset(puntoB.x, puntoB.y + 260f)
                            },
                            enabled = largoActual > 20f,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = TamanosControles.alturaMinima)
                        ) {
                            Text("Fijar referencia y medir la pieza")
                        }
                    }

                    PasoMedicion.PIEZA -> {
                        Insignia(texto = "Paso 2 de 2", color = VerdeExito)
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "Marca la pieza",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(10.dp))

                        if (medida != null) {
                            Text(
                                text = medida.formateada(),
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(Espaciado.minimo))
                            Text(
                                text = "${medida.enFraccion}  (${String.format("%.2f", medida.pulgadas)} pulgadas)",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            medida.metricaProxima?.let { metrica ->
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = "Compatible con metrica $metrica",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(Modifier.height(14.dp))
                        BotonSecundario(
                            onClick = {
                                paso = PasoMedicion.REFERENCIA
                                pixelesReferencia = null
                            },
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Volver a marcar la referencia")
                        }
                    }
                }

                if (mapa == null) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "No hay ninguna fotografia que medir.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
