package com.identificador.industrial.ui.pantallas

import com.identificador.industrial.ui.theme.Espaciado
import com.identificador.industrial.ui.theme.TamanosControles
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import com.identificador.industrial.ui.componentes.BotonPrincipal
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import com.identificador.industrial.ui.componentes.BotonSecundario
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.identificador.industrial.datos.modelo.Categoria
import com.identificador.industrial.ui.Fabricas
import com.identificador.industrial.ui.componentes.PantallaBase

/**
 * Alta y edicion de materiales del catalogo.
 *
 * Es la misma pantalla para los dos casos: cambian el titulo, la clave (que en
 * el alta se genera sola) y la existencia del boton de baja.
 */
@Composable
fun PantallaEditarMaterial(
    materialId: String?,
    onVolver: () -> Unit,
    onGuardado: () -> Unit
) {
    val vm: EdicionViewModel = viewModel(factory = Fabricas.Edicion)
    LaunchedEffect(materialId) { vm.cargar(materialId) }

    val f = vm.formulario
    var confirmarBaja by remember { mutableStateOf(false) }

    LaunchedEffect(vm.terminado) { if (vm.terminado) onGuardado() }

    PantallaBase(
        titulo = if (f.esNuevo) "Nuevo material" else "Editar material",
        onVolver = onVolver
    ) { modifier ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(Espaciado.pantalla),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (!f.esNuevo) {
                Text(
                    text = "Clave ${f.id}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Campo("Nombre de la pieza", f.nombre) { vm.actualizar(f.copy(nombre = it)) }

            Campo(
                etiqueta = "Numero de parte",
                valor = f.numeroParte,
                apoyo = "Como viene grabado o etiquetado en la pieza"
            ) { vm.actualizar(f.copy(numeroParte = it)) }

            Campo("Fabricante", f.fabricante) { vm.actualizar(f.copy(fabricante = it)) }

            Campo(
                etiqueta = "Descripcion",
                valor = f.descripcion,
                lineas = 3
            ) { vm.actualizar(f.copy(descripcion = it)) }

            Seccion("Categoria")
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(Espaciado.pequeno)
            ) {
                Categoria.entries.forEach { categoria ->
                    FilterChip(
                        selected = f.categoria == categoria,
                        onClick = { vm.actualizar(f.copy(categoria = categoria)) },
                        label = { Text(categoria.etiqueta) }
                    )
                }
            }

            Seccion("Medidas")
            Campo(
                etiqueta = "Medidas completas",
                valor = f.medidas,
                apoyo = "Por ejemplo: 25 x 52 x 15 mm"
            ) { vm.actualizar(f.copy(medidas = it)) }
            Campo(
                etiqueta = "Medida clave",
                valor = f.medidaClave,
                apoyo = "La que se pide en almacen: M10, 1/2 pulgada, eje 25 mm"
            ) { vm.actualizar(f.copy(medidaClave = it)) }

            Seccion("Existencias")

            // Ajuste rapido: contar en el rack y corregir de uno en uno o de
            // diez en diez es la operacion mas frecuente del almacen, y
            // hacerlo con el teclado numerico es lento y propenso a erratas.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Espaciado.pequeno),
                modifier = Modifier.fillMaxWidth()
            ) {
                BotonAjuste("-10") { vm.actualizar(f.copy(existencia = ajustar(f.existencia, -10))) }
                BotonAjuste("-1") { vm.actualizar(f.copy(existencia = ajustar(f.existencia, -1))) }
                Campo(
                    etiqueta = "Existencia",
                    valor = f.existencia,
                    numerico = true,
                    modifier = Modifier.weight(1f)
                ) { vm.actualizar(f.copy(existencia = it)) }
                BotonAjuste("+1") { vm.actualizar(f.copy(existencia = ajustar(f.existencia, 1))) }
                BotonAjuste("+10") { vm.actualizar(f.copy(existencia = ajustar(f.existencia, 10))) }
            }

            Campo(
                etiqueta = "Minimo de seguridad",
                valor = f.existenciaMinima,
                numerico = true,
                apoyo = "Por debajo de esta cantidad la pieza se marca en ambar"
            ) { vm.actualizar(f.copy(existenciaMinima = it)) }
            Campo("Unidad de medida", f.unidadMedida) { vm.actualizar(f.copy(unidadMedida = it)) }

            Seccion("Ubicacion en almacen")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Campo("Almacen", f.almacen, modifier = Modifier.weight(1f)) {
                    vm.actualizar(f.copy(almacen = it))
                }
                Campo("Pasillo", f.pasillo, modifier = Modifier.weight(1f)) {
                    vm.actualizar(f.copy(pasillo = it))
                }
                Campo("Rack", f.rack, modifier = Modifier.weight(1f)) {
                    vm.actualizar(f.copy(rack = it))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Campo("Nivel", f.nivel, numerico = true, modifier = Modifier.weight(1f)) {
                    vm.actualizar(f.copy(nivel = it))
                }
                Campo("Posicion", f.posicion, numerico = true, modifier = Modifier.weight(1f)) {
                    vm.actualizar(f.copy(posicion = it))
                }
            }

            vm.error?.let { mensaje ->
                Text(
                    text = mensaje,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(Modifier.height(Espaciado.minimo))

            BotonPrincipal(
                onClick = { vm.guardar() },
                cargando = vm.guardando,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = TamanosControles.alturaMinima)
            ) {
                Text(
                    text = if (f.esNuevo) "Dar de alta" else "Guardar cambios",
                    style = MaterialTheme.typography.labelLarge
                )
            }

            if (!f.esNuevo) {
                BotonSecundario(
                    onClick = { confirmarBaja = true },
                    enabled = !vm.guardando,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Dar de baja del catalogo")
                }
            }

            Spacer(Modifier.height(Espaciado.medio))
        }
    }

    if (confirmarBaja) {
        AlertDialog(
            onDismissRequest = { confirmarBaja = false },
            title = { Text("Dar de baja ${f.nombre}") },
            text = {
                Text(
                    "Dejara de aparecer en el catalogo y en las busquedas, y se " +
                        "borraran sus vistas de referencia. El historial que ya " +
                        "apunta a esta pieza se conserva."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.darDeBaja()
                    confirmarBaja = false
                }) { Text("Dar de baja") }
            },
            dismissButton = {
                TextButton(onClick = { confirmarBaja = false }) { Text("Cancelar") }
            }
        )
    }
}

/** Nunca baja de cero: una existencia negativa no significa nada en un almacen. */
private fun ajustar(valor: String, delta: Int): String =
    ((valor.toIntOrNull() ?: 0) + delta).coerceAtLeast(0).toString()

@Composable
private fun BotonAjuste(texto: String, onClick: () -> Unit) {
    BotonSecundario(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
        modifier = Modifier.heightIn(min = TamanosControles.alturaMinima)
    ) {
        Text(texto, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun Seccion(titulo: String) {
    Text(
        text = titulo,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 6.dp)
    )
}

@Composable
private fun Campo(
    etiqueta: String,
    valor: String,
    modifier: Modifier = Modifier,
    apoyo: String? = null,
    numerico: Boolean = false,
    lineas: Int = 1,
    alCambiar: (String) -> Unit
) {
    CampoTexto(
        value = valor,
        onValueChange = alCambiar,
        label = { Text(etiqueta) },
        supportingText = apoyo?.let { { Text(it) } },
        singleLine = lineas == 1,
        minLines = lineas,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (numerico) KeyboardType.Number else KeyboardType.Text
        ),
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth()
    )
}
