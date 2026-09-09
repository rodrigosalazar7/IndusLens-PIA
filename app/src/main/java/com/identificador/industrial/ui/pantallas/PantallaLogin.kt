package com.identificador.industrial.ui.pantallas

import com.identificador.industrial.ui.theme.Espaciado
import com.identificador.industrial.ui.theme.TamanosControles
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import com.identificador.industrial.ui.componentes.BotonPrincipal
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import com.identificador.industrial.ui.componentes.CampoTexto
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.identificador.industrial.R
import com.identificador.industrial.sesion.SesionViewModel

/** Pantalla 1: inicio de sesion. */
@Composable
fun PantallaLogin(
    sesion: SesionViewModel,
    onLoginExitoso: () -> Unit
) {
    var usuario by remember { mutableStateOf("") }
    var clave by remember { mutableStateOf("") }
    var claveVisible by remember { mutableStateOf(false) }

    val teclado = LocalSoftwareKeyboardController.current

    // Cuando el ViewModel confirma la sesion, se avisa al grafo de navegacion.
    LaunchedEffect(sesion.usuarioActivo) {
        if (sesion.usuarioActivo != null) onLoginExitoso()
    }

    fun intentarEntrar() {
        teclado?.hide()
        sesion.iniciarSesion(usuario, clave)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .widthIn(max = TamanosControles.anchoFormulario)
                .padding(horizontal = 28.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LogoIndusLens()

            Spacer(Modifier.height(22.dp))

            Text(
                text = "IndusLens",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Identifica y localiza materiales con una foto",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(36.dp))

            CampoTexto(
                value = usuario,
                onValueChange = {
                    usuario = it
                    sesion.limpiarError()
                },
                label = { Text("Usuario") },
                leadingIcon = {
                    Icon(Icons.Default.Person, contentDescription = null)
                },
                singleLine = true,
                enabled = !sesion.cargando,
                isError = sesion.error != null,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(14.dp))

            CampoTexto(
                value = clave,
                onValueChange = {
                    clave = it
                    sesion.limpiarError()
                },
                label = { Text("Contrasena") },
                leadingIcon = {
                    Icon(Icons.Default.Lock, contentDescription = null)
                },
                trailingIcon = {
                    TextButton(onClick = { claveVisible = !claveVisible }) {
                        Text(if (claveVisible) "Ocultar" else "Ver")
                    }
                },
                visualTransformation = if (claveVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                singleLine = true,
                enabled = !sesion.cargando,
                isError = sesion.error != null,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { intentarEntrar() }),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            )

            // Se reserva la altura del mensaje de error para que el boton no
            // brinque hacia abajo cuando aparece.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 34.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                sesion.error?.let { mensaje ->
                    Text(
                        text = mensaje,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            BotonPrincipal(
                onClick = { intentarEntrar() },
                cargando = sesion.cargando,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = TamanosControles.alturaMinima)
            ) {
                Text("Entrar", style = MaterialTheme.typography.labelLarge)
            }

            Spacer(Modifier.height(28.dp))

            AvisoCredencialesDemo()
        }
    }
}

@Composable
private fun LogoIndusLens() {
    Box(
        modifier = Modifier
            .size(108.dp)
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.extraLarge
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = "Logotipo de IndusLens",
            modifier = Modifier.size(96.dp)
        )
    }
}

/**
 * Cuentas de demostracion sembradas en Room. No usar estas claves en produccion.
 */
@Composable
private fun AvisoCredencialesDemo() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.medium
            )
            .padding(Espaciado.normal),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "Cuentas de prueba",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        listOf(
            "operador / 1234",
            "almacen / 1234",
            "admin / admin"
        ).forEach {
            Text(
                text = it,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
