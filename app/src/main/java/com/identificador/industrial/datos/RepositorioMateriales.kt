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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

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
    private val escrituras = Mutex()

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
    suspend fun buscarPorNumeroParte(numeroParte: String): Material? {
        val normalizado = Material.normalizarNumeroParte(numeroParte)
        if (normalizado.isBlank()) return null
        return dao.obtenerTodosIncluyendoInactivos().firstOrNull {
            it.activo && it.numeroParteNormalizado == normalizado
        }
    }

    /** Genera la clave y realiza el alta bajo el mismo bloqueo, sin reemplazar registros. */
    suspend fun crear(material: Material): Material = escrituras.withLock {
        val id = if (baseRemota?.sesionRemotaActiva == true) {
            "M-" + UUID.randomUUID().toString()
        } else siguienteClave()
        material.copy(id = id).also { insertarValidado(it) }
    }

    suspend fun guardar(material: Material) = escrituras.withLock {
        insertarValidado(material)
    }

    private suspend fun insertarValidado(material: Material) {
        require(dao.obtenerPorId(material.id) == null) { "Esa clave ya existe. Vuelve a intentar el alta." }
        validar(material)
        // Para una cuenta Firebase, Firestore manda. Si las reglas rechazan
        // el cambio, tampoco se altera la cache y la app muestra el error.
        if (baseRemota?.sesionRemotaActiva == true) baseRemota.guardar(material)
        dao.insertar(material)
    }

    suspend fun actualizar(material: Material, esperado: Material? = null) = escrituras.withLock {
        val actual = dao.obtenerPorId(material.id)
            ?: error("La pieza ya no existe. Vuelve al catálogo.")
        check(esperado == null || actual == esperado) {
            "La pieza cambió mientras la editabas. Vuelve a abrirla para no sobrescribir esos cambios."
        }
        validar(material)
        if (baseRemota?.sesionRemotaActiva == true) baseRemota.guardar(material)
        check(dao.actualizar(material) == 1) { "No se pudo actualizar la pieza." }
    }

    private suspend fun validar(material: Material) {
        require(material.id.isNotBlank() && material.nombre.isNotBlank()) { "La pieza necesita clave y nombre." }
        require(material.numeroParteNormalizado.isNotBlank()) { "El número de parte debe contener letras o números." }
        require(material.existencia >= 0 && material.existenciaMinima >= 0) { "Las existencias no pueden ser negativas." }
        val u = material.ubicacion
        require(u.almacen.isNotBlank() && u.pasillo.isNotBlank() && u.rack.isNotBlank() &&
            u.nivel > 0 && u.posicion > 0) { "Completa una ubicación válida." }
        val duplicado = dao.obtenerTodosIncluyendoInactivos().firstOrNull {
            it.id != material.id && it.numeroParteNormalizado == material.numeroParteNormalizado
        }
        require(duplicado == null) {
            "El número de parte ya pertenece a ${duplicado?.nombre}" +
                (if (duplicado?.activo == false) " (dada de baja)." else ".") +
                " Usa un código distinto; la pieza existente se conserva."
        }
    }

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
            id.takeIf { it.startsWith("M-") }?.removePrefix("M-")?.toLongOrNull()
        }
        val maximo = numeros.maxOrNull() ?: 0L
        check(maximo < Long.MAX_VALUE) { "No se pudo generar otra clave interna." }
        val siguiente = maximo + 1
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
            escrituras.withLock { dao.insertarTodos(materialesRemotos) }
        }

        trabajoSincronizacion?.cancel()
        trabajoSincronizacion = alcanceSincronizacion.launch {
            remota.observarMateriales()
                .catch { /* La cache local mantiene la app util sin red. */ }
                .collectLatest { materiales ->
                    if (materiales.isNotEmpty()) escrituras.withLock { dao.insertarTodos(materiales) }
                }
        }
    }

    fun desconectarBaseRemota() {
        trabajoSincronizacion?.cancel()
        trabajoSincronizacion = null
    }
}
