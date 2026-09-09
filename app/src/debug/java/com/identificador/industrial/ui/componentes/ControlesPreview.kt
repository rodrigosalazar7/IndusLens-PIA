package com.identificador.industrial.ui.componentes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.identificador.industrial.ui.theme.Espaciado
import com.identificador.industrial.ui.theme.IndusLensTheme

/** Galeria de revision en Android Studio; no agrega una pantalla al producto. */
@Preview(name = "Controles IndusLens", widthDp = 360, showBackground = true)
@Preview(name = "Texto grande", widthDp = 360, fontScale = 1.5f, showBackground = true)
@Composable
private fun ControlesPreview() {
    IndusLensTheme {
        Surface {
            Column(Modifier.padding(Espaciado.pantalla), Arrangement.spacedBy(Espaciado.medio)) {
                CampoTexto("Rodamiento", {}, label = { Text("Nombre de la pieza") }, modifier = Modifier.fillMaxWidth())
                CampoTexto("", {}, label = { Text("Usuario") }, isError = true,
                    supportingText = { Text("Escribe un nombre de usuario.") }, modifier = Modifier.fillMaxWidth())
                BotonPrincipal({}, Modifier.fillMaxWidth()) { Text("Guardar cambios") }
                BotonPrincipal({}, Modifier.fillMaxWidth(), cargando = true) { Text("Guardar") }
                BotonPrincipal({}, Modifier.fillMaxWidth(), enabled = false) { Text("Sin cambios") }
                BotonSecundario({}, Modifier.fillMaxWidth()) { Text("Seleccionar una imagen del dispositivo") }
            }
        }
    }
}
