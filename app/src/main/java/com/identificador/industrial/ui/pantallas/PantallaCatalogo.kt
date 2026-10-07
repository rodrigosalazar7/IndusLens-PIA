package com.identificador.industrial.ui.pantallas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.identificador.industrial.datos.modelo.Categoria
import com.identificador.industrial.ui.Fabricas
import com.identificador.industrial.ui.componentes.PantallaBase

/**
 * Consulta del catalogo para cualquier rol.
 *
 * Es la misma lista que ve el administrador, pero de solo lectura: sirve al
 * operador para buscar una pieza por nombre o numero de parte cuando ya lo
 * conoce y no necesita fotografiarla. Ademas filtra por categoria.
 */
@Composable
fun PantallaCatalogo(
    onVolver: () -> Unit,
    onVerDetalle: (String) -> Unit
) {
    val vm: CatalogoViewModel = viewModel(factory = Fabricas.Catalogo)
    val texto by vm.textoBusqueda.collectAsStateWithLifecycle()
    val categoria by vm.categoria.collectAsStateWithLifecycle()
    val categorias by vm.categoriasDisponibles.collectAsStateWithLifecycle()
    val materiales by vm.materiales.collectAsStateWithLifecycle()

    PantallaBase(
        titulo = "Catalogo de materiales",
        onVolver = onVolver
    ) { modifier ->
        Column(modifier = modifier.fillMaxSize()) {

            BuscadorCatalogo(
                texto = texto,
                onCambiar = vm::cambiarBusqueda,
                onLimpiar = vm::limpiarBusqueda
            )

            // Con una sola categoria el filtro no aportaria nada. Si hay uno
            // elegido se sigue mostrando, para que nunca quede un filtro
            // activo que la persona no pueda ver ni quitar.
            if (categorias.size > 1 || categoria != null) {
                FiltroCategorias(
                    categorias = (categorias + listOfNotNull(categoria)).distinct(),
                    elegida = categoria,
                    onElegir = vm::alternarCategoria
                )
            }

            ListaMateriales(
                materiales = materiales,
                textoBusqueda = texto,
                onVerDetalle = onVerDetalle
            )
        }
    }
}

@Composable
private fun FiltroCategorias(
    categorias: List<Categoria>,
    elegida: Categoria?,
    onElegir: (Categoria) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categorias, key = { it.name }) { c ->
            FilterChip(
                selected = c == elegida,
                onClick = { onElegir(c) },
                label = { Text(c.etiqueta) }
            )
        }
    }
}
