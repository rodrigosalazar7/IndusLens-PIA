package com.identificador.industrial.ui.pantallas

import com.identificador.industrial.ui.theme.Espaciado
import com.identificador.industrial.ui.theme.TamanosControles
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.identificador.industrial.ui.componentes.BotonPrincipal
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import com.identificador.industrial.ui.componentes.BotonSecundario
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.identificador.industrial.datos.modelo.EmbeddingMaterial
import com.identificador.industrial.datos.modelo.Material
import com.identificador.industrial.ui.Fabricas
import com.identificador.industrial.ui.componentes.FotoReferenciaMaterial
import com.identificador.industrial.ui.componentes.Insignia
import com.identificador.industrial.ui.componentes.PantallaBase
import com.identificador.industrial.ui.theme.AmarilloAviso
import com.identificador.industrial.ui.theme.RojoError
import com.identificador.industrial.ui.theme.VerdeExito

/** Pantalla 7: detalle del material. */
@Composable
fun PantallaDetalle(
    materialId: String,
    onVolver: () -> Unit,
    onVerUbicacion: (String) -> Unit,
    onEnsenarPieza: (String) -> Unit,
    /**
     * Solo el administrador ve el lapiz. Es habitual llegar aqui tras
     * identificar una pieza y descubrir que la existencia no cuadra con lo que
     * hay en el rack; poder corregirlo en el momento evita que el error siga
     * ahi durante meses.
     */
    puedeEditar: Boolean = false,
    onEditar: (String) -> Unit = {},
    vm: DetalleViewModel = viewModel(factory = Fabricas.Detalle)
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val vistas by vm.vistas.collectAsStateWithLifecycle()

    LaunchedEffect(materialId) { vm.cargar(materialId) }

    PantallaBase(
        titulo = "Detalle de pieza",
        onVolver = onVolver,
        acciones = {
            if (puedeEditar && (estado as? EstadoDetalle.Encontrado)?.material?.activo == true) {
                IconButton(onClick = { onEditar(materialId) }) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Editar este material",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    ) { modifier ->
        when (val actual = estado) {
            is EstadoDetalle.Cargando -> Centrado(modifier) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }

            is EstadoDetalle.NoEncontrado -> Centrado(modifier) {
                Text(
                    text = "No se encontro ningun material con el identificador $materialId",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(Espaciado.extraGrande)
                )
            }

            is EstadoDetalle.Error -> Centrado(modifier) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("No se pudo consultar la pieza.", color = MaterialTheme.colorScheme.error)
                    BotonPrincipal(onClick = vm::reintentar) { Text("Reintentar") }
                }
            }

            is EstadoDetalle.Encontrado -> Contenido(
                modifier = modifier,
                material = actual.material,
                vistas = vistas,
                onVerUbicacion = onVerUbicacion,
                onEnsenarPieza = onEnsenarPieza,
                puedeEditar = puedeEditar,
                onEditar = onEditar
            )
        }
    }
}

@Composable
private fun Centrado(modifier: Modifier, contenido: @Composable () -> Unit) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        contenido()
    }
}

@Composable
private fun Contenido(
    modifier: Modifier,
    material: Material,
    vistas: Int,
    onVerUbicacion: (String) -> Unit,
    onEnsenarPieza: (String) -> Unit,
    puedeEditar: Boolean,
    onEditar: (String) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Espaciado.pantalla).testTag("detalle_pieza"),
        verticalArrangement = Arrangement.spacedBy(Espaciado.normal)
    ) {
        Text(
            text = material.nombre,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        FotoReferenciaMaterial(fotoReferencia = material.fotoReferencia)

        FlowRow(horizontalArrangement = Arrangement.spacedBy(Espaciado.pequeno),
            verticalArrangement = Arrangement.spacedBy(Espaciado.pequeno)) {
            Insignia(
                texto = material.categoria.etiqueta,
                color = MaterialTheme.colorScheme.secondary
            )
            Insignia(
                texto = textoExistencia(material),
                color = colorExistencia(material)
            )
        }

        if (!material.activo) {
            Text("Pieza dada de baja. Se conserva para consultar su historial.",
                color = MaterialTheme.colorScheme.error)
        } else {
            BotonPrincipal(onClick = { onVerUbicacion(material.id) },
                modifier = Modifier.fillMaxWidth().testTag("ver_ubicacion")) {
                Text("Ver ubicación en almacén")
            }
            if (puedeEditar) BotonSecundario(onClick = { onEditar(material.id) },
                modifier = Modifier.fillMaxWidth().testTag("editar_pieza")) {
                Text("Editar pieza e inventario")
            }
        }

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
            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Spacer(Modifier.height(14.dp))
            FilaDato("Fabricante", material.fabricante)
            FilaDato("Clave interna", material.id)
            material.medidaClave?.let { FilaDato("Medida", it) }
        }

        material.medidas?.let { medidas ->
            Tarjeta {
                Text(
                    text = "Medidas",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = medidas,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Dato del fabricante, no medido sobre una fotografia.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Tarjeta {
            Text(
                text = "Descripcion",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(Espaciado.pequeno))
            Text(
                text = material.descripcion.ifBlank { "Sin descripción adicional." },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Tarjeta {
            Text(
                text = "Existencia",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = material.existencia.toString(),
                    style = MaterialTheme.typography.headlineLarge,
                    color = colorExistencia(material)
                )
                Spacer(Modifier.size(Espaciado.pequeno))
                Text(
                    text = material.unidadMedida.lowercase(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            Spacer(Modifier.height(10.dp))
            FilaDato("Minimo de seguridad", "${material.existenciaMinima} ${material.unidadMedida.lowercase()}")
        }

        Tarjeta {
            Text(
                text = "Ubicacion en almacen",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = material.ubicacion.codigo,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(Espaciado.pequeno))
            Text(
                text = material.ubicacion.comoLlegar,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Tarjeta {
            Text(
                text = "Busqueda por parecido visual",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            Insignia(
                texto = when {
                    vistas < 0 -> "No se pudieron consultar las vistas"
                    vistas == 0 -> "Sin vistas de referencia"
                    vistas >= EmbeddingMaterial.VISTAS_RECOMENDADAS -> "$vistas vistas · lista"
                    else -> "$vistas de ${EmbeddingMaterial.VISTAS_RECOMENDADAS} vistas"
                },
                color = when {
                    vistas == 0 -> MaterialTheme.colorScheme.onSurfaceVariant
                    vistas >= EmbeddingMaterial.VISTAS_RECOMENDADAS -> VerdeExito
                    else -> AmarilloAviso
                }
            )
            Spacer(Modifier.height(Espaciado.medio))
            Text(
                text = when {
                    vistas == 0 ->
                        "Fotografia la pieza para que la aplicacion aprenda a " +
                            "reconocerla por su forma, sin depender del numero grabado."

                    vistas < EmbeddingMaterial.VISTAS_RECOMENDADAS ->
                        "Hay referencias guardadas. Añade otros ángulos y fondos, " +
                            "y comprueba el resultado con una foto nueva."

                    else ->
                        "Hay varias referencias guardadas. El parecido es orientativo; " +
                            "confirma el número de parte y las medidas antes de usar la pieza."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(14.dp))
            BotonSecundario(
                onClick = { onEnsenarPieza(material.id) },
                enabled = material.activo,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (vistas == 0) "Ensenar esta pieza" else "Anadir otra vista")
            }
        }

        Spacer(Modifier.height(Espaciado.pequeno))
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

@Composable
private fun FilaDato(etiqueta: String, valor: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = etiqueta,
            modifier = Modifier.weight(1f).padding(end = 12.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = valor,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun textoExistencia(material: Material): String = when {
    material.sinExistencia -> "Sin existencia"
    material.bajoMinimo -> "Bajo minimo"
    else -> "Disponible"
}

private fun colorExistencia(material: Material): Color = when {
    material.sinExistencia -> RojoError
    material.bajoMinimo -> AmarilloAviso
    else -> VerdeExito
}
