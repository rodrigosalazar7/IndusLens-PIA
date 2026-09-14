package com.identificador.industrial.datos

import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.identificador.industrial.datos.local.BaseDatos
import com.identificador.industrial.datos.modelo.Categoria
import com.identificador.industrial.datos.modelo.Material
import com.identificador.industrial.datos.modelo.Ubicacion
import com.identificador.industrial.ui.pantallas.EdicionViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PiezasIntegracionTest {
    private val contexto = InstrumentationRegistry.getInstrumentation().targetContext
    private val archivo = "pruebas-presentacion-piezas.db"
    private lateinit var db: BaseDatos
    private lateinit var repo: RepositorioMateriales
    private val piezasIniciales = CatalogoInicial.materiales.size

    @Before fun abrir() {
        contexto.deleteDatabase(archivo)
        db = BaseDatos.crear(contexto, archivo)
        repo = RepositorioMateriales(db.materialDao())
    }
    @After fun cerrar() { db.close(); contexto.deleteDatabase(archivo) }

    private fun pieza(codigo: String = "KIT-001") = Material(
        "", "Martillo del kit", "", codigo, "Genérico", Categoria.entries.first(),
        "Pieza", 4, 1, Ubicacion("A", "01", "B", 2, 3),
        fotoReferencia = "foto_del_kit"
    )
    private suspend fun esperar(fin: () -> Boolean) = withTimeout(10_000) {
        while (!withContext(Dispatchers.Main) { fin() }) delay(25)
    }
    private suspend fun editar(id: String): EdicionViewModel {
        val vm = withContext(Dispatchers.Main) { EdicionViewModel(repo).also { it.cargar(id) } }
        esperar { !vm.cargando }
        return vm
    }

    @Test fun altaPersisteAlReabrirLaBaseYSePuedeBuscar(): Unit = runBlocking {
        assertEquals(piezasIniciales, repo.contar())
        val creada = repo.crear(pieza())
        assertEquals("M-031", creada.id)
        db.close()
        db = BaseDatos.crear(contexto, archivo)
        repo = RepositorioMateriales(db.materialDao())
        assertEquals(creada, repo.obtenerPorId(creada.id))
        assertEquals(creada, repo.buscar("Martillo").first().single())
        assertEquals(creada, repo.buscarPorNumeroParte("kit / 001"))
    }
    @Test fun numeroDeParteNormalizadoDuplicadoNoReemplazaLaPieza(): Unit = runBlocking {
        val original = repo.crear(pieza("AB/123"))
        try { repo.crear(pieza(" ab-123 ")); fail("Aceptó un duplicado") }
        catch (_: IllegalArgumentException) { }
        assertEquals(piezasIniciales + 1, repo.contar())
        assertEquals(original, repo.obtenerPorId(original.id))
    }
    @Test fun altasSimultaneasNoCompartenClave(): Unit = runBlocking {
        val creadas = (1..8).map { n -> async(Dispatchers.Default) { repo.crear(pieza("KIT-$n")) } }.awaitAll()
        assertEquals(8, creadas.map { it.id }.toSet().size)
        assertEquals(piezasIniciales + 8, repo.contar())
    }
    @Test fun daoNoReemplazaPorClaveONumeroParte(): Unit = runBlocking {
        val original = repo.crear(pieza())
        try { db.materialDao().insertar(original.copy(nombre = "Intruso")); fail("Reemplazó la clave") }
        catch (_: android.database.sqlite.SQLiteConstraintException) { }
        try { db.materialDao().insertar(original.copy(id = "otro")); fail("Reemplazó la pieza") }
        catch (_: android.database.sqlite.SQLiteConstraintException) { }
        assertEquals(original, repo.obtenerPorId(original.id))
    }
    @Test fun edicionConservaFotoYGuardaUbicacionEInventario(): Unit = runBlocking {
        val original = repo.crear(pieza())
        val vm = editar(original.id)
        withContext(Dispatchers.Main) {
            vm.actualizar(vm.formulario.copy(existencia = "9", almacen = "C", rack = "D", nivel = "3"))
            vm.guardar()
            vm.guardar()
        }
        esperar { !vm.guardando }
        assertTrue(vm.error.orEmpty(), vm.terminado)
        val actual = repo.obtenerPorId(original.id)!!
        assertEquals(9, actual.existencia)
        assertEquals(Ubicacion("C", "01", "D", 3, 3), actual.ubicacion)
        assertEquals(original.fotoReferencia, actual.fotoReferencia)
        assertEquals(piezasIniciales + 1, repo.contar())
    }
    @Test fun piezaInexistenteNoSeConvierteEnAltaAccidental(): Unit = runBlocking {
        val vm = editar("no-existe")
        assertNotNull(vm.errorCarga)
        withContext(Dispatchers.Main) { vm.guardar() }
        assertFalse(vm.terminado)
        assertEquals(piezasIniciales, repo.contar())
    }
    @Test fun edicionDesactualizadaNoPisaCambiosRecientes(): Unit = runBlocking {
        val original = repo.crear(pieza())
        val vm = editar(original.id)
        repo.actualizar(original.copy(existencia = 30))
        withContext(Dispatchers.Main) {
            vm.actualizar(vm.formulario.copy(existencia = "8"))
            vm.guardar()
        }
        esperar { !vm.guardando }
        assertFalse(vm.terminado)
        assertTrue(vm.error.orEmpty().contains("cambió"))
        assertEquals(30, repo.obtenerPorId(original.id)!!.existencia)
    }
    @Test fun bajaLogicaExcluyeBusquedaVisualYReservaElNumero(): Unit = runBlocking {
        val original = repo.crear(pieza())
        val visual = RepositorioVisual(db.embeddingDao(), db.materialDao())
        visual.guardarHuella(original.id, floatArrayOf(1f, 0f))
        val vm = editar(original.id)
        withContext(Dispatchers.Main) { vm.darDeBaja(); vm.darDeBaja() }
        esperar { !vm.guardando }
        assertTrue(vm.dadaDeBaja)
        assertFalse(repo.obtenerPorId(original.id)!!.activo)
        assertFalse(repo.observarTodos().first().any { it.id == original.id })
        assertNull(repo.buscarPorNumeroParte(original.numeroParte))
        assertTrue(visual.buscarParecidos(floatArrayOf(1f, 0f)).isEmpty())
        assertEquals(1, visual.vistasDe(original.id))
        try { repo.crear(pieza()); fail("Reutilizó el código de una baja") }
        catch (_: IllegalArgumentException) { }
    }
    @Test fun borradorRestauradoConservaLoEscrito(): Unit = runBlocking {
        val guardado = SavedStateHandle()
        withContext(Dispatchers.Main) {
            val primero = EdicionViewModel(repo, guardado)
            primero.cargar(null)
            primero.actualizar(primero.formulario.copy(nombre = "Mi kit", numeroParte = "A-1"))
            val restaurado = EdicionViewModel(repo, guardado)
            restaurado.cargar(null)
            assertEquals("Mi kit", restaurado.formulario.nombre)
            assertTrue(restaurado.tieneCambios)
        }
    }
}
