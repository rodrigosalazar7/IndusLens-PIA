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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.identificador.industrial.sesion.Usuario
import com.identificador.industrial.ui.componentes.Insignia
import com.identificador.industrial.ui.componentes.PantallaBase
import com.identificador.industrial.ui.componentes.TarjetaAccion

/** Pantalla 2: menu principal. */
@Composable
fun PantallaMenu(
    usuario: Usuario?,
    onIdentificar: () -> Unit,
    onHistorial: () -> Unit,
    onAdministrar: () -> Unit,
    onCerrarSesion: () -> Unit
) {
    PantallaBase(
        titulo = "Menu principal",
        acciones = {
            TextButton(onClick = onCerrarSesion) {
                Text("Salir")
            }
        }
    ) { modifier ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (usuario != null) {
                CabeceraUsuario(usuario)
                Spacer(Modifier.height(4.dp))
            }

            TarjetaAccion(
                icono = Icons.Default.Search,
                titulo = "Identificar pieza",
                descripcion = "Toma una foto y la IA reconoce el componente",
                onClick = onIdentificar
            )

            TarjetaAccion(
                icono = Icons.Default.DateRange,
                titulo = "Historial de busquedas",
                descripcion = "Consulta las piezas identificadas recientemente",
                colorIcono = MaterialTheme.colorScheme.secondary,
                onClick = onHistorial
            )

            TarjetaAccion(
                icono = Icons.Default.Settings,
                titulo = "Administracion de materiales",
                descripcion = if (usuario?.puedeAdministrar == true) {
                    "Alta, edicion y existencias del catalogo"
                } else {
                    "Requiere permisos de administrador"
                },
                colorIcono = MaterialTheme.colorScheme.tertiary,
                habilitada = usuario?.puedeAdministrar == true,
                onClick = onAdministrar
            )
        }
    }
}

@Composable
private fun CabeceraUsuario(usuario: Usuario) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = usuario.nombre.take(1).uppercase(),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.size(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = usuario.nombre,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(5.dp))
            Insignia(
                texto = usuario.rol.etiqueta,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}
