package com.identificador.industrial.ui

import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.identificador.industrial.ui.componentes.BotonPrincipal
import com.identificador.industrial.ui.componentes.BotonSecundario
import com.identificador.industrial.ui.componentes.CampoTexto
import com.identificador.industrial.ui.theme.IndusLensTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ControlesTest {
    @get:Rule val compose = createComposeRule()

    @Test fun botonPrincipalTieneAlturaMinimaYResponde() {
        var clics = 0
        compose.setContent { IndusLensTheme {
            BotonPrincipal({ clics++ }, Modifier.testTag("boton")) { Text("Entrar") }
        } }
        compose.onNodeWithTag("boton").assertHeightIsAtLeast(52.dp).assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(1, clics) }
    }

    @Test fun cargandoImpideDuplicarAccionesYExponeEstadoAccesible() {
        compose.setContent { IndusLensTheme {
            BotonPrincipal({}, Modifier.testTag("boton"), cargando = true) { Text("Guardar") }
        } }
        compose.onNodeWithTag("boton").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Cargando", useUnmergedTree = true).assertExists()
    }

    @Test fun textoGrandeHaceCrecerElBotonEnVezDeRecortarlo() {
        compose.setContent { IndusLensTheme {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1.5f)) {
                BotonSecundario({}, Modifier.width(220.dp).testTag("boton")) {
                    Text("Seleccionar una imagen del dispositivo")
                }
            }
        } }
        compose.onNodeWithTag("boton").assertHeightIsAtLeast(72.dp)
    }

    @Test fun campoCompartidoTransmiteLaEntradaSinModificarla() {
        var recibido = ""
        compose.setContent { IndusLensTheme {
            CampoTexto("", { recibido = it }, Modifier.testTag("campo"), singleLine = true)
        } }
        compose.onNodeWithTag("campo").performTextInput(" ADMIN ")
        compose.runOnIdle { assertEquals(" ADMIN ", recibido) }
    }
}
