package com.identificador.industrial.datos

import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDatabase
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.identificador.industrial.datos.local.BaseDatos
import com.identificador.industrial.datos.local.MigracionUsuarios4a5
import com.identificador.industrial.datos.modelo.Rol
import com.identificador.industrial.datos.modelo.UsuarioEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UsuariosIntegracionTest {
    private val contexto = InstrumentationRegistry.getInstrumentation().targetContext
    private val archivo = "pruebas-integridad-usuarios.db"

    @get:Rule val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(), BaseDatos::class.java
    )

    @After fun limpiarSoloBaseDePruebas() { contexto.deleteDatabase(archivo) }

    private fun insertar(db: SupportSQLiteDatabase, id: String, usuario: String, activo: Int = 1) {
        db.execSQL("INSERT INTO usuarios VALUES (?,?,?,?,?,?,?)", arrayOf<Any>(
            id, usuario, "Nombre $id", "ADMINISTRADOR", "hash-$id", "sal-$id", activo
        ))
    }

    private fun migrar(): SupportSQLiteDatabase =
        helper.runMigrationsAndValidate(archivo, 5, true, MigracionUsuarios4a5)

    @Test fun migracionConservaCuentasCredencialesEHistorial() {
        helper.createDatabase(archivo, 4).use { db ->
            insertar(db, "u1", " Admin ", 0)
            db.execSQL("""INSERT INTO historial_busquedas
                (id, materialId, usuarioId, fechaHora, metodo, confianza, textoDetectado, fotoPath)
                VALUES (1, NULL, 'u1', 100, 'OCR', 0.8, 'pieza', NULL)""")
        }
        migrar().use { db ->
            db.query("SELECT * FROM usuarios").use { c ->
                assertTrue(c.moveToFirst())
                assertEquals("u1", c.getString(c.getColumnIndexOrThrow("id")))
                assertEquals("admin", c.getString(c.getColumnIndexOrThrow("usuario")))
                assertEquals("hash-u1", c.getString(c.getColumnIndexOrThrow("hashClave")))
                assertEquals("sal-u1", c.getString(c.getColumnIndexOrThrow("sal")))
                assertEquals("ADMINISTRADOR", c.getString(c.getColumnIndexOrThrow("rol")))
                assertEquals(0, c.getInt(c.getColumnIndexOrThrow("activo")))
                assertFalse(c.moveToNext())
            }
            db.query("SELECT usuarioId FROM historial_busquedas WHERE id = 1").use { c ->
                assertTrue(c.moveToFirst()); assertEquals("u1", c.getString(0))
            }
        }
    }

    @Test fun colisionesHistoricasNoBorranNiFusionanCuentas() {
        helper.createDatabase(archivo, 4).use { db ->
            insertar(db, "u1", "Admin")
            insertar(db, "u2", "admin")
            insertar(db, "u3", "admin-2")
            insertar(db, "u4", " ADMIN ")
        }
        migrar().use { db ->
            val esperado = mapOf("u1" to "admin-3", "u2" to "admin", "u3" to "admin-2", "u4" to "admin-4")
            db.query("SELECT id, usuario, hashClave FROM usuarios").use { c ->
                assertEquals(4, c.count)
                while (c.moveToNext()) {
                    assertEquals(esperado[c.getString(0)], c.getString(1))
                    assertEquals("hash-${c.getString(0)}", c.getString(2))
                }
            }
        }
    }

    @Test fun indiceYDisparadoresRechazanInsercionesYActualizacionesInvalidas() {
        helper.createDatabase(archivo, 4).use { insertar(it, "u1", "admin") }
        migrar().use { db ->
            listOf("admin", "ADMIN", " admin ", "", " ").forEach { nombre ->
                assertThrows(SQLiteConstraintException::class.java) { insertar(db, "u2", nombre) }
            }
            listOf("id", "usuario", "nombre", "hashClave", "sal").forEach { campo ->
                assertThrows(SQLiteConstraintException::class.java) {
                    db.execSQL("UPDATE usuarios SET $campo = ' ' WHERE id = 'u1'")
                }
            }
            assertThrows(SQLiteConstraintException::class.java) {
                db.execSQL("UPDATE usuarios SET rol = 'INVITADO' WHERE id = 'u1'")
            }
            assertThrows(SQLiteConstraintException::class.java) {
                db.execSQL("UPDATE usuarios SET activo = 7 WHERE id = 'u1'")
            }
            db.query("SELECT hashClave FROM usuarios").use { c ->
                assertEquals(1, c.count); assertTrue(c.moveToFirst()); assertEquals("hash-u1", c.getString(0))
            }
        }
    }

    @Test fun datosHistoricosInvalidosReviertenMigracionSinPerdida() {
        helper.createDatabase(archivo, 4).use { insertar(it, "u1", " ") }
        assertThrows(IllegalArgumentException::class.java) { migrar().close() }
        SQLiteDatabase.openDatabase(contexto.getDatabasePath(archivo).path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
            assertEquals(4, db.version)
            db.rawQuery("SELECT usuario, hashClave FROM usuarios", null).use { c ->
                assertTrue(c.moveToFirst()); assertEquals(" ", c.getString(0)); assertEquals("hash-u1", c.getString(1))
            }
        }
    }

    @Test fun versionesAntiguasTambienLleganAlEsquema5() {
        helper.createDatabase(archivo, 1).close()
        helper.runMigrationsAndValidate(archivo, 5, true,
            BaseDatos.MIGRACION_1_2, BaseDatos.MIGRACION_2_3,
            BaseDatos.MIGRACION_3_4, MigracionUsuarios4a5
        ).close()
    }

    @Test fun instalacionNuevaSiembraUsuariosYProtecciones(): Unit = runBlocking {
        val db = BaseDatos.crear(contexto, archivo)
        try {
            assertEquals(3, db.usuarioDao().contar())
            val repo = RepositorioUsuarios(db.usuarioDao())
            assertEquals(Rol.ADMINISTRADOR, repo.autenticar("  ADMIN  ", "admin")?.rol)
            assertNull(repo.autenticar("admin", "incorrecta"))
            assertNull(repo.autenticar("   ", "admin"))
            assertNull(repo.autenticar("admin", ""))
            assertThrows(SQLiteConstraintException::class.java) {
                db.openHelper.writableDatabase.execSQL("UPDATE usuarios SET nombre = ''")
            }
        } finally { db.close() }
    }

    @Test fun daoRechazaDuplicadoSinReemplazarCuentaYLoteEsAtomico() = runBlocking {
        val db = BaseDatos.crear(contexto, archivo)
        try {
            val dao = db.usuarioDao()
            val original = dao.buscarPorUsuario("admin")!!
            val nueva = UsuarioEntity.crear("test-nuevo", "nuevo", "Nuevo", Rol.OPERADOR, "hash", "sal")
            try {
                dao.insertarTodos(listOf(nueva, original.copy(id = "otro-id", hashClave = "otra-clave")))
                fail("Se acepto un usuario duplicado")
            } catch (_: SQLiteConstraintException) { /* Esperado: ABORT del lote. */ }
            assertEquals(original, dao.buscarPorUsuario("admin"))
            assertNull(dao.buscarPorUsuario("nuevo"))
            assertEquals(3, dao.contar())
        } finally { db.close() }
    }

    @Test fun cuentaMigradaPuedeEntrarYCuentasInactivasNo() = runBlocking {
        val sal = Claves.generarSal()
        helper.createDatabase(archivo, 4).use { db ->
            insertar(db, "u1", " ADMIN ")
            insertar(db, "u2", "Inactivo", 0)
            db.execSQL("UPDATE usuarios SET hashClave = ?, sal = ?", arrayOf(Claves.hash("clave", sal), sal))
        }
        // Usa la misma construccion que la app: tambien verifica la migracion en una apertura real.
        val db = BaseDatos.crear(contexto, archivo)
        try {
            val repo = RepositorioUsuarios(db.usuarioDao())
            assertEquals("u1", repo.autenticar("AdMiN", "clave")?.id)
            assertNull(repo.autenticar("inactivo", "clave"))
        } finally { db.close() }
    }
}
