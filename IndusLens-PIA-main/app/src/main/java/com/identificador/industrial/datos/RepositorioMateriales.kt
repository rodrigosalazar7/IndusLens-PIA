package com.identificador.industrial.datos

import com.identificador.industrial.datos.local.MaterialDao
import com.identificador.industrial.datos.modelo.Material
import kotlinx.coroutines.flow.Flow

/**
 * Unico punto de acceso al catalogo desde la interfaz.
 *
 * La interfaz nunca habla con el DAO directamente. Esa separacion es la que
 * permitira, en su momento, cambiar el origen de los datos por Supabase sin
 * tocar una sola pantalla: bastaria con otra implementacion de esta clase.
 */
class RepositorioMateriales(private val dao: MaterialDao) {

    fun observarTodos(): Flow<List<Material>> = dao.observarTodos()

    fun observarPorId(id: String): Flow<Material?> = dao.observarPorId(id)

    suspend fun obtenerPorId(id: String): Material? = dao.obtenerPorId(id)

    /** Con el texto vacio devuelve el catalogo completo en lugar de nada. */
    fun buscar(texto: String): Flow<List<Material>> =
        if (texto.isBlank()) dao.observarTodos() else dao.buscar(texto.trim())

    /**
     * Busqueda por numero de parte. Normaliza lo recibido antes de consultar,
     * de modo que da igual como venga escrito desde el OCR.
     */
    suspend fun buscarPorNumeroParte(numeroParte: String): Material? =
        dao.buscarPorNumeroParte(Material.normalizarNumeroParte(numeroParte))

    suspend fun guardar(material: Material) = dao.insertar(material)

    suspend fun actualizar(material: Material) = dao.actualizar(material)

    suspend fun eliminar(material: Material) = dao.eliminar(material)

    suspend fun contar(): Int = dao.contar()

    /**
     * Siguiente clave libre con el formato M-001, M-002...
     *
     * Se calcula sobre el maximo existente y no sobre el numero de materiales:
     * si alguien da de baja una pieza intermedia, contar daria una clave ya
     * usada y el alta fallaria por duplicado.
     */
    suspend fun siguienteClave(): String {
        val numeros = dao.todosLosIds().mapNotNull { id ->
            id.removePrefix("M-").toIntOrNull()
        }
        val siguiente = (numeros.maxOrNull() ?: 0) + 1
        return "M-" + siguiente.toString().padStart(3, '0')
    }

    suspend fun pasillosDe(almacen: String): List<String> = dao.pasillosDe(almacen)

    suspend fun racksDe(almacen: String, pasillo: String): List<String> =
        dao.racksDe(almacen, pasillo)

    suspend fun vecinosDeRack(
        almacen: String,
        pasillo: String,
        rack: String,
        excluir: String
    ): List<Material> = dao.vecinosDeRack(almacen, pasillo, rack, excluir)
}
