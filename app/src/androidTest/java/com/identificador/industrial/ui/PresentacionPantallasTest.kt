package com.identificador.industrial.ui

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import com.identificador.industrial.datos.*
import com.identificador.industrial.datos.local.BaseDatos
import com.identificador.industrial.datos.modelo.*
import com.identificador.industrial.sesion.*
import com.identificador.industrial.ui.pantallas.*
import com.identificador.industrial.ui.theme.IndusLensTheme
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*

/** Pantallas reales sobre Room aislada. No usa ni escribe en el catálogo de la aplicación. */
class PresentacionPantallasTest {
    @get:Rule val compose = createComposeRule()
    private val contexto = InstrumentationRegistry.getInstrumentation().targetContext
    private val archivo = "pruebas-ui-presentacion.db"
    private val archivoSesion = "pruebas-ui-presentacion-sesion"
    private lateinit var db: BaseDatos
    private lateinit var repo: RepositorioMateriales
    private val almacen = ViewModelStore()
    private var contador = 0
    private val piezasIniciales = CatalogoInicial.materiales.size

    @Before fun preparar() {
        contexto.deleteDatabase(archivo)
        contexto.getSharedPreferences(archivoSesion, 0).edit().clear().commit()
        db = BaseDatos.crear(contexto, archivo)
        repo = RepositorioMateriales(db.materialDao())
        runBlocking { repo.contar() }
    }
    @After fun limpiarSoloDatosDePrueba() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync { almacen.clear() }
        db.close()
        contexto.deleteDatabase(archivo)
        contexto.getSharedPreferences(archivoSesion, 0).edit().clear().commit()
    }
    private fun <T : ViewModel> conservar(vm: T): T = vm.also { almacen.put("vm-${contador++}", it) }
    private fun mostrar(inicial: String = "login", puedeEditar: Boolean = true) {
        compose.setContent {
            var pantalla by remember { mutableStateOf(inicial) }
            var id by remember { mutableStateOf<String?>(null) }
            IndusLensTheme {
                when (pantalla) {
                    "login" -> {
                        val sesion = remember { conservar(SesionViewModel(
                            RepositorioUsuarios(db.usuarioDao()),
                            AutenticacionCorreo(contexto, BaseRemota(contexto)),
                            SesionPersistida(contexto, archivoSesion), repo
                        )) }
                        PantallaLogin(sesion) { pantalla = "catalogo" }
                    }
                    "catalogo" -> {
                        val vm = remember { conservar(CatalogoViewModel(repo)) }
                        PantallaAdmin({}, { id = it; pantalla = "detalle" },
                            { id = it; pantalla = "editar" }, { id = null; pantalla = "editar" },
                            puedeEditar = puedeEditar, vm = vm)
                    }
                    "editar" -> {
                        val vm = remember { conservar(EdicionViewModel(repo)) }
                        PantallaEditarMaterial(id, { pantalla = "catalogo" },
                            { id = it; pantalla = "detalle" },
                            onBaja = { pantalla = "catalogo" }, vm = vm)
                    }
                    "detalle" -> {
                        val vm = remember { conservar(DetalleViewModel(repo,
                            RepositorioVisual(db.embeddingDao(), db.materialDao()))) }
                        PantallaDetalle(id!!, { pantalla = "catalogo" }, {}, {},
                            puedeEditar, { pantalla = "editar" }, vm)
                    }
                }
            }
        }
    }
    private fun esperar(tag: String) {
        compose.waitUntil(10_000) { compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
    }
    private fun campo(campo: CampoMaterial, texto: String) {
        compose.onNodeWithTag("campo_${campo.name}").performScrollTo().performTextReplacement(texto)
    }
    private fun pieza() = Material("", "Llave del kit", "", "KIT-01", "Genérico",
        Categoria.entries.first(), "Pieza", 3, 1, Ubicacion("A", "01", "A", 1, 1))

    @Test fun loginCatalogoAltaDetalleYEdicionFuncionanConPersistencia() {
        mostrar()
        compose.onNodeWithText("Correo o usuario").performTextInput("admin")
        compose.onNodeWithText("Contrasena").performTextInput("incorrecta")
        compose.onNodeWithText("Entrar").performScrollTo().performClick()
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("Usuario o contrasena incorrectos").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Contrasena").performTextReplacement("admin")
        compose.onNodeWithText("Entrar").performScrollTo().performClick()
        esperar("nueva_pieza")
        compose.onNodeWithTag("nueva_pieza").performClick()
        esperar("guardar_material")
        compose.onNodeWithTag("guardar_material").performClick()
        compose.onNodeWithTag("error_formulario").assertExists()
        campo(CampoMaterial.NOMBRE, "Martillo del kit")
        campo(CampoMaterial.NUMERO_PARTE, "KIT-001")
        compose.onNodeWithTag("seleccionar_categoria").performScrollTo().performClick()
        compose.onNodeWithText("Herramientas").performScrollTo().performClick()
        campo(CampoMaterial.EXISTENCIA, "7")
        compose.onNodeWithTag("cambiar_ubicacion").performScrollTo().performClick()
        campo(CampoMaterial.ALMACEN, "B")
        campo(CampoMaterial.RACK, "C")
        compose.onNodeWithTag("guardar_material").performClick()
        esperar("detalle_pieza")
        compose.onNodeWithText("Martillo del kit").assertExists()
        compose.onNodeWithTag("editar_pieza").performClick()
        esperar("guardar_material")
        campo(CampoMaterial.EXISTENCIA, "12")
        compose.onNodeWithTag("guardar_material").performClick()
        esperar("detalle_pieza")
        val actual = runBlocking { repo.buscarPorNumeroParte("kit001")!! }
        assertEquals(12, actual.existencia)
        assertEquals(Categoria.HERRAMIENTA, actual.categoria)
        assertEquals("B", actual.ubicacion.almacen)
        assertEquals("C", actual.ubicacion.rack)
        compose.onNodeWithContentDescription("Volver").performClick()
        esperar("buscar_catalogo")
        compose.onNodeWithTag("buscar_catalogo").performTextInput("inexistente")
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("Limpiar búsqueda").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Limpiar búsqueda").performClick()
        esperar("pieza_${actual.id}")
    }
    @Test fun duplicadoMuestraErrorYPermiteCorregirSinPerderFormulario() {
        runBlocking { repo.crear(pieza()) }
        mostrar("editar")
        esperar("guardar_material")
        campo(CampoMaterial.NOMBRE, "Otra pieza")
        campo(CampoMaterial.NUMERO_PARTE, "kit / 01")
        compose.onNodeWithTag("guardar_material").performClick()
        esperar("error_formulario")
        compose.onNodeWithTag("campo_NOMBRE").assertTextContains("Otra pieza")
        assertEquals(piezasIniciales + 1, runBlocking { repo.contar() })
        campo(CampoMaterial.NUMERO_PARTE, "KIT-02")
        compose.onNodeWithTag("guardar_material").performClick()
        esperar("detalle_pieza")
        assertEquals(piezasIniciales + 2, runBlocking { repo.contar() })
    }
    @Test fun salirPideConfirmacionYCancelarConservaElBorrador() {
        mostrar("editar")
        esperar("guardar_material")
        campo(CampoMaterial.NOMBRE, "Pendiente del kit")
        compose.onNodeWithContentDescription("Volver").performClick()
        compose.onNodeWithText("¿Salir sin guardar?").assertIsDisplayed()
        compose.onNodeWithText("Seguir editando").performClick()
        compose.onNodeWithTag("campo_NOMBRE").assertTextContains("Pendiente del kit")
        compose.onNodeWithContentDescription("Volver").performClick()
        compose.onNodeWithText("Descartar cambios").performClick()
        esperar("nueva_pieza")
        assertEquals(piezasIniciales, runBlocking { repo.contar() })
    }
    @Test fun operadorPuedeConsultarPeroNoVeAccionesAdministrativas() {
        val material = runBlocking { repo.crear(pieza()) }
        mostrar("catalogo", puedeEditar = false)
        esperar("pieza_${material.id}")
        compose.onNodeWithTag("nueva_pieza").assertDoesNotExist()
        compose.onNodeWithContentDescription("Editar ${material.nombre}").assertDoesNotExist()
        compose.onNodeWithTag("pieza_${material.id}").performClick()
        esperar("detalle_pieza")
        compose.onNodeWithTag("editar_pieza").assertDoesNotExist()
        compose.onNodeWithTag("ver_ubicacion").assertExists()
    }

    @Test fun resultadoDetalleYEdicionConservanFotoYDatosDeLuis() {
        val imagen = android.graphics.Bitmap.createBitmap(1200, 600, android.graphics.Bitmap.Config.ARGB_8888)
        val lienzo = android.graphics.Canvas(imagen)
        lienzo.drawColor(android.graphics.Color.WHITE)
        val pincel = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.BLACK
            typeface = android.graphics.Typeface.MONOSPACE
            textSize = 110f
            textAlign = android.graphics.Paint.Align.CENTER
        }
        lienzo.drawText("TOR-001", 600f, 350f, pincel)
        val archivoFoto = java.io.File(contexto.cacheDir, "prueba-ui-luis.png")
        archivoFoto.outputStream().use { imagen.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        imagen.recycle()
        lateinit var identificacion: IdentificacionViewModel
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            identificacion = conservar(IdentificacionViewModel(
                repo, RepositorioBusquedas(db.busquedaDao()), RepositorioVisual(db.embeddingDao(), db.materialDao()),
                { error("La etiqueta debe resolverse por OCR") },
                { error("No se necesita clasificacion general") },
                { error("Esta prueba nunca usa la nube") }
            ))
        }
        try {
            compose.setContent {
                var pantalla by remember { mutableStateOf("resultado") }
                IndusLensTheme {
                    when (pantalla) {
                        "resultado" -> PantallaResultado(identificacion, {},
                            { pantalla = "detalle" }, {}, {}, {}, null, {})
                        "detalle" -> PantallaDetalle("M-027", {}, {}, {},
                            puedeEditar = true, onEditar = { pantalla = "editar" },
                            vm = remember { conservar(DetalleViewModel(repo,
                                RepositorioVisual(db.embeddingDao(), db.materialDao()))) })
                        "editar" -> PantallaEditarMaterial("M-027", { pantalla = "detalle" },
                            { pantalla = "detalle" },
                            vm = remember { conservar(EdicionViewModel(repo)) })
                    }
                }
            }
            compose.runOnIdle {
                identificacion.registrarFoto(android.net.Uri.fromFile(archivoFoto))
                identificacion.analizar(contexto, "u3")
            }
            compose.waitUntil(30_000) {
                identificacion.estado.value is EstadoIdentificacion.Identificado ||
                    identificacion.estado.value is EstadoIdentificacion.Fallo
            }
            assertTrue(identificacion.estado.value.toString(),
                identificacion.estado.value is EstadoIdentificacion.Identificado)
            val descripcionFoto = "Foto de referencia de la pieza en el catalogo"
            compose.waitUntil(10_000) {
                compose.onAllNodesWithContentDescription(descripcionFoto).fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("Ver detalle del material").performScrollTo().performClick()
            esperar("detalle_pieza")
            compose.waitUntil(10_000) {
                compose.onAllNodesWithContentDescription(descripcionFoto).fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithTag("editar_pieza").performScrollTo().performClick()
            esperar("guardar_material")
            campo(CampoMaterial.EXISTENCIA, "91")
            compose.onNodeWithTag("guardar_material").performClick()
            esperar("detalle_pieza")
            val actual = runBlocking { repo.obtenerPorId("M-027")!! }
            assertEquals(91, actual.existencia)
            assertEquals("M-027.jpg", actual.fotoReferencia)
            assertEquals(10, runBlocking { db.embeddingDao().contarDeMaterial("M-027") })
        } finally { archivoFoto.delete() }
    }
}
