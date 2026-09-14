package com.identificador.industrial.ui.pantallas

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    onEditar: (String) -> Unit = {}
) {
    val vm: DetalleViewModel = viewModel(factory = Fabricas.Detalle)
    val estado by vm.estado.collectAsStateWithLifecycle()
    val vistas by vm.vistas.collectAsStateWithLifecycle()

    LaunchedEffect(materialId) { vm.cargar(materialId) }

    PantallaBase(
        titulo = "Detalle del material",
        onVolver = onVolver,
        acciones = {
            if (puedeEditar && estado is EstadoDetalle.Encontrado) {
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
                    modifier = Modifier.padding(32.dp)
                )
            }

            is EstadoDetalle.Encontrado -> Contenido(
                modifier = modifier,
                material = actual.material,
                vistas = vistas,
                onVerUbicacion = onVerUbicacion,
                onEnsenarPieza = onEnsenarPieza
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
    onEnsenarPieza: (String) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = material.nombre,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        FotoReferenciaMaterial(fotoReferencia = material.fotoReferencia)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Insignia(
                texto = material.categoria.etiqueta,
                color = MaterialTheme.colorScheme.secondary
            )
            Insignia(
                texto = textoExistencia(material),
                color = colorExistencia(material)
            )
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
            Spacer(Modifier.height(8.dp))
            Text(
                text = material.descripcion,
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
                Spacer(Modifier.size(8.dp))
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
            Spacer(Modifier.height(8.dp))
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
            Spacer(Modifier.height(12.dp))
            Text(
                text = when {
                    vistas == 0 ->
                        "Fotografia la pieza para que la aplicacion aprenda a " +
                            "reconocerla por su forma, sin depender del numero grabado."

                    vistas < EmbeddingMaterial.VISTAS_RECOMENDADAS ->
                        "Ya se reconoce, pero solo desde angulos parecidos a los " +
                            "que fotografiaste. Anade vistas desde otros lados y " +
                            "sobre otros fondos para que acierte siempre."

                    else ->
                        "Bien aprendida: se reconoce desde varios angulos aunque " +
                            "no se vea ningun numero."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(14.dp))
            OutlinedButton(
                onClick = { onEnsenarPieza(material.id) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (vistas == 0) "Ensenar esta pieza" else "Anadir otra vista")
            }
        }

        Button(
            onClick = { onVerUbicacion(material.id) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text("Ver ubicacion en almacen", style = MaterialTheme.typography.labelLarge)
        }

        Spacer(Modifier.height(8.dp))
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
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = valor,
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
