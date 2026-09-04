package com.identificador.industrial.ui.pantallas

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.identificador.industrial.datos.RepositorioMateriales
import com.identificador.industrial.datos.modelo.Material
import kotlinx.coroutines.launch

/** Distribucion del almacen alrededor de la pieza buscada. */
data class PlanoAlmacen(
    val almacen: String,
    val pasillos: List<String>,
    val racksDelPasillo: List<String>
)

data class EstadoUbicacion(
    val material: Material? = null,
    val plano: PlanoAlmacen? = null,
    val vecinos: List<Material> = emptyList(),
    val cargando: Boolean = true
)

class UbicacionViewModel(
    private val repositorio: RepositorioMateriales
) : ViewModel() {

    var estado by mutableStateOf(EstadoUbicacion())
        private set

    private var cargado: String? = null

    fun cargar(materialId: String) {
        if (cargado == materialId) return
        cargado = materialId

        viewModelScope.launch {
            val material = repositorio.obtenerPorId(materialId)
            if (material == null) {
                estado = EstadoUbicacion(cargando = false)
                return@launch
            }

            val u = material.ubicacion
            estado = EstadoUbicacion(
                material = material,
                plano = PlanoAlmacen(
                    almacen = u.almacen,
                    pasillos = repositorio.pasillosDe(u.almacen),
                    racksDelPasillo = repositorio.racksDe(u.almacen, u.pasillo)
                ),
                vecinos = repositorio.vecinosDeRack(u.almacen, u.pasillo, u.rack, material.id),
                cargando = false
            )
        }
    }
}
