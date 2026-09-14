package com.identificador.industrial.ui.pantallas

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.identificador.industrial.datos.modelo.Categoria
import com.identificador.industrial.ui.Fabricas
import com.identificador.industrial.ui.componentes.BotonPrincipal
import com.identificador.industrial.ui.componentes.BotonSecundario
import com.identificador.industrial.ui.componentes.CampoTexto
import com.identificador.industrial.ui.componentes.PantallaBase
import com.identificador.industrial.ui.theme.Espaciado

/** Alta y edición comparten validación, borrador y el mismo diseño. */
@Composable
fun PantallaEditarMaterial(
    materialId: String?,
    onVolver: () -> Unit,
    onGuardado: (String) -> Unit,
    sugerenciaNombre: String? = null,
    sugerenciaCategoria: Categoria? = null,
    onBaja: () -> Unit = onVolver,
    vm: EdicionViewModel = viewModel(factory = Fabricas.Edicion)
) {
    LaunchedEffect(materialId) { vm.cargar(materialId, sugerenciaNombre, sugerenciaCategoria) }
    val f = vm.formulario
    var confirmarBaja by rememberSaveable { mutableStateOf(false) }
    var confirmarSalida by rememberSaveable { mutableStateOf(false) }
    var editarUbicacion by rememberSaveable { mutableStateOf(false) }
    var verOpcionales by rememberSaveable { mutableStateOf(false) }
    var verCategorias by rememberSaveable { mutableStateOf(false) }
    val teclado = LocalSoftwareKeyboardController.current

    fun volver() {
        if (vm.guardando) return
        if (vm.tieneCambios) confirmarSalida = true else onVolver()
    }
    BackHandler(enabled = vm.tieneCambios || vm.guardando) { volver() }
    LaunchedEffect(vm.terminado) {
        if (vm.terminado) {
            if (vm.dadaDeBaja) onBaja() else vm.idGuardado?.let(onGuardado)
        }
    }
    LaunchedEffect(vm.errores) {
        if (vm.errores.keys.any { it in setOf(CampoMaterial.ALMACEN, CampoMaterial.PASILLO,
                CampoMaterial.RACK, CampoMaterial.NIVEL, CampoMaterial.POSICION) }) editarUbicacion = true
    }

    PantallaBase(
        titulo = if (materialId.isNullOrBlank()) "Nueva pieza" else "Editar pieza",
        onVolver = { volver() }
    ) { modifier ->
        when {
            vm.cargando -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            vm.errorCarga != null -> Column(
                modifier.fillMaxSize().padding(Espaciado.pantalla),
                verticalArrangement = Arrangement.Center
            ) {
                Text(vm.errorCarga.orEmpty(), color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(16.dp))
                BotonPrincipal(onClick = { vm.reintentar(materialId) }) { Text("Reintentar") }
                BotonSecundario(onClick = onVolver) { Text("Volver al catálogo") }
            }
            else -> Column(modifier.fillMaxSize().imePadding()) {
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState())
                        .padding(Espaciado.pantalla).testTag("formulario_pieza"),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        if (f.esNuevo) "Registra una pieza de tu kit. Los campos con * son obligatorios."
                        else "Clave ${f.id} · Los cambios se reflejarán en el catálogo.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    GrupoFormulario("Identificación") {
                        CampoPieza("Nombre de la pieza *", f.nombre, CampoMaterial.NOMBRE, vm) {
                            vm.actualizar(f.copy(nombre = it))
                        }
                        CampoPieza("Número de parte *", f.numeroParte, CampoMaterial.NUMERO_PARTE, vm,
                            apoyo = "Usa el código de la pieza o uno propio del kit, por ejemplo KIT-001.") {
                            vm.actualizar(f.copy(numeroParte = it))
                        }
                        Text("Categoría", style = MaterialTheme.typography.labelLarge)
                        Box {
                            BotonSecundario(onClick = { verCategorias = true },
                                enabled = !vm.guardando,
                                modifier = Modifier.fillMaxWidth().testTag("seleccionar_categoria")) {
                                Text("${f.categoria.etiqueta}  ▾")
                            }
                            DropdownMenu(expanded = verCategorias, onDismissRequest = { verCategorias = false }) {
                                Categoria.entries.forEach { categoria ->
                                    DropdownMenuItem(
                                        text = { Text(categoria.etiqueta) },
                                        onClick = {
                                            vm.actualizar(f.copy(categoria = categoria))
                                            verCategorias = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    GrupoFormulario("Inventario") {
                        CampoPieza("Existencia *", f.existencia, CampoMaterial.EXISTENCIA, vm, numerico = true) {
                            vm.actualizar(f.copy(existencia = it))
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(-10, -1, 1, 10).forEach { delta ->
                                BotonSecundario(
                                    onClick = { vm.actualizar(f.copy(existencia = ajustarExistencia(f.existencia, delta))) },
                                    enabled = !vm.guardando,
                                    modifier = Modifier.weight(1f).testTag("ajustar_$delta"),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                                ) { Text(if (delta > 0) "+$delta" else delta.toString()) }
                            }
                        }
                        CampoPieza("Mínimo de seguridad *", f.existenciaMinima, CampoMaterial.MINIMO, vm,
                            numerico = true, apoyo = "Se avisa cuando la existencia llega a este mínimo.") {
                            vm.actualizar(f.copy(existenciaMinima = it))
                        }
                        CampoPieza("Unidad de medida", f.unidadMedida, vm = vm) {
                            vm.actualizar(f.copy(unidadMedida = it))
                        }
                    }
                    GrupoFormulario("Ubicación") {
                        Text(
                            "Almacén ${f.almacen} · Pasillo ${f.pasillo} · Rack ${f.rack}\n" +
                                "Nivel ${f.nivel} · Posición ${f.posicion}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        TextButton(onClick = { editarUbicacion = !editarUbicacion }, enabled = !vm.guardando,
                            modifier = Modifier.testTag("cambiar_ubicacion")) {
                            Text(if (editarUbicacion) "Ocultar campos de ubicación" else "Cambiar ubicación")
                        }
                        if (editarUbicacion) {
                            CampoPieza("Almacén *", f.almacen, CampoMaterial.ALMACEN, vm) {
                                vm.actualizar(f.copy(almacen = it))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                CampoPieza("Pasillo *", f.pasillo, CampoMaterial.PASILLO, vm, Modifier.weight(1f)) {
                                    vm.actualizar(f.copy(pasillo = it))
                                }
                                CampoPieza("Rack *", f.rack, CampoMaterial.RACK, vm, Modifier.weight(1f)) {
                                    vm.actualizar(f.copy(rack = it))
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                CampoPieza("Nivel *", f.nivel, CampoMaterial.NIVEL, vm, Modifier.weight(1f), numerico = true) {
                                    vm.actualizar(f.copy(nivel = it))
                                }
                                CampoPieza("Posición *", f.posicion, CampoMaterial.POSICION, vm, Modifier.weight(1f), numerico = true) {
                                    vm.actualizar(f.copy(posicion = it))
                                }
                            }
                        }
                    }
                    GrupoFormulario("Información adicional") {
                        TextButton(onClick = { verOpcionales = !verOpcionales }, enabled = !vm.guardando,
                            modifier = Modifier.testTag("datos_opcionales")) {
                            Text(if (verOpcionales) "Ocultar datos opcionales" else "Fabricante, descripción y medidas")
                        }
                        if (verOpcionales) {
                            CampoPieza("Fabricante", f.fabricante, vm = vm) { vm.actualizar(f.copy(fabricante = it)) }
                            CampoPieza("Descripción", f.descripcion, vm = vm, lineas = 3) {
                                vm.actualizar(f.copy(descripcion = it))
                            }
                            CampoPieza("Medidas completas", f.medidas, vm = vm, apoyo = "Ejemplo: 25 x 52 x 15 mm") {
                                vm.actualizar(f.copy(medidas = it))
                            }
                            CampoPieza("Medida clave", f.medidaClave, vm = vm, apoyo = "Ejemplo: M10 o eje de 25 mm") {
                                vm.actualizar(f.copy(medidaClave = it))
                            }
                        }
                    }
                    if (!f.esNuevo) {
                        BotonSecundario(
                            onClick = { confirmarBaja = true }, enabled = !vm.guardando,
                            modifier = Modifier.fillMaxWidth().testTag("baja_material")
                        ) { Text("Dar de baja del catálogo", color = MaterialTheme.colorScheme.error) }
                    }
                }
                Surface(tonalElevation = 3.dp) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val mensaje = vm.error ?: vm.errores.values.firstOrNull()
                        if (mensaje != null) Text(mensaje,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.testTag("error_formulario"))
                        BotonPrincipal(
                            onClick = { teclado?.hide(); vm.guardar() },
                            cargando = vm.guardando,
                            modifier = Modifier.fillMaxWidth().testTag("guardar_material")
                        ) { Text(if (f.esNuevo) "Guardar pieza" else "Guardar cambios") }
                    }
                }
            }
        }
    }
    if (confirmarSalida) AlertDialog(
        onDismissRequest = { confirmarSalida = false },
        title = { Text("¿Salir sin guardar?") },
        text = { Text("Tienes cambios pendientes. Puedes seguir editando o descartarlos.") },
        confirmButton = { TextButton(onClick = { confirmarSalida = false; onVolver() }) { Text("Descartar cambios") } },
        dismissButton = { TextButton(onClick = { confirmarSalida = false }) { Text("Seguir editando") } }
    )
    if (confirmarBaja) AlertDialog(
        onDismissRequest = { confirmarBaja = false },
        title = { Text("¿Dar de baja esta pieza?") },
        text = { Text("${f.nombre} dejará de aparecer en el catálogo y el reconocimiento. Su registro e historial se conservan.") },
        confirmButton = { TextButton(onClick = { confirmarBaja = false; vm.darDeBaja() },
            modifier = Modifier.testTag("confirmar_baja")) { Text("Dar de baja") } },
        dismissButton = { TextButton(onClick = { confirmarBaja = false }) { Text("Cancelar") } }
    )
}

@Composable
private fun GrupoFormulario(titulo: String, contenido: @Composable ColumnScope.() -> Unit) {
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
            contenido()
        }
    }
}

@Composable
private fun CampoPieza(
    etiqueta: String, valor: String, campo: CampoMaterial? = null, vm: EdicionViewModel,
    modifier: Modifier = Modifier, apoyo: String? = null, numerico: Boolean = false,
    lineas: Int = 1, alCambiar: (String) -> Unit
) {
    val error = campo?.let { vm.errores[it] }
    CampoTexto(
        value = valor, onValueChange = alCambiar, label = { Text(etiqueta) },
        enabled = !vm.guardando, isError = error != null,
        supportingText = (error ?: apoyo)?.let { texto -> { Text(texto) } },
        singleLine = lineas == 1, minLines = lineas,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (numerico) KeyboardType.Number else KeyboardType.Text,
            imeAction = if (lineas == 1) ImeAction.Next else ImeAction.Default
        ),
        modifier = modifier.fillMaxWidth().testTag("campo_${campo?.name ?: etiqueta}")
    )
}
