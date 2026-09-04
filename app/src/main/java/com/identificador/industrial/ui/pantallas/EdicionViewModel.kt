package com.identificador.industrial.ui.pantallas

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.identificador.industrial.datos.RepositorioMateriales
import com.identificador.industrial.datos.RepositorioVisual
import com.identificador.industrial.datos.modelo.Categoria
import com.identificador.industrial.datos.modelo.Material
import com.identificador.industrial.datos.modelo.Ubicacion
import kotlinx.coroutines.launch

/**
 * Campos del formulario como texto.
 *
 * Se guardan en crudo, tal y como se escriben, y solo se convierten a numero
 * al validar. Si se guardaran ya como Int, borrar el ultimo digito de un campo
 * dejaria una cadena vacia que habria que interpretar como cero, y el usuario
 * veria aparecer un 0 que no ha escrito.
 */
data class FormularioMaterial(
    val id: String? = null,
    val nombre: String = "",
    val descripcion: String = "",
    val numeroParte: String = "",
    val fabricante: String = "",
    val categoria: Categoria = Categoria.TORNILLERIA,
    val unidadMedida: String = "Pieza",
    val existencia: String = "0",
    val existenciaMinima: String = "0",
    val medidas: String = "",
    val medidaClave: String = "",
    val almacen: String = "A",
    val pasillo: String = "01",
    val rack: String = "A",
    val nivel: String = "1",
    val posicion: String = "1"
) {
    val esNuevo: Boolean get() = id == null
}

class EdicionViewModel(
    private val repositorio: RepositorioMateriales,
    private val repositorioVisual: RepositorioVisual
) : ViewModel() {

    var formulario by mutableStateOf(FormularioMaterial())
        private set

    var error by mutableStateOf<String?>(null)
        private set

    var guardando by mutableStateOf(false)
        private set

    var terminado by mutableStateOf(false)
        private set

    private var cargado = false

    fun cargar(materialId: String?) {
        if (cargado) return
        cargado = true

        if (materialId.isNullOrBlank()) return

        viewModelScope.launch {
            val m = repositorio.obtenerPorId(materialId) ?: return@launch
            formulario = FormularioMaterial(
                id = m.id,
                nombre = m.nombre,
                descripcion = m.descripcion,
                numeroParte = m.numeroParte,
                fabricante = m.fabricante,
                categoria = m.categoria,
                unidadMedida = m.unidadMedida,
                existencia = m.existencia.toString(),
                existenciaMinima = m.existenciaMinima.toString(),
                medidas = m.medidas.orEmpty(),
                medidaClave = m.medidaClave.orEmpty(),
                almacen = m.ubicacion.almacen,
                pasillo = m.ubicacion.pasillo,
                rack = m.ubicacion.rack,
                nivel = m.ubicacion.nivel.toString(),
                posicion = m.ubicacion.posicion.toString()
            )
        }
    }

    fun actualizar(nuevo: FormularioMaterial) {
        formulario = nuevo
        error = null
    }

    fun guardar() {
        if (guardando) return
        val f = formulario

        val fallo = validar(f)
        if (fallo != null) {
            error = fallo
            return
        }

        viewModelScope.launch {
            guardando = true
            try {
                // Si es alta, se comprueba que el numero de parte no exista ya.
                // El indice unico lo impediria de todas formas, pero avisar
                // claramente es mejor que dejar reventar la insercion.
                val existente = repositorio.buscarPorNumeroParte(f.numeroParte)
                if (existente != null && existente.id != f.id) {
                    error = "Ya hay un material con el numero de parte " +
                        "${existente.numeroParte}: ${existente.nombre}"
                    guardando = false
                    return@launch
                }

                val material = Material(
                    id = f.id ?: repositorio.siguienteClave(),
                    nombre = f.nombre.trim(),
                    descripcion = f.descripcion.trim(),
                    numeroParte = f.numeroParte.trim(),
                    fabricante = f.fabricante.trim().ifBlank { "Generico" },
                    categoria = f.categoria,
                    unidadMedida = f.unidadMedida.trim().ifBlank { "Pieza" },
                    existencia = f.existencia.toIntOrNull() ?: 0,
                    existenciaMinima = f.existenciaMinima.toIntOrNull() ?: 0,
                    ubicacion = Ubicacion(
                        almacen = f.almacen.trim().uppercase(),
                        pasillo = f.pasillo.trim(),
                        rack = f.rack.trim().uppercase(),
                        nivel = f.nivel.toIntOrNull() ?: 1,
                        posicion = f.posicion.toIntOrNull() ?: 1
                    ),
                    medidas = f.medidas.trim().ifBlank { null },
                    medidaClave = f.medidaClave.trim().ifBlank { null }
                )

                repositorio.guardar(material)
                terminado = true
            } catch (e: Exception) {
                error = e.message ?: "No se pudo guardar el material"
            } finally {
                guardando = false
            }
        }
    }

    /**
     * Baja logica, no borrado.
     *
     * El material se marca como inactivo en lugar de eliminarlo: el historial
     * de busquedas apunta a el, y borrarlo dejaria registros huerfanos que ya
     * no se podrian explicar. Ademas, en un almacen real, "esta pieza ya no se
     * usa" y "esta pieza nunca existio" no son lo mismo.
     */
    fun darDeBaja() {
        val id = formulario.id ?: return
        viewModelScope.launch {
            guardando = true
            try {
                repositorio.obtenerPorId(id)?.let { material ->
                    repositorio.actualizar(material.copy(activo = false))
                    repositorioVisual.borrarHuellas(id)
                }
                terminado = true
            } catch (e: Exception) {
                error = e.message ?: "No se pudo dar de baja el material"
            } finally {
                guardando = false
            }
        }
    }

    private fun validar(f: FormularioMaterial): String? = when {
        f.nombre.isBlank() -> "El nombre es obligatorio"
        f.numeroParte.isBlank() -> "El numero de parte es obligatorio"
        f.existencia.toIntOrNull() == null -> "La existencia debe ser un numero"
        f.existenciaMinima.toIntOrNull() == null -> "El minimo debe ser un numero"
        (f.existencia.toIntOrNull() ?: 0) < 0 -> "La existencia no puede ser negativa"
        f.nivel.toIntOrNull() == null -> "El nivel debe ser un numero"
        f.posicion.toIntOrNull() == null -> "La posicion debe ser un numero"
        f.pasillo.isBlank() -> "Indica el pasillo"
        f.rack.isBlank() -> "Indica el rack"
        else -> null
    }
}
