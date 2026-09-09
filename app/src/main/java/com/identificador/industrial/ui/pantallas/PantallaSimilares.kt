package com.identificador.industrial.ui.pantallas

import com.identificador.industrial.ui.theme.Espaciado
import com.identificador.industrial.ui.theme.TamanosControles
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.foundation.lazy.itemsIndexed
import com.identificador.industrial.ui.componentes.BotonPrincipal
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.identificador.industrial.datos.modelo.Coincidencia
import com.identificador.industrial.ui.componentes.Insignia
import com.identificador.industrial.ui.componentes.PantallaBase
import com.identificador.industrial.ui.theme.AmarilloAviso
import com.identificador.industrial.ui.theme.VerdeExito

/** Pantalla 6: piezas similares. */
@Composable
fun PantallaSimilares(
    identificacion: IdentificacionViewModel,
    onVolver: () -> Unit,
    onVerDetalle: (String) -> Unit,
    onRepetirFoto: () -> Unit
) {
    val coincidencias = identificacion.coincidencias

    PantallaBase(titulo = "Piezas similares", onVolver = onVolver) { modifier ->
        if (coincidencias.isEmpty()) {
            SinCandidatas(modifier, onRepetirFoto)
        } else {
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(Espaciado.medio)
            ) {
                item {
                    Column {
                        Text(
                            text = "Ordenadas por parecido con tu fotografia",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "El parecido visual no distingue piezas casi " +
                                "iguales. Confirma el numero de parte antes de " +
                                "tomar el material.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AmarilloAviso
                        )
                    }
                }

                itemsIndexed(coincidencias, key = { _, c -> c.material.id }) { indice, coincidencia ->
                    FilaCoincidencia(
                        posicion = indice + 1,
                        coincidencia = coincidencia,
                        onClick = { onVerDetalle(coincidencia.material.id) }
                    )
                }

                item {
                    Spacer(Modifier.height(Espaciado.pequeno))
                    BotonPrincipal(
                        onClick = onRepetirFoto,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = TamanosControles.alturaMinima)
                    ) {
                        Text("Ninguna es, repetir la fotografia")
                    }
                }
            }
        }
    }
}

@Composable
private fun FilaCoincidencia(
    posicion: Int,
    coincidencia: Coincidencia,
    onClick: () -> Unit
) {
    val material = coincidencia.material

    ElevatedCard(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(Espaciado.normal)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "$posicion.  ${material.nombre}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.height(0.dp))
                Insignia(
                    texto = "${coincidencia.porcentaje}%",
                    color = colorSegunParecido(coincidencia.similitud)
                )
            }

            Spacer(Modifier.height(Espaciado.pequeno))

            Text(
                text = "${material.numeroParte}  ·  ${material.fabricante}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.height(Espaciado.medio))

            BarraParecido(coincidencia.similitud)

            Spacer(Modifier.height(10.dp))

            Text(
                text = "${material.existencia} ${material.unidadMedida.lowercase()}  ·  ${material.ubicacion.codigo}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Barra visual del parecido: mas rapida de leer de un vistazo que el numero. */
@Composable
private fun BarraParecido(similitud: Float) {
    val proporcion = similitud.coerceIn(0f, 1f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.extraSmall
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(proporcion)
                .height(6.dp)
                .background(
                    color = colorSegunParecido(similitud),
                    shape = MaterialTheme.shapes.extraSmall
                )
        )
    }
}

@Composable
private fun SinCandidatas(modifier: Modifier, onRepetirFoto: () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Espaciado.extraGrande),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Todavia no hay piezas que comparar",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(Espaciado.medio))
        Text(
            text = "La busqueda por parecido necesita que las piezas del " +
                "catalogo tengan una fotografia de referencia. Abre la ficha " +
                "de un material y usa \"Ensenar esta pieza\" para anadirla: " +
                "basta una sola foto por pieza.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(28.dp))
        BotonPrincipal(
            onClick = onRepetirFoto,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = TamanosControles.alturaMinima)
        ) {
            Text("Repetir la fotografia")
        }
    }
}

private fun colorSegunParecido(similitud: Float): Color = when {
    similitud >= 0.88f -> VerdeExito
    similitud >= 0.70f -> AmarilloAviso
    else -> Color(0xFF6E7B8A)
}
