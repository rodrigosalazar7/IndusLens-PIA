package com.identificador.industrial.ui.componentes

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.identificador.industrial.ui.theme.Espaciado
import com.identificador.industrial.ui.theme.TamanosControles

/** Altura minima, no fija: el control puede crecer con el tamano de letra. */
@Composable
fun BotonPrincipal(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    cargando: Boolean = false,
    shape: Shape = MaterialTheme.shapes.medium,
    contentPadding: PaddingValues = PaddingValues(Espaciado.normal, Espaciado.medio),
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = TamanosControles.alturaMinima),
        enabled = enabled && !cargando,
        shape = shape,
        contentPadding = contentPadding
    ) {
        if (cargando) {
            CircularProgressIndicator(
                modifier = Modifier.size(TamanosControles.indicador)
                    .semantics { contentDescription = "Cargando" },
                strokeWidth = 2.dp
            )
        } else {
            content()
        }
    }
}

@Composable
fun BotonSecundario(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = MaterialTheme.shapes.medium,
    contentPadding: PaddingValues = PaddingValues(Espaciado.normal, Espaciado.medio),
    content: @Composable RowScope.() -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = TamanosControles.alturaMinima),
        enabled = enabled,
        shape = shape,
        contentPadding = contentPadding,
        content = content
    )
}

/** Un unico campo base para login, busquedas y formularios de inventario. */
@Composable
fun CampoTexto(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: (@Composable () -> Unit)? = null,
    placeholder: (@Composable () -> Unit)? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    supportingText: (@Composable () -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    minLines: Int = 1,
    shape: Shape = MaterialTheme.shapes.medium
) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange, modifier = modifier,
        enabled = enabled, label = label, placeholder = placeholder,
        leadingIcon = leadingIcon, trailingIcon = trailingIcon,
        supportingText = supportingText, isError = isError,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions, keyboardActions = keyboardActions,
        singleLine = singleLine, minLines = minLines, shape = shape
    )
}
