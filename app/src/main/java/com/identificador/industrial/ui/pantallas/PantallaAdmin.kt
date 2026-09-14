package com.identificador.industrial.ui.pantallas

import com.identificador.industrial.ui.theme.Espaciado
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.foundation.layout.width
import androidx.compose.material3.IconButton
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.CircularProgressIndicator
import com.identificador.industrial.ui.componentes.BotonPrincipal
import com.identificador.industrial.ui.componentes.BotonSecundario
import com.identificador.industrial.ui.componentes.CampoTexto
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.identificador.industrial.datos.modelo.Material
import com.identificador.industrial.ui.Fabricas
import com.identificador.industrial.ui.componentes.Insignia
import com.identificador.industrial.ui.componentes.PantallaBase
import com.identificador.industrial.ui.theme.AmarilloAviso
import com.identificador.industrial.ui.theme.RojoError
import com.identificador.industrial.ui.theme.VerdeExito

/** Pantalla 10: administracion de materiales. */
@Composable
fun PantallaAdmin(
    onVolver: () -> Unit,
    onVerDetalle: (String) -> Unit,
    onEditar: (String) -> Unit,
    onNuevo: () -> Unit,
    puedeEditar: Boolean = true,
    vm: CatalogoViewModel = viewModel(factory = Fabricas.Catalogo)
) {
    val texto by vm.textoBusqueda.collectAsStateWithLifecycle()
    val estado by vm.estado.collectAsStateWithLifecycle()
    val materiales = estado.materiales

    PantallaBase(
        titulo = "Catálogo de piezas",
        onVolver = onVolver,
        acciones = {
            if (puedeEditar) IconButton(onClick = onNuevo) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Anadir material nuevo",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    ) { modifier ->
        Column(modifier = modifier.fillMaxSize()) {

            CampoTexto(
                value = texto,
                onValueChange = vm::cambiarBusqueda,
                label = { Text("Buscar piezas") },
                placeholder = { Text("Nombre, número de parte o fabricante") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (texto.isNotEmpty()) {
                        TextButton(onClick = vm::limpiarBusqueda) { Text("Limpiar") }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp).testTag("buscar_catalogo")
            )

            Text(
                text = when {
                    estado.cargando -> "Consultando catálogo…"
                    estado.error -> "Consulta no disponible"
                    materiales.size == 1 -> "1 pieza"
                    else -> "${materiales.size} piezas"
                },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )

            if (puedeEditar && materiales.isNotEmpty()) {
                BotonPrincipal(onClick = onNuevo, modifier = Modifier
                    .fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).testTag("nueva_pieza")) {
                    Text("Registrar nueva pieza")
                }
            }

            if (estado.cargando || estado.error || materiales.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (estado.cargando) {
                        CircularProgressIndicator()
                    } else Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = if (estado.error) {
                            "No se pudo consultar el catálogo. Intenta de nuevo."
                        } else if (texto.isBlank()) {
                            "Tu kit comienza aquí. Registra tu primera pieza con su existencia y ubicación."
                        } else {
                            "Ningun material coincide con \"$texto\""
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("estado_catalogo")
                    )
                    when {
                        estado.error -> BotonPrincipal(onClick = vm::reintentar) { Text("Reintentar") }
                        texto.isNotBlank() -> BotonSecundario(onClick = vm::limpiarBusqueda) { Text("Limpiar búsqueda") }
                        puedeEditar -> BotonPrincipal(onClick = onNuevo, modifier = Modifier.testTag("nueva_pieza")) {
                            Text("Registrar primera pieza")
                        }
                        else -> Text("Un administrador puede registrar el kit.", style = MaterialTheme.typography.bodySmall)
                    }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(materiales, key = { it.id }) { material ->
                        FilaMaterial(
                            material = material,
                            onClick = { onVerDetalle(material.id) },
                            onEditar = { onEditar(material.id) },
                            puedeEditar = puedeEditar
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilaMaterial(
    material: Material,
    onClick: () -> Unit,
    onEditar: () -> Unit,
    puedeEditar: Boolean
) {
    ElevatedCard(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth().testTag("pieza_${material.id}")
    ) {
        Column(modifier = Modifier.padding(Espaciado.normal)) {

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
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "  ·  ${material.fabricante}",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Insignia(
                    texto = "${material.existencia} ${material.unidadMedida}",
                    color = colorSegunExistencia(material)
                )
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = material.ubicacion.codigo,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = MaterialTheme.shapes.small
                            )
                            .padding(horizontal = 9.dp, vertical = 4.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    if (puedeEditar) IconButton(onClick = onEditar) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar ${material.nombre}",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

private fun colorSegunExistencia(material: Material): Color = when {
    material.sinExistencia -> RojoError
    material.bajoMinimo -> AmarilloAviso
    else -> VerdeExito
}
