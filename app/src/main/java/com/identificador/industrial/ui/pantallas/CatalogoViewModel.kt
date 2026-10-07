package com.identificador.industrial.ui.pantallas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.identificador.industrial.datos.RepositorioMateriales
import com.identificador.industrial.datos.modelo.Categoria
import com.identificador.industrial.datos.modelo.Material
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Catalogo de materiales con buscador. Lo comparten la pantalla de
 * administracion y la de consulta del catalogo.
 */
class CatalogoViewModel(
    private val repositorio: RepositorioMateriales
) : ViewModel() {

    private val _textoBusqueda = MutableStateFlow("")
    val textoBusqueda: StateFlow<String> = _textoBusqueda.asStateFlow()

    /** Categoria elegida en los filtros; null significa "todas". */
    private val _categoria = MutableStateFlow<Categoria?>(null)
    val categoria: StateFlow<Categoria?> = _categoria.asStateFlow()

    /**
     * El `debounce` evita lanzar una consulta por cada tecla pulsada: espera a
     * que el usuario deje de escribir un cuarto de segundo. El `flatMapLatest`
     * cancela la consulta anterior si llega texto nuevo, de modo que nunca
     * puede pintarse en pantalla el resultado de una busqueda ya obsoleta.
     *
     * La categoria se aplica en memoria sobre el resultado del texto: el
     * catalogo de un almacen cabe de sobra y asi no hace falta una consulta
     * SQL por cada combinacion de texto y categoria.
     */
    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val materiales: StateFlow<List<Material>> = _textoBusqueda
        .debounce(250)
        .flatMapLatest { texto -> repositorio.buscar(texto) }
        .combine(_categoria) { lista, categoria ->
            if (categoria == null) lista else lista.filter { it.categoria == categoria }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    /**
     * Categorias que de verdad tienen materiales, para no ofrecer filtros que
     * siempre darian una lista vacia.
     */
    val categoriasDisponibles: StateFlow<List<Categoria>> = repositorio.observarTodos()
        .map { lista -> lista.map { it.categoria }.distinct().sortedBy { it.ordinal } }
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

    /** Pulsar la categoria ya elegida la quita y vuelve a mostrar todas. */
    fun alternarCategoria(categoria: Categoria) {
        _categoria.value = if (_categoria.value == categoria) null else categoria
    }
}
