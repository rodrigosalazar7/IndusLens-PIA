package com.identificador.industrial.ui.pantallas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.identificador.industrial.datos.RepositorioBusquedas
import com.identificador.industrial.datos.RepositorioMateriales
import com.identificador.industrial.datos.modelo.Busqueda
import com.identificador.industrial.datos.modelo.Material
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Una busqueda del historial junto con la pieza a la que llego, si llego a alguna.
 */
data class EntradaHistorial(
    val busqueda: Busqueda,
    val material: Material?
)

class HistorialViewModel(
    private val repositorioBusquedas: RepositorioBusquedas,
    private val repositorioMateriales: RepositorioMateriales
) : ViewModel() {

    /**
     * El historial guarda solo el identificador del material, no una copia de
     * sus datos. Aqui se resuelve el nombre en el momento de mostrarlo, para
     * que si alguien corrige el catalogo el historial refleje la correccion en
     * lugar de arrastrar datos viejos.
     */
    val entradas: StateFlow<List<EntradaHistorial>> = repositorioBusquedas
        .observarRecientes()
        .map { lista ->
            lista.map { busqueda ->
                EntradaHistorial(
                    busqueda = busqueda,
                    material = busqueda.materialId?.let { repositorioMateriales.obtenerPorId(it) }
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun borrarTodo() {
        viewModelScope.launch { repositorioBusquedas.borrarTodo() }
    }
}
