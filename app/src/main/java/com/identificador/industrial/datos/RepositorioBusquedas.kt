package com.identificador.industrial.datos

import com.identificador.industrial.datos.local.BusquedaDao
import com.identificador.industrial.datos.modelo.Busqueda
import com.identificador.industrial.datos.modelo.MetodoIdentificacion
import kotlinx.coroutines.flow.Flow

class RepositorioBusquedas(private val dao: BusquedaDao) {

    fun observarRecientes(): Flow<List<Busqueda>> = dao.observarRecientes()

    /**
     * Registra un intento de identificacion.
     *
     * Tambien se guardan los intentos fallidos (`materialId` nulo) a proposito:
     * revelan piezas que existen en planta pero no estan dadas de alta, que es
     * informacion valiosa para quien mantiene el catalogo.
     */
    suspend fun registrar(
        materialId: String?,
        usuarioId: String,
        metodo: MetodoIdentificacion,
        confianza: Float,
        textoDetectado: String?,
        fotoPath: String?
    ) {
        dao.insertar(
            Busqueda(
                materialId = materialId,
                usuarioId = usuarioId,
                fechaHora = System.currentTimeMillis(),
                metodo = metodo,
                confianza = confianza,
                textoDetectado = textoDetectado?.take(500),
                fotoPath = fotoPath
            )
        )
    }

    suspend fun borrarTodo() = dao.borrarTodo()
}
