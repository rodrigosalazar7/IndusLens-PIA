package com.identificador.industrial.datos

import android.net.Uri
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.platform.app.InstrumentationRegistry
import com.identificador.industrial.datos.local.BaseDatos
import com.identificador.industrial.datos.modelo.EmbeddingMaterial
import com.identificador.industrial.datos.modelo.Material
import com.identificador.industrial.ia.Embebedor
import com.identificador.industrial.ia.Fotos
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import java.io.File

/** Bases aisladas: nunca abre ni borra el inventario real de la aplicacion. */
class CatalogoLuisIntegracionTest {
    private val contexto = InstrumentationRegistry.getInstrumentation().targetContext
    private val archivo = "pruebas-integracion-luis.db"
    @get:Rule val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(), BaseDatos::class.java
    )
    @Before fun preparar() { contexto.deleteDatabase(archivo) }
    @After fun limpiar() { contexto.deleteDatabase(archivo) }

    private suspend fun conBase(prueba: suspend (BaseDatos) -> Unit) {
        val db = BaseDatos.crear(contexto, archivo)
        try { prueba(db) } finally { db.close() }
    }

    private fun insertar(db: SupportSQLiteDatabase, m: Material) {
        db.execSQL("""
            INSERT INTO materiales
            (id,nombre,descripcion,numeroParte,fabricante,categoria,unidadMedida,
             existencia,existenciaMinima,almacen,pasillo,rack,nivel,posicion,
             medidas,medidaClave,fotoReferencia,activo)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
        """.trimIndent(), arrayOf<Any?>(
            m.id,m.nombre,m.descripcion,m.numeroParte,m.fabricante,m.categoria.name,m.unidadMedida,
            m.existencia,m.existenciaMinima,m.ubicacion.almacen,m.ubicacion.pasillo,
            m.ubicacion.rack,m.ubicacion.nivel,m.ubicacion.posicion,
            m.medidas,m.medidaClave,m.fotoReferencia,if (m.activo) 1 else 0
        ))
    }

    @Test fun nuevaInstalacionCargaCuatroPiezasY43VistasSinDuplicarlas() = runBlocking {
        repeat(2) {
            conBase { db ->
                assertEquals(4, db.materialDao().contar())
                assertEquals(43, db.embeddingDao().contar(EmbeddingMaterial.MODELO_ACTUAL))
                for (m in CatalogoInicial.materiales) {
                    assertEquals(m, db.materialDao().obtenerPorId(m.id))
                    val foto = Fotos.cargarDeAssets(contexto, m.fotoReferencia!!)
                    assertNotNull(m.id, foto)
                    foto?.recycle()
                }
                assertTrue(db.embeddingDao().todos(EmbeddingMaterial.MODELO_ACTUAL)
                    .all { it.comoFloats().size == 1024 })
            }
        }
    }

    @Test fun actualizarDesdeVersion4ConservaCambiosDeLuisUsuarioEHistorial() = runBlocking {
        val modificada = CatalogoInicial.materiales.first().copy(existencia = 99)
        helper.createDatabase(archivo, 4).use { db ->
            insertar(db, modificada)
            db.execSQL("INSERT INTO usuarios VALUES ('u9',' Operador ','Equipo','OPERADOR','hash','sal',1)")
            db.execSQL("""INSERT INTO historial_busquedas
                (id,materialId,usuarioId,fechaHora,metodo,confianza,textoDetectado,fotoPath)
                VALUES (1,'M-027','u9',100,'OCR',0.9,'TOR-001',NULL)""")
            db.execSQL("INSERT INTO embeddings (materialId,vector,modelo,fechaHora) VALUES (?,?,?,?)",
                arrayOf<Any>("M-027", EmbeddingMaterial.aBytes(floatArrayOf(1f,0f)),
                    EmbeddingMaterial.MODELO_ACTUAL, 100L))
        }
        conBase { db ->
            assertEquals(5, db.openHelper.writableDatabase.version)
            assertEquals(modificada, db.materialDao().obtenerPorId("M-027"))
            assertEquals(4, db.materialDao().contar())
            assertEquals(1, db.embeddingDao().contarDeMaterial("M-027"))
            assertEquals("hash", db.usuarioDao().buscarPorUsuario("operador")?.hashClave)
            db.openHelper.writableDatabase.query("SELECT textoDetectado FROM historial_busquedas WHERE id = 1")
                .use { c -> assertTrue(c.moveToFirst()); assertEquals("TOR-001", c.getString(0)) }
        }
    }

    @Test fun actualizarVersion5AgregaKitSinBorrarPiezaPropia() = runBlocking {
        val propia = CatalogoInicial.materiales.first().copy(
            id = "M-001", nombre = "Martillo propio", numeroParte = "DEMO-001",
            existencia = 12, fotoReferencia = null
        )
        helper.createDatabase(archivo, 5).use { insertar(it, propia) }
        repeat(2) {
            conBase { db ->
                assertEquals(propia, db.materialDao().obtenerPorId("M-001"))
                assertEquals(5, db.materialDao().contar())
                assertEquals(43, db.embeddingDao().contar(EmbeddingMaterial.MODELO_ACTUAL))
            }
        }
    }

    @Test fun colisionesYBajasNoSeSobrescribenNiRecibenHuellasAjenas() = runBlocking {
        val porId = CatalogoInicial.materiales[0].copy(numeroParte = "PROPIO", nombre = "Pieza propia")
        val porCodigo = CatalogoInicial.materiales[1].copy(id = "M-100", numeroParte = "tor / 002")
        val baja = CatalogoInicial.materiales[2].copy(activo = false, existencia = 2)
        helper.createDatabase(archivo, 5).use { db ->
            listOf(porId, porCodigo, baja).forEach { insertar(db, it) }
        }
        repeat(2) {
            conBase { db ->
                assertEquals(porId, db.materialDao().obtenerPorId(porId.id))
                assertEquals(porCodigo, db.materialDao().obtenerPorId(porCodigo.id))
                assertEquals(baja, db.materialDao().obtenerPorId(baja.id))
                assertNull(db.materialDao().obtenerPorId("M-028"))
                assertEquals(14, db.embeddingDao().contar(EmbeddingMaterial.MODELO_ACTUAL))
                assertTrue(db.embeddingDao().todos(EmbeddingMaterial.MODELO_ACTUAL)
                    .all { it.materialId == "M-030" })
            }
        }
    }

    /** Usa las fotos de referencia de Luis; no acredita fotos nuevas ni camara fisica. */
    @Test fun modeloAndroidAsociaLasCuatroFotosConSusReferenciasPrecargadas() = runBlocking {
        conBase { db ->
            db.materialDao().contar()
            val visual = RepositorioVisual(db.embeddingDao(), db.materialDao())
            val modelo = Embebedor(contexto)
            val temporal = File(contexto.cacheDir, "prueba-foto-referencia-luis.jpg")
            try {
                for (m in CatalogoInicial.materiales) {
                    contexto.assets.open("fotos_catalogo/${m.fotoReferencia}").use { entrada ->
                        temporal.outputStream().use { entrada.copyTo(it) }
                    }
                    val imagen = Fotos.cargarParaAnalisis(contexto, Uri.fromFile(temporal))!!
                    val candidatas = visual.buscarParecidos(modelo.vector(imagen))
                    imagen.recycle()
                    assertEquals("Primera candidata para ${m.numeroParte}", m.id, candidatas.firstOrNull()?.material?.id)
                }
            } finally { modelo.cerrar(); temporal.delete() }
        }
    }
}
