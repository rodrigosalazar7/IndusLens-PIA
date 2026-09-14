package com.identificador.industrial.ui.pantallas

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.identificador.industrial.datos.RepositorioMateriales
import com.identificador.industrial.datos.modelo.Categoria
import com.identificador.industrial.datos.modelo.Material
import com.identificador.industrial.datos.modelo.Ubicacion
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.util.Locale

class EdicionViewModel(
    private val repositorio: RepositorioMateriales,
    private val estadoGuardado: SavedStateHandle = SavedStateHandle()
) : ViewModel() {
    var formulario by mutableStateOf(estadoGuardado.get<FormularioMaterial>("borrador") ?: FormularioMaterial())
        private set
    var errores by mutableStateOf<Map<CampoMaterial, String>>(emptyMap())
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var errorCarga by mutableStateOf<String?>(null)
        private set
    var cargando by mutableStateOf(true)
        private set
    var guardando by mutableStateOf(false)
        private set
    var terminado by mutableStateOf(false)
        private set
    var dadaDeBaja by mutableStateOf(false)
        private set
    var idGuardado: String? = null
        private set

    private var inicializado = false
    private var original: Material? = null
    private var inicial = FormularioMaterial()
    val tieneCambios: Boolean get() = !cargando && errorCarga == null && formulario != inicial

    fun cargar(
        materialId: String?,
        sugerenciaNombre: String? = null,
        sugerenciaCategoria: Categoria? = null
    ) {
        if (inicializado) return
        inicializado = true
        errorCarga = null
        cargando = true
        if (materialId.isNullOrBlank()) {
            inicial = FormularioMaterial(
                nombre = sugerenciaNombre?.replaceFirstChar { it.uppercase() }.orEmpty(),
                categoria = sugerenciaCategoria ?: Categoria.TORNILLERIA
            )
            formulario = estadoGuardado.get<FormularioMaterial>("borrador")
                ?.takeIf { it.esNuevo } ?: inicial
            cargando = false
            return
        }
        viewModelScope.launch {
            try {
                val material = repositorio.obtenerPorId(materialId)
                if (material == null || !material.activo) {
                    errorCarga = "Esta pieza no existe o fue dada de baja. Vuelve al catálogo."
                    return@launch
                }
                original = material
                inicial = FormularioMaterial.desde(material)
                formulario = estadoGuardado.get<FormularioMaterial>("borrador")
                    ?.takeIf { it.id == materialId } ?: inicial
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                errorCarga = "No se pudo cargar la pieza. Revisa el almacenamiento e intenta de nuevo."
            } finally {
                cargando = false
            }
        }
    }

    fun reintentar(materialId: String?) {
        if (cargando) return
        inicializado = false
        cargar(materialId)
    }

    fun actualizar(nuevo: FormularioMaterial) {
        if (cargando || guardando || terminado || errorCarga != null) return
        formulario = nuevo.copy(id = formulario.id)
        estadoGuardado["borrador"] = formulario
        if (errores.isNotEmpty()) errores = formulario.errores()
        error = null
    }

    fun guardar() {
        if (cargando || guardando || terminado || errorCarga != null) return
        val f = formulario
        errores = f.errores()
        if (errores.isNotEmpty()) return
        error = null
        guardando = true // Antes de lanzar la corrutina: ignora dobles pulsaciones.
        viewModelScope.launch {
            try {
                val material = Material(
                    id = f.id.orEmpty(), nombre = f.nombre.trim(),
                    descripcion = f.descripcion.trim(), numeroParte = f.numeroParte.trim(),
                    fabricante = f.fabricante.trim().ifBlank { "Genérico" },
                    categoria = f.categoria, unidadMedida = f.unidadMedida.trim().ifBlank { "Pieza" },
                    existencia = f.existencia.trim().toInt(),
                    existenciaMinima = f.existenciaMinima.trim().toInt(),
                    ubicacion = Ubicacion(
                        almacen = f.almacen.trim().uppercase(Locale.ROOT), pasillo = f.pasillo.trim(),
                        rack = f.rack.trim().uppercase(Locale.ROOT),
                        nivel = f.nivel.trim().toInt(), posicion = f.posicion.trim().toInt()
                    ),
                    medidas = f.medidas.trim().ifBlank { null },
                    medidaClave = f.medidaClave.trim().ifBlank { null },
                    fotoReferencia = original?.fotoReferencia,
                    activo = original?.activo ?: true
                )
                val guardado = if (f.esNuevo) {
                    repositorio.crear(material)
                } else {
                    repositorio.actualizar(material, esperado = original)
                    material
                }
                idGuardado = guardado.id
                estadoGuardado.remove<FormularioMaterial>("borrador")
                terminado = true
            } catch (e: CancellationException) {
                throw e
            } catch (e: IllegalArgumentException) {
                error = e.message
            } catch (e: IllegalStateException) {
                error = e.message
            } catch (_: Exception) {
                error = "No se pudo guardar. Tus datos siguen en el formulario; intenta de nuevo."
            } finally {
                guardando = false
            }
        }
    }

    /** Baja lógica: conserva el registro y sus referencias, pero no participa en búsquedas. */
    fun darDeBaja() {
        if (cargando || guardando || terminado || errorCarga != null) return
        val material = original ?: return
        error = null
        guardando = true
        viewModelScope.launch {
            try {
                repositorio.actualizar(material.copy(activo = false), esperado = material)
                dadaDeBaja = true
                idGuardado = material.id
                estadoGuardado.remove<FormularioMaterial>("borrador")
                terminado = true
            } catch (e: CancellationException) {
                throw e
            } catch (e: IllegalStateException) {
                error = e.message
            } catch (_: Exception) {
                error = "No se pudo dar de baja. Intenta de nuevo."
            } finally {
                guardando = false
            }
        }
    }
}
