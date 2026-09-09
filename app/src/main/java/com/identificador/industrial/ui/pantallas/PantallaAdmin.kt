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
import com.identificador.industrial.ui.componentes.CampoTexto
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    onNuevo: () -> Unit
) {
    val vm: CatalogoViewModel = viewModel(factory = Fabricas.Catalogo)
    val texto by vm.textoBusqueda.collectAsStateWithLifecycle()
    val materiales by vm.materiales.collectAsStateWithLifecycle()

    PantallaBase(
        titulo = "Administracion",
        onVolver = onVolver,
        acciones = {
            IconButton(onClick = onNuevo) {
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
                placeholder = { Text("Buscar por nombre, numero de parte o fabricante") },
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
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            )

            Text(
                text = if (materiales.size == 1) "1 material" else "${materiales.size} materiales",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )

            if (materiales.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (texto.isBlank()) {
                            "El catalogo esta vacio"
                        } else {
                            "Ningun material coincide con \"$texto\""
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(Espaciado.extraGrande)
                    )
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
                            onEditar = { onEditar(material.id) }
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
    onEditar: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth()
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
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "  ·  ${material.fabricante}",
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = material.ubicacion.codigo,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = MaterialTheme.shapes.small
                            )
                            .padding(horizontal = 9.dp, vertical = 4.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    IconButton(onClick = onEditar) {
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
