package com.identificador.industrial.ui.pantallas

import com.identificador.industrial.ui.theme.Espaciado
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.identificador.industrial.datos.modelo.Material
import com.identificador.industrial.ui.Fabricas
import com.identificador.industrial.ui.componentes.Insignia
import com.identificador.industrial.ui.componentes.PantallaBase
import com.identificador.industrial.ui.theme.VerdeExito

/** Pantalla 8: localizacion dentro del almacen. */
@Composable
fun PantallaUbicacion(
    materialId: String,
    onVolver: () -> Unit
) {
    val vm: UbicacionViewModel = viewModel(factory = Fabricas.Ubicacion)
    LaunchedEffect(materialId) { vm.cargar(materialId) }

    val estado = vm.estado

    PantallaBase(titulo = "Ubicacion en almacen", onVolver = onVolver) { modifier ->
        when {
            estado.cargando -> Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }

            estado.material == null -> Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No se encontro el material $materialId",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(Espaciado.extraGrande)
                )
            }

            else -> Contenido(modifier, estado)
        }
    }
}

@Composable
private fun Contenido(modifier: Modifier, estado: EstadoUbicacion) {
    val material = estado.material!!
    val u = material.ubicacion

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Espaciado.pantalla),
        verticalArrangement = Arrangement.spacedBy(Espaciado.normal)
    ) {
        Text(
            text = material.nombre,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // El codigo, enorme: es lo que el operador lee de lejos mientras camina.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = MaterialTheme.shapes.large
                )
                .padding(vertical = 26.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = u.codigo,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold
            )
        }

        estado.plano?.let { Plano(it, u.pasillo, u.rack) }

        Tarjeta {
            Text(
                text = "Como llegar",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(Espaciado.medio))
            listOf(
                "Entra al almacen ${u.almacen}",
                "Recorre hasta el pasillo ${u.pasillo}",
                "Busca el rack ${u.rack}",
                "Sube al nivel ${u.nivel}",
                "La pieza esta en la posicion ${u.posicion}"
            ).forEachIndexed { indice, paso ->
                Row(
                    modifier = Modifier.padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                shape = MaterialTheme.shapes.medium
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${indice + 1}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = paso,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Tarjeta {
            Text(
                text = "Disponible en esta ubicacion",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "${material.existencia} ${material.unidadMedida.lowercase()}",
                style = MaterialTheme.typography.headlineMedium,
                color = if (material.sinExistencia) {
                    MaterialTheme.colorScheme.error
                } else {
                    VerdeExito
                }
            )
            if (material.sinExistencia) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "No vayas: la ubicacion existe pero esta vacia.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        if (estado.vecinos.isNotEmpty()) {
            Tarjeta {
                Text(
                    text = "Tambien en el rack ${u.rack}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(Espaciado.minimo))
                Text(
                    text = "Por si vas a por varias cosas del mismo viaje.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                estado.vecinos.forEach { vecino ->
                    FilaVecino(vecino)
                }
            }
        }

        Spacer(Modifier.height(Espaciado.pequeno))
    }
}

/**
 * Plano esquematico del almacen.
 *
 * No es un plano a escala del edificio: se dibuja con los pasillos y racks que
 * de verdad existen en el catalogo, y sirve para situarse ("estoy en el tercero
 * de cinco pasillos"). Un plano inventado seria mas bonito y menos util.
 */
@Composable
private fun Plano(plano: PlanoAlmacen, pasilloActual: String, rackActual: String) {
    Tarjeta {
        Text(
            text = "Almacen ${plano.almacen}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(14.dp))

        plano.pasillos.forEach { pasillo ->
            val esElPasillo = pasillo == pasilloActual
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Pasillo $pasillo",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (esElPasillo) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    fontWeight = if (esElPasillo) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.width(96.dp)
                )

                if (esElPasillo) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        plano.racksDelPasillo.forEach { rack ->
                            CeldaRack(rack, rack == rackActual)
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .height(34.dp)
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = MaterialTheme.shapes.small
                            )
                    )
                }
            }
        }

        Spacer(Modifier.height(Espaciado.medio))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .background(MaterialTheme.colorScheme.primary, MaterialTheme.shapes.extraSmall)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Rack donde esta la pieza",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CeldaRack(rack: String, resaltado: Boolean) {
    Box(
        modifier = Modifier
            .size(width = 46.dp, height = 34.dp)
            .background(
                color = if (resaltado) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                shape = MaterialTheme.shapes.small
            )
            .border(
                width = if (resaltado) 0.dp else 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = MaterialTheme.shapes.small
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = rack,
            style = MaterialTheme.typography.labelLarge,
            color = if (resaltado) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

@Composable
private fun FilaVecino(material: Material) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = material.nombre,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = material.numeroParte,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Insignia(
            texto = "N${material.ubicacion.nivel}-${material.ubicacion.posicion}",
            color = MaterialTheme.colorScheme.secondary
        )
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
