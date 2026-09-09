package com.identificador.industrial.ui.componentes

import com.identificador.industrial.ui.theme.Espaciado
import com.identificador.industrial.ui.theme.TamanosControles
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import com.identificador.industrial.ui.componentes.BotonSecundario
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Armazon comun de todas las pantallas: barra superior con titulo, boton de
 * volver opcional y el contenido debajo respetando los margenes del sistema.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaBase(
    titulo: String,
    onVolver: (() -> Unit)? = null,
    acciones: @Composable () -> Unit = {},
    contenido: @Composable (Modifier) -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = titulo,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    if (onVolver != null) {
                        IconButton(onClick = onVolver) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Volver"
                            )
                        }
                    }
                },
                actions = { acciones() },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        contenido(Modifier.padding(padding))
    }
}

/** Tarjeta grande y tocable del menu principal. */
@Composable
fun TarjetaAccion(
    icono: ImageVector,
    titulo: String,
    descripcion: String,
    colorIcono: Color = MaterialTheme.colorScheme.primary,
    habilitada: Boolean = true,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        enabled = habilitada,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            disabledContainerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(
                        color = colorIcono.copy(alpha = 0.14f),
                        shape = MaterialTheme.shapes.medium
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icono,
                    contentDescription = null,
                    tint = if (habilitada) colorIcono else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(Modifier.size(Espaciado.normal))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = titulo,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = descripcion,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Cuerpo provisional de las pantallas que aun no tienen logica.
 * Deja escrito que va en cada una y en que fase se construye, para que el
 * proyecto se pueda revisar y presentar aunque este a medias.
 */
@Composable
fun PantallaPendiente(
    modifier: Modifier = Modifier,
    numeroPantalla: Int,
    fase: Int,
    descripcion: String,
    elementos: List<String>,
    // Botones temporales para poder recorrer el flujo completo sin la logica real.
    // Se retiran cuando la pantalla se implemente de verdad.
    accionesPrueba: List<Pair<String, () -> Unit>> = emptyList()
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Espaciado.pantalla),
        verticalArrangement = Arrangement.spacedBy(Espaciado.normal)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Espaciado.pequeno),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Insignia(texto = "Pantalla $numeroPantalla")
            Insignia(
                texto = "Fase $fase",
                color = MaterialTheme.colorScheme.secondary
            )
        }

        Text(
            text = descripcion,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = MaterialTheme.shapes.medium
                )
                .padding(Espaciado.normal),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Esta pantalla incluira",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            elementos.forEach { elemento ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    // La vinieta lleva el mismo estilo que el texto para que
                    // compartan altura de linea y queden alineadas arriba.
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = elemento,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        accionesPrueba.forEach { (texto, accion) ->
            BotonSecundario(
                onClick = accion,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = TamanosControles.alturaMinima)
            ) {
                Text(texto)
            }
        }
    }
}

/** Etiqueta pequena de color, para estados y clasificaciones. */
@Composable
fun Insignia(
    texto: String,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Text(
        text = texto.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = color,
        modifier = Modifier
            .background(
                color = color.copy(alpha = 0.14f),
                shape = MaterialTheme.shapes.small
            )
            .padding(horizontal = 9.dp, vertical = 4.dp)
    )
}
