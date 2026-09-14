package com.identificador.industrial.datos

import com.identificador.industrial.datos.local.MaterialDao
import com.identificador.industrial.datos.modelo.Material
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Unico punto de acceso al catalogo desde la interfaz.
 *
 * La interfaz nunca habla con el DAO ni con Firestore directamente. Room es
 * la cache sin conexion y BaseRemota es la fuente compartida cuando hay una
 * cuenta verificada.
 */
class RepositorioMateriales(
    private val dao: MaterialDao,
    private val baseRemota: BaseRemota? = null
) {

    private val alcanceSincronizacion = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var trabajoSincronizacion: Job? = null

    fun observarTodos(): Flow<List<Material>> = dao.observarTodos()

    fun observarPorId(id: String): Flow<Material?> = dao.observarPorId(id)

    suspend fun obtenerPorId(id: String): Material? = dao.obtenerPorId(id)

    /** Con el texto vacio devuelve el catalogo completo en lugar de nada. */
    fun buscar(texto: String): Flow<List<Material>> =
        if (texto.isBlank()) dao.observarTodos() else dao.buscar(escaparComodines(texto.trim()))

    /**
     * Escapa los comodines de SQLite (`%` y `_`) antes de armar el LIKE.
     *
     * Sin esto, buscar un numero de parte que trajera de verdad uno de estos
     * caracteres (por ejemplo "M_027") no lo buscaria tal cual, sino que "_"
     * se interpretaria como "cualquier caracter" y devolveria coincidencias
     * que no tienen nada que ver.
     */
    private fun escaparComodines(texto: String): String =
        texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")

    /**
     * Busqueda por numero de parte. Normaliza lo recibido antes de consultar,
     * de modo que da igual como venga escrito desde el OCR.
     */
    suspend fun buscarPorNumeroParte(numeroParte: String): Material? =
        dao.buscarPorNumeroParte(Material.normalizarNumeroParte(numeroParte))

    suspend fun guardar(material: Material) {
        // Para una cuenta Firebase, Firestore manda. Si las reglas rechazan
        // el cambio, tampoco se altera la cache y la app muestra el error.
        if (baseRemota?.sesionRemotaActiva == true) baseRemota.guardar(material)
        dao.insertar(material)
    }

    suspend fun actualizar(material: Material) = guardar(material)

    suspend fun eliminar(material: Material) {
        if (baseRemota?.sesionRemotaActiva == true) baseRemota.eliminar(material.id)
        dao.eliminar(material)
    }

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

    /**
     * Conecta la cache Room con la base central despues de iniciar sesion.
     *
     * Si la coleccion es nueva y quien entra es administrador, publica el
     * catalogo semilla una sola vez. Despues escucha los cambios en vivo.
     */
    suspend fun conectarBaseRemota(esAdministrador: Boolean) {
        val remota = baseRemota ?: return
        if (!remota.sesionRemotaActiva) return

        val materialesRemotos = remota.obtenerMateriales()
        if (materialesRemotos.isEmpty() && esAdministrador) {
            remota.guardarTodos(dao.obtenerTodosIncluyendoInactivos())
        } else if (materialesRemotos.isNotEmpty()) {
            dao.insertarTodos(materialesRemotos)
        }

        trabajoSincronizacion?.cancel()
        trabajoSincronizacion = alcanceSincronizacion.launch {
            remota.observarMateriales()
                .catch { /* La cache local mantiene la app util sin red. */ }
                .collectLatest { materiales ->
                    if (materiales.isNotEmpty()) dao.insertarTodos(materiales)
                }
        }
    }

    fun desconectarBaseRemota() {
        trabajoSincronizacion?.cancel()
        trabajoSincronizacion = null
    }
}
