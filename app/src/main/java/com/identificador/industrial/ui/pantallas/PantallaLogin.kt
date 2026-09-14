package com.identificador.industrial.ui.pantallas

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.layout.heightIn
import com.identificador.industrial.ui.theme.TamanosControles
import com.identificador.industrial.ui.theme.Espaciado
import com.identificador.industrial.ui.componentes.BotonSecundario
import com.identificador.industrial.ui.componentes.CampoTexto
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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

private enum class ModoAcceso {
    ENTRAR,
    CREAR_CUENTA
}

/** Pantalla 1: acceso local y registro verificado por correo. */
@Composable
fun PantallaLogin(
    sesion: SesionViewModel,
    onLoginExitoso: () -> Unit
) {
    var modo by remember { mutableStateOf(ModoAcceso.ENTRAR) }
    var identificador by remember { mutableStateOf("") }
    var nombre by remember { mutableStateOf("") }
    var correoRegistro by remember { mutableStateOf("") }
    var clave by remember { mutableStateOf("") }
    var confirmacion by remember { mutableStateOf("") }
    var claveVisible by remember { mutableStateOf(false) }

    val teclado = LocalSoftwareKeyboardController.current

    LaunchedEffect(sesion.usuarioActivo) {
        if (sesion.usuarioActivo != null) onLoginExitoso()
    }

    fun enviarFormulario() {
        teclado?.hide()
        if (modo == ModoAcceso.ENTRAR) {
            sesion.iniciarSesion(identificador, clave)
        } else {
            sesion.registrar(nombre, correoRegistro, clave, confirmacion)
        }
    }

    fun cambiarModo() {
        if (modo == ModoAcceso.CREAR_CUENTA) {
            identificador = correoRegistro
            modo = ModoAcceso.ENTRAR
        } else {
            correoRegistro = identificador.takeIf { '@' in it }.orEmpty()
            modo = ModoAcceso.CREAR_CUENTA
        }
        clave = ""
        confirmacion = ""
        sesion.limpiarMensajes()
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
                .padding(horizontal = 28.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LogoIndusLens()

            Spacer(Modifier.height(18.dp))
            Text(
                text = "IndusLens",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (modo == ModoAcceso.ENTRAR) {
                    "Identifica y localiza materiales con una foto"
                } else {
                    "Crea una cuenta y confirma tu correo"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(28.dp))

            if (modo == ModoAcceso.CREAR_CUENTA) {
                CampoTexto(
                    value = nombre,
                    onValueChange = {
                        nombre = it
                        sesion.limpiarError()
                    },
                    label = { Text("Nombre") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    singleLine = true,
                    enabled = !sesion.cargando,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
            }

            CampoTexto(
                value = if (modo == ModoAcceso.ENTRAR) identificador else correoRegistro,
                onValueChange = {
                    if (modo == ModoAcceso.ENTRAR) identificador = it else correoRegistro = it
                    sesion.limpiarError()
                },
                label = {
                    Text(if (modo == ModoAcceso.ENTRAR) "Correo o usuario" else "Correo")
                },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                singleLine = true,
                enabled = !sesion.cargando,
                isError = sesion.error != null,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            CampoClave(
                valor = clave,
                etiqueta = "Contrasena",
                visible = claveVisible,
                habilitado = !sesion.cargando,
                esUltimo = modo == ModoAcceso.ENTRAR,
                hayError = sesion.error != null,
                onValorCambia = {
                    clave = it
                    sesion.limpiarError()
                },
                onAlternarVisibilidad = { claveVisible = !claveVisible },
                onTerminar = { enviarFormulario() }
            )

            if (modo == ModoAcceso.CREAR_CUENTA) {
                Spacer(Modifier.height(12.dp))
                CampoClave(
                    valor = confirmacion,
                    etiqueta = "Confirmar contrasena",
                    visible = claveVisible,
                    habilitado = !sesion.cargando,
                    esUltimo = true,
                    hayError = sesion.error != null,
                    onValorCambia = {
                        confirmacion = it
                        sesion.limpiarError()
                    },
                    onAlternarVisibilidad = { claveVisible = !claveVisible },
                    onTerminar = { enviarFormulario() }
                )
            }

            sesion.error?.let { mensaje ->
                Spacer(Modifier.height(10.dp))
                MensajeAcceso(mensaje, esError = true)
            }
            sesion.aviso?.let { mensaje ->
                Spacer(Modifier.height(10.dp))
                MensajeAcceso(mensaje, esError = false)
            }

            Spacer(Modifier.height(16.dp))

            BotonPrincipal(
                onClick = { enviarFormulario() },
                enabled = !sesion.cargando,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = TamanosControles.alturaMinima)
            ) {
                if (sesion.cargando) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        if (modo == ModoAcceso.ENTRAR) "Entrar" else "Crear cuenta",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            if (sesion.correoPendiente != null) {
                Spacer(Modifier.height(10.dp))
                BotonSecundario(
                    onClick = {
                        val correo = sesion.correoPendiente.orEmpty()
                        identificador = correo
                        correoRegistro = correo
                        sesion.iniciarSesion(correo, clave)
                    },
                    enabled = !sesion.cargando,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Ya verifique, entrar")
                }
                TextButton(
                    onClick = {
                        sesion.reenviarVerificacion(
                            sesion.correoPendiente.orEmpty(),
                            clave
                        )
                    },
                    enabled = !sesion.cargando
                ) {
                    Text("Reenviar correo de verificacion")
                }
            }

            if (modo == ModoAcceso.ENTRAR) {
                TextButton(
                    onClick = { sesion.enviarRestablecimiento(identificador) },
                    enabled = !sesion.cargando
                ) {
                    Text("Olvide mi contrasena")
                }
            }

            TextButton(onClick = { cambiarModo() }, enabled = !sesion.cargando) {
                Text(
                    if (modo == ModoAcceso.ENTRAR) {
                        "Crear cuenta con correo"
                    } else {
                        "Ya tengo cuenta"
                    }
                )
            }

            if (modo == ModoAcceso.CREAR_CUENTA && !sesion.correoConfigurado) {
                Text(
                    text = "El registro se activa al conectar Firebase. " +
                        "Las cuentas de prueba ya funcionan sin Internet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            if (modo == ModoAcceso.ENTRAR) {
                Spacer(Modifier.height(20.dp))
                AvisoCredencialesDemo()
            }
        }
    }
}

@Composable
private fun CampoClave(
    valor: String,
    etiqueta: String,
    visible: Boolean,
    habilitado: Boolean,
    esUltimo: Boolean,
    hayError: Boolean,
    onValorCambia: (String) -> Unit,
    onAlternarVisibilidad: () -> Unit,
    onTerminar: () -> Unit
) {
    CampoTexto(
        value = valor,
        onValueChange = onValorCambia,
        label = { Text(etiqueta) },
        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
        trailingIcon = {
            TextButton(onClick = onAlternarVisibilidad) {
                Text(if (visible) "Ocultar" else "Ver")
            }
        },
        visualTransformation = if (visible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        singleLine = true,
        enabled = habilitado,
        isError = hayError,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = if (esUltimo) ImeAction.Done else ImeAction.Next
        ),
        keyboardActions = KeyboardActions(onDone = { onTerminar() }),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun MensajeAcceso(mensaje: String, esError: Boolean) {
    Text(
        text = mensaje,
        style = MaterialTheme.typography.bodyMedium,
        color = if (esError) {
            MaterialTheme.colorScheme.onErrorContainer
        } else {
            MaterialTheme.colorScheme.onSecondaryContainer
        },
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (esError) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    MaterialTheme.colorScheme.secondaryContainer
                },
                shape = MaterialTheme.shapes.medium
            )
            .padding(12.dp)
    )
}

@Composable
private fun LogoIndusLens() {
    Box(
        modifier = Modifier
            .size(100.dp)
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.extraLarge
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = "Logotipo de IndusLens",
            modifier = Modifier.size(90.dp)
        )
    }
}

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
            text = "Acceso rapido para la demostracion",
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
