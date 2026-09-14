package com.identificador.industrial.ui.pantallas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.identificador.industrial.datos.RepositorioMateriales
import com.identificador.industrial.datos.RepositorioVisual
import com.identificador.industrial.datos.modelo.Material
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Los tres estados posibles de la ficha. Se distinguen a proposito "cargando"
 * y "no encontrado": si se usara un simple `Material?`, la pantalla mostraria
 * "material no encontrado" durante el instante previo a que llegue el dato,
 * que es exactamente el tipo de parpadeo que hace desconfiar de una app.
 */
sealed interface EstadoDetalle {
    data object Cargando : EstadoDetalle
    data class Encontrado(val material: Material) : EstadoDetalle
    data object NoEncontrado : EstadoDetalle
}

class DetalleViewModel(
    private val repositorio: RepositorioMateriales,
    private val repositorioVisual: RepositorioVisual
) : ViewModel() {

    private val idSolicitado = MutableStateFlow<String?>(null)

    /** Cuantas vistas de referencia tiene la pieza para la busqueda visual. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val vistas: StateFlow<Int> = idSolicitado
        .filterNotNull()
        .flatMapLatest { id -> repositorioVisual.observarVistas(id) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val estado: StateFlow<EstadoDetalle> = idSolicitado
        .filterNotNull()
        .flatMapLatest { id ->
            repositorio.observarPorId(id).map { material ->
                if (material == null) EstadoDetalle.NoEncontrado
                else EstadoDetalle.Encontrado(material)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = EstadoDetalle.Cargando
        )

    fun cargar(materialId: String) {
        if (idSolicitado.value != materialId) idSolicitado.value = materialId
    }
}
