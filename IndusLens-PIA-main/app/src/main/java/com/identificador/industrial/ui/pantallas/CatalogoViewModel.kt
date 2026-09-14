package com.identificador.industrial.ui.pantallas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.identificador.industrial.datos.RepositorioMateriales
import com.identificador.industrial.datos.modelo.Material
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

/**
 * Catalogo de materiales con buscador, para la pantalla de administracion.
 */
class CatalogoViewModel(
    private val repositorio: RepositorioMateriales
) : ViewModel() {

    private val _textoBusqueda = MutableStateFlow("")
    val textoBusqueda: StateFlow<String> = _textoBusqueda.asStateFlow()

    /**
     * El `debounce` evita lanzar una consulta por cada tecla pulsada: espera a
     * que el usuario deje de escribir un cuarto de segundo. El `flatMapLatest`
     * cancela la consulta anterior si llega texto nuevo, de modo que nunca
     * puede pintarse en pantalla el resultado de una busqueda ya obsoleta.
     */
    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val materiales: StateFlow<List<Material>> = _textoBusqueda
        .debounce(250)
        .flatMapLatest { texto -> repositorio.buscar(texto) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun cambiarBusqueda(texto: String) {
        _textoBusqueda.value = texto
    }

    fun limpiarBusqueda() {
        _textoBusqueda.value = ""
    }
}
