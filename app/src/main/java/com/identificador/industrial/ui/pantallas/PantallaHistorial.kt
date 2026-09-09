package com.identificador.industrial.ui.pantallas

import com.identificador.industrial.ui.theme.Espaciado
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.identificador.industrial.datos.modelo.MetodoIdentificacion
import com.identificador.industrial.ui.Fabricas
import com.identificador.industrial.ui.componentes.Insignia
import com.identificador.industrial.ui.componentes.PantallaBase
import com.identificador.industrial.ui.theme.AmarilloAviso
import com.identificador.industrial.ui.theme.VerdeExito
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Pantalla 9: historial de busquedas. */
@Composable
fun PantallaHistorial(
    onVolver: () -> Unit,
    onVerDetalle: (String) -> Unit
) {
    val vm: HistorialViewModel = viewModel(factory = Fabricas.Historial)
    val entradas by vm.entradas.collectAsStateWithLifecycle()
    var confirmarBorrado by remember { mutableStateOf(false) }

    PantallaBase(
        titulo = "Historial",
        onVolver = onVolver,
        acciones = {
            if (entradas.isNotEmpty()) {
                TextButton(onClick = { confirmarBorrado = true }) { Text("Borrar") }
            }
        }
    ) { modifier ->

        if (entradas.isEmpty()) {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Todavia no has identificado ninguna pieza.\n" +
                        "Aqui apareceran tus busquedas, incluidas las que no den resultado.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(Espaciado.extraGrande)
                )
            }
        } else {
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        text = "Se guardan tambien los intentos fallidos: senalan piezas " +
                            "que existen en planta pero no estan dadas de alta.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                items(entradas, key = { it.busqueda.id }) { entrada ->
                    FilaHistorial(entrada) {
                        entrada.material?.let { onVerDetalle(it.id) }
                    }
                }
            }
        }
    }

    if (confirmarBorrado) {
        AlertDialog(
            onDismissRequest = { confirmarBorrado = false },
            title = { Text("Borrar el historial") },
            text = {
                Text(
                    "Se eliminaran todas las busquedas guardadas. Las piezas del " +
                        "catalogo y lo que la aplicacion ha aprendido no se tocan."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.borrarTodo()
                    confirmarBorrado = false
                }) { Text("Borrar") }
            },
            dismissButton = {
                TextButton(onClick = { confirmarBorrado = false }) { Text("Cancelar") }
            }
        )
    }
}

private val FORMATO = DateTimeFormatter.ofPattern("d MMM yyyy · HH:mm")

@Composable
private fun FilaHistorial(entrada: EntradaHistorial, onClick: () -> Unit) {
    val busqueda = entrada.busqueda
    val material = entrada.material

    val fecha = remember(busqueda.fechaHora) {
        LocalDateTime
            .ofInstant(Instant.ofEpochMilli(busqueda.fechaHora), ZoneId.systemDefault())
            .format(FORMATO)
    }

    ElevatedCard(
        onClick = onClick,
        enabled = material != null,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            disabledContainerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(Espaciado.normal)) {

            Text(
                text = material?.nombre ?: "No se identifico ninguna pieza",
                style = MaterialTheme.typography.titleMedium,
                color = if (material != null) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (material != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "${material.numeroParte}  ·  ${material.ubicacion.codigo}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            } else if (!busqueda.textoDetectado.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Se leyo: ${busqueda.textoDetectado.take(60).replace("\n", " ")}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(Espaciado.medio))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Insignia(
                    texto = etiquetaMetodo(busqueda.metodo, busqueda.confianza, material != null),
                    color = colorMetodo(busqueda.metodo, material != null)
                )
                Text(
                    text = fecha,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * El metodo importa tanto como el resultado: una lectura del numero grabado y
 * un parecido visual del 80% no merecen la misma confianza, y quien revise el
 * historial tiene derecho a distinguirlos de un vistazo.
 */
private fun etiquetaMetodo(
    metodo: MetodoIdentificacion,
    confianza: Float,
    hubieraAcierto: Boolean
): String {
    if (!hubieraAcierto) return "Sin resultado"
    val porcentaje = (confianza * 100).toInt()
    return when (metodo) {
        MetodoIdentificacion.OCR -> "Numero leido · $porcentaje%"
        MetodoIdentificacion.VISUAL -> "Parecido visual · $porcentaje%"
        MetodoIdentificacion.MANUAL -> "Confirmada a mano"
    }
}

private fun colorMetodo(metodo: MetodoIdentificacion, hubieraAcierto: Boolean): Color = when {
    !hubieraAcierto -> AmarilloAviso
    metodo == MetodoIdentificacion.VISUAL -> AmarilloAviso
    else -> VerdeExito
}
