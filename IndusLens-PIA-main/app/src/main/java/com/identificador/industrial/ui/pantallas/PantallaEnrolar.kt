package com.identificador.industrial.ui.pantallas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.identificador.industrial.datos.modelo.EmbeddingMaterial
import com.identificador.industrial.ui.Fabricas
import com.identificador.industrial.ui.theme.AmarilloAviso
import com.identificador.industrial.ui.componentes.Insignia
import com.identificador.industrial.ui.componentes.PantallaBase
import com.identificador.industrial.ui.theme.VerdeExito

/**
 * Ensenar una pieza a la aplicacion.
 *
 * Se fotografia el material y se guarda su huella visual. Con eso la pieza
 * queda disponible para la busqueda por parecido, sin reentrenar ningun modelo.
 */
@Composable
fun PantallaEnrolar(
    materialId: String,
    onVolver: () -> Unit
) {
    val contexto = LocalContext.current
    val vm: EnrolamientoViewModel = viewModel(factory = Fabricas.Enrolamiento)

    when (val estado = vm.estado) {

        is EstadoEnrolamiento.Inactivo -> PantallaCamara(
            titulo = "Ensenar la pieza",
            textoGuia = "Coloca la pieza sobre un fondo liso y encuadrala",
            onVolver = onVolver,
            onFotoTomada = { uri -> vm.enrolar(contexto, materialId, uri) }
        )

        is EstadoEnrolamiento.Procesando -> PantallaBase(
            titulo = "Ensenar la pieza",
            onVolver = onVolver
        ) { modifier ->
            Centro(modifier) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(24.dp))
                Text(
                    text = "Calculando la huella visual",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        is EstadoEnrolamiento.Listo -> PantallaBase(
            titulo = "Ensenar la pieza",
            onVolver = onVolver
        ) { modifier ->
            Centro(modifier) {
                val completa = estado.vistas >= EmbeddingMaterial.VISTAS_RECOMENDADAS
                Insignia(
                    texto = if (estado.vistas == 1) "1 vista guardada" else "${estado.vistas} vistas guardadas",
                    color = if (completa) VerdeExito else AmarilloAviso
                )
                Spacer(Modifier.height(20.dp))
                Text(
                    text = estado.nombreMaterial,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = if (completa) {
                        "Esta pieza ya se reconoce desde varios angulos aunque " +
                            "no se vea ningun numero."
                    } else {
                        "Ya se reconoce, pero solo desde angulos parecidos a este. " +
                            "Gira la pieza, cambia el fondo y anade otra vista: con " +
                            "${EmbeddingMaterial.VISTAS_RECOMENDADAS} acierta casi siempre."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(32.dp))
                Button(
                    onClick = { vm.reiniciar(); onVolver() },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text("Listo")
                }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { vm.reiniciar() },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Anadir otra vista")
                }
            }
        }

        is EstadoEnrolamiento.Error -> PantallaBase(
            titulo = "Ensenar la pieza",
            onVolver = onVolver
        ) { modifier ->
            Centro(modifier) {
                Text(
                    text = "No se pudo aprender la pieza",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = estado.mensaje,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(28.dp))
                Button(
                    onClick = { vm.reiniciar() },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text("Intentar de nuevo")
                }
            }
        }
    }
}

@Composable
private fun Centro(modifier: Modifier, contenido: @Composable () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        contenido()
    }
}
