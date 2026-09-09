package com.identificador.industrial.ui.pantallas

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.identificador.industrial.datos.modelo.Categoria
import com.identificador.industrial.datos.modelo.Material
import com.identificador.industrial.ui.Fabricas
import com.identificador.industrial.ui.componentes.Insignia
import com.identificador.industrial.ui.componentes.PantallaBase

/**
 * La persona senala en el catalogo que pieza es.
 *
 * Aparece cuando la app no ha sabido reconocerla. Si el modelo generico dio
 * alguna pista del tipo de objeto, se abre ya filtrado por esa familia para
 * que haya que mirar cinco piezas y no las veintiseis.
 *
 * Al elegir, la fotografia se guarda como referencia de esa pieza. Cada vez
 * que alguien corrige a la app, la app aprende.
 */
@Composable
fun PantallaElegirPieza(
    identificacion: IdentificacionViewModel,
    usuarioId: String,
    onVolver: () -> Unit,
    onElegida: () -> Unit
) {
    val contexto = LocalContext.current
    val vm: CatalogoViewModel = viewModel(factory = Fabricas.Catalogo)
    val texto by vm.textoBusqueda.collectAsStateWithLifecycle()
    val materiales by vm.materiales.collectAsStateWithLifecycle()

    val estado = identificacion.estado.collectAsStateWithLifecycle().value
    val pistas = when (estado) {
        is EstadoIdentificacion.NoIdentificado -> estado.pistas
        is EstadoIdentificacion.Similares -> estado.pistas
        else -> emptyList()
    }
    val sugerida = pistas.firstNotNullOfOrNull { it.categoria }

    // Arranca filtrando por lo que la IA cree ver; se puede quitar de un toque.
    var filtro by remember(sugerida) { mutableStateOf(sugerida) }

    val visibles = remember(materiales, filtro) {
        if (filtro == null) materiales else materiales.filter { it.categoria == filtro }
    }

    PantallaBase(titulo = "Que pieza es", onVolver = onVolver) { modifier ->
        Column(modifier = modifier.fillMaxSize()) {

            if (pistas.isNotEmpty()) {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                    Text(
                        text = "La camara ve " +
                            pistas.take(2).joinToString(" o ") { it.etiqueta.lowercase() } + ".",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Senala cual es y la aprendere para la proxima vez.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            OutlinedTextField(
                value = texto,
                onValueChange = vm::cambiarBusqueda,
                placeholder = { Text("Buscar en el catalogo") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filtro == null,
                    onClick = { filtro = null },
                    label = { Text("Todo el catalogo") },
                    colors = FilterChipDefaults.filterChipColors()
                )
                if (sugerida != null) {
                    FilterChip(
                        selected = filtro == sugerida,
                        onClick = { filtro = sugerida },
                        label = { Text(sugerida.etiqueta) }
                    )
                }
            }

            if (visibles.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Ninguna pieza coincide",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(32.dp)
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(visibles, key = { it.id }) { material ->
                        FilaEleccion(material) {
                            identificacion.confirmarPieza(contexto, material.id, usuarioId)
                            onElegida()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilaEleccion(material: Material, onClick: () -> Unit) {
    ElevatedCard(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = material.nombre,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = material.numeroParte,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.fillMaxWidth(0f))
                Text(
                    text = "  ·  ${material.ubicacion.codigo}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
            Insignia(
                texto = material.categoria.etiqueta,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}
