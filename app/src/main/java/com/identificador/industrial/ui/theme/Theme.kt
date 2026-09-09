package com.identificador.industrial.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/**
 * La app usa siempre el esquema oscuro, independientemente del ajuste del sistema.
 * Es una decision de diseno: se usa en piso de planta y almacen, donde la pantalla
 * clara deslumbra y consume mas bateria en paneles OLED.
 */
private val EsquemaOscuro = darkColorScheme(
    primary = AzulInteligencia,
    onPrimary = TextoPrincipal,
    primaryContainer = AzulMedio,
    onPrimaryContainer = TextoPrincipal,

    secondary = AmbarHallazgo,
    onSecondary = FondoBase,
    secondaryContainer = ColorAmbarOscuro,
    onSecondaryContainer = TextoPrincipal,

    tertiary = AzulMedio,
    onTertiary = TextoPrincipal,

    background = FondoBase,
    onBackground = TextoPrincipal,

    surface = Superficie,
    onSurface = TextoPrincipal,
    surfaceVariant = SuperficieAlta,
    onSurfaceVariant = TextoSecundario,

    outline = Borde,
    outlineVariant = Borde,

    error = RojoError,
    onError = TextoPrincipal
)

@Composable
fun IndusLensTheme(
    // Se recibe el parametro por convencion, pero se ignora a proposito:
    // ver comentario de arriba.
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = EsquemaOscuro,
        typography = TipografiaApp,
        shapes = FormasApp,
        content = content
    )
}
