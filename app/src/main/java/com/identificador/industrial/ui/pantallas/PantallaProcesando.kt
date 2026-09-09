package com.identificador.industrial.ui.pantallas

import com.identificador.industrial.ui.theme.Espaciado
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.identificador.industrial.ui.componentes.PantallaBase

/** Pantalla 4: procesamiento con IA. */
@Composable
fun PantallaProcesando(
    identificacion: IdentificacionViewModel,
    usuarioId: String,
    onVolver: () -> Unit,
    onListo: () -> Unit
) {
    val contexto = LocalContext.current
    val estado by identificacion.estado.collectAsStateWithLifecycle()

    /**
     * Un unico efecto gobierna la pantalla, y reacciona al estado en lugar de
     * dispararse una sola vez al entrar.
     *
     * La diferencia importa: antes el analisis se lanzaba con LaunchedEffect(Unit),
     * asi que si algo devolvia el estado a "inactivo" despues de entrar aqui
     * -por ejemplo una segunda captura tardia de la camara, que en el emulador
     * ocurre cuando el proveedor de video falla y CameraX reintenta- ya no
     * habia nada que relanzara el trabajo. La pantalla se quedaba girando para
     * siempre, y ni siquiera saltaba el limite de tiempo, porque no habia
     * ninguna corrutina corriendo que pudiera agotarlo.
     *
     * Reaccionando al estado, la pantalla se repara sola: si vuelve a estar
     * inactiva, se vuelve a analizar.
     */
    LaunchedEffect(estado) {
        when (estado) {
            is EstadoIdentificacion.Inactivo -> identificacion.analizar(contexto, usuarioId)

            is EstadoIdentificacion.Identificado,
            is EstadoIdentificacion.Similares,
            is EstadoIdentificacion.NoIdentificado,
            is EstadoIdentificacion.Fallo -> onListo()

            is EstadoIdentificacion.Analizando -> Unit
        }
    }

    val pasoActual = (estado as? EstadoIdentificacion.Analizando)?.paso

    PantallaBase(titulo = "Analizando", onVolver = onVolver) { modifier ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(Espaciado.extraGrande),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(56.dp),
                strokeWidth = 4.dp,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.height(36.dp))

            Text(
                text = "Identificando la pieza",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(Modifier.height(28.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                PasoAnalisis.entries.forEach { paso ->
                    FilaPaso(
                        etiqueta = paso.etiqueta,
                        estado = when {
                            pasoActual == null -> EstadoPaso.PENDIENTE
                            paso.ordinal < pasoActual.ordinal -> EstadoPaso.HECHO
                            paso == pasoActual -> EstadoPaso.EN_CURSO
                            else -> EstadoPaso.PENDIENTE
                        }
                    )
                }
            }
        }
    }
}

private enum class EstadoPaso { PENDIENTE, EN_CURSO, HECHO }

@Composable
private fun FilaPaso(etiqueta: String, estado: EstadoPaso) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(22.dp),
            contentAlignment = Alignment.Center
        ) {
            when (estado) {
                EstadoPaso.EN_CURSO -> CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )

                EstadoPaso.HECHO -> Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                )

                EstadoPaso.PENDIENTE -> Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(MaterialTheme.colorScheme.outline, CircleShape)
                )
            }
        }

        Spacer(Modifier.size(14.dp))

        Text(
            text = etiqueta,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (estado == EstadoPaso.EN_CURSO) FontWeight.SemiBold else FontWeight.Normal,
            color = when (estado) {
                EstadoPaso.EN_CURSO -> MaterialTheme.colorScheme.onBackground
                EstadoPaso.HECHO -> MaterialTheme.colorScheme.onSurfaceVariant
                EstadoPaso.PENDIENTE -> MaterialTheme.colorScheme.outline
            }
        )
    }
}
