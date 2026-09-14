package com.identificador.industrial.datos.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
// execSQL sobre SQLiteConnection es funcion de extension, no metodo de la clase.
import androidx.sqlite.execSQL
import androidx.sqlite.db.SupportSQLiteDatabase
import com.identificador.industrial.datos.CatalogoInicial
import com.identificador.industrial.datos.modelo.Busqueda
import com.identificador.industrial.datos.modelo.EmbeddingMaterial
import com.identificador.industrial.datos.modelo.Material
import com.identificador.industrial.datos.modelo.UsuarioEntity
import java.io.FileNotFoundException
import org.json.JSONArray

@Database(
    entities = [
        Material::class,
        UsuarioEntity::class,
        Busqueda::class,
        EmbeddingMaterial::class
    ],
    version = 5,
    exportSchema = true
)
@TypeConverters(Convertidores::class)
abstract class BaseDatos : RoomDatabase() {

    abstract fun materialDao(): MaterialDao
    abstract fun usuarioDao(): UsuarioDao
    abstract fun busquedaDao(): BusquedaDao
    abstract fun embeddingDao(): EmbeddingDao

    companion object {

        private const val NOMBRE_ARCHIVO = "identificador.db"

        /** Huellas precalculadas por el script de Python. Opcional. */
        private const val ARCHIVO_HUELLAS = "huellas_iniciales.json"

        /**
         * Version 1 -> 2: llega la tabla de huellas visuales.
         *
         * Se escribe la migracion en vez de recrear la base para no borrar los
         * datos de quien ya tenga la app instalada. En este proyecto el
         * catalogo se resiembra solo, pero el historial de busquedas no: se
         * perderia trabajo real del almacen.
         */
        val MIGRACION_1_2 = object : Migration(1, 2) {
            override fun migrate(connection: androidx.sqlite.SQLiteConnection) {
                connection.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `embeddings` (
                        `materialId` TEXT NOT NULL,
                        `vector` BLOB NOT NULL,
                        `modelo` TEXT NOT NULL,
                        `fechaHora` INTEGER NOT NULL,
                        PRIMARY KEY(`materialId`)
                    )
                    """.trimIndent()
                )
            }
        }

        /**
         * Version 2 -> 3: una pieza pasa a poder tener VARIAS huellas.
         *
         * Antes `materialId` era la clave primaria, asi que solo cabia una
         * huella por pieza y cada foto nueva pisaba la anterior. Ahora la
         * clave es un identificador propio y `materialId` solo lleva indice,
         * de modo que caben tantas vistas como haga falta.
         *
         * Se copian las huellas existentes en vez de borrar la tabla: quien ya
         * hubiera ensenado piezas no tiene por que volver a fotografiarlas.
         */
        val MIGRACION_2_3 = object : Migration(2, 3) {
            override fun migrate(connection: androidx.sqlite.SQLiteConnection) {
                connection.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `embeddings_nueva` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `materialId` TEXT NOT NULL,
                        `vector` BLOB NOT NULL,
                        `modelo` TEXT NOT NULL,
                        `fechaHora` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                connection.execSQL(
                    """
                    INSERT INTO `embeddings_nueva` (materialId, vector, modelo, fechaHora)
                    SELECT materialId, vector, modelo, fechaHora FROM `embeddings`
                    """.trimIndent()
                )
                connection.execSQL("DROP TABLE `embeddings`")
                connection.execSQL("ALTER TABLE `embeddings_nueva` RENAME TO `embeddings`")
                connection.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_embeddings_materialId` ON `embeddings` (`materialId`)"
                )
            }
        }

        @Volatile
        private var instancia: BaseDatos? = null

        fun obtener(contexto: Context): BaseDatos =
            instancia ?: synchronized(this) {
                instancia ?: crear(contexto).also { instancia = it }
            }

        internal fun crear(contexto: Context, nombreArchivo: String = NOMBRE_ARCHIVO): BaseDatos =
            Room.databaseBuilder(
                contexto.applicationContext,
                BaseDatos::class.java,
                nombreArchivo
            )
                .addCallback(Sembrado(contexto.applicationContext))
                .addMigrations(MIGRACION_1_2, MIGRACION_2_3, MIGRACION_3_4, MigracionUsuarios4a5)
                .build()

        /**
         * Llena la base la primera y unica vez que se crea el archivo.
         *
         * Se insertan las filas con SQL directo, dentro de la misma transaccion
         * de creacion, en lugar de lanzar una corrutina que use los DAO. Es
         * mas verboso pero elimina una condicion de carrera real: con la
         * corrutina, la primera consulta de la interfaz puede ejecutarse antes
         * de que el sembrado termine y mostrar un catalogo vacio.
         */
        /**
         * Version 3 -> 4: el catalogo guarda las medidas de cada pieza.
         *
         * Asi, en cuanto se identifica una pieza, la app puede decir cuanto
         * mide sin medirla sobre la foto: el dato del fabricante es exacto y
         * la medicion optica nunca lo es.
         */
        val MIGRACION_3_4 = object : Migration(3, 4) {
            override fun migrate(connection: androidx.sqlite.SQLiteConnection) {
                connection.execSQL("ALTER TABLE `materiales` ADD COLUMN `medidas` TEXT")
                connection.execSQL("ALTER TABLE `materiales` ADD COLUMN `medidaClave` TEXT")

                // Anadir la columna no basta: ALTER TABLE la deja vacia en las
                // filas que ya existian, y la siembra solo corre al crear la
                // base desde cero. Sin este relleno, quien ya tuviera la app
                // instalada veria el catalogo entero sin medidas.
                CatalogoInicial.materiales.forEach { m ->
                    val consulta = connection.prepare(
                        "UPDATE materiales SET medidas = ?, medidaClave = ? WHERE id = ?"
                    )
                    consulta.use { sentencia ->
                        if (m.medidas == null) sentencia.bindNull(1)
                        else sentencia.bindText(1, m.medidas)
                        if (m.medidaClave == null) sentencia.bindNull(2)
                        else sentencia.bindText(2, m.medidaClave)
                        sentencia.bindText(3, m.id)
                        sentencia.step()
                    }
                }
            }
        }

        private class Sembrado(private val contexto: Context) : Callback() {

            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                IntegridadUsuarios.sentencias.forEach(db::execSQL)
                val nuevas = insertarMateriales(db)
                insertarUsuarios(db)
                insertarHuellas(db, nuevas)
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                // Incorpora el kit tambien al actualizar una instalacion con
                // datos. Nunca reemplaza filas, reactiva bajas ni duplica vistas.
                // Materiales y huellas se incorporan juntos o se revierten juntos.
                db.beginTransaction()
                try {
                    insertarHuellas(db, insertarMateriales(db))
                    db.setTransactionSuccessful()
                } finally {
                    db.endTransaction()
                }
            }

            /**
             * Carga las huellas visuales precalculadas, si las hay.
             *
             * El archivo lo genera `herramientas/generar_huellas.py` a partir
             * de una carpeta de fotografias. Es opcional a proposito: sin el,
             * la app arranca igual y las piezas se van ensenando una a una
             * desde la ficha de cada material.
             */
            private fun insertarHuellas(db: SupportSQLiteDatabase, nuevas: Set<String>) {
                if (nuevas.isEmpty()) return
                val contenido = try {
                    contexto.assets.open(ARCHIVO_HUELLAS)
                        .bufferedReader()
                        .use { it.readText() }
                } catch (e: FileNotFoundException) {
                    return  // No hay huellas precalculadas: es un caso normal.
                }

                val sql = """
                    INSERT INTO embeddings (materialId, vector, modelo, fechaHora)
                    VALUES (?,?,?,?)
                """.trimIndent()

                val ahora = System.currentTimeMillis()
                val lista = JSONArray(contenido)

                for (i in 0 until lista.length()) {
                    val fila = lista.getJSONObject(i)
                    // Un ID ocupado puede pertenecer a otra pieza del usuario.
                    // Sus referencias nunca deben mezclarse con las del kit.
                    if (fila.getString("materialId") !in nuevas) continue
                    val valores = fila.getJSONArray("vector")
                    val vector = FloatArray(valores.length()) { j ->
                        valores.getDouble(j).toFloat()
                    }
                    db.execSQL(
                        sql,
                        arrayOf<Any?>(
                            fila.getString("materialId"),
                            EmbeddingMaterial.aBytes(vector),
                            fila.optString("modelo", EmbeddingMaterial.MODELO_ACTUAL),
                            ahora
                        )
                    )
                }
            }

            private fun insertarMateriales(db: SupportSQLiteDatabase): Set<String> {
                val ids = mutableSetOf<String>()
                val numeros = mutableSetOf<String>()
                db.query("SELECT id, numeroParte FROM materiales").use { cursor ->
                    while (cursor.moveToNext()) {
                        ids += cursor.getString(0)
                        numeros += Material.normalizarNumeroParte(cursor.getString(1))
                    }
                }
                val nuevas = mutableSetOf<String>()
                val sql = """
                    INSERT INTO materiales
                    (id, nombre, descripcion, numeroParte, fabricante, categoria,
                     unidadMedida, existencia, existenciaMinima,
                     almacen, pasillo, rack, nivel, posicion,
                     medidas, medidaClave, fotoReferencia, activo)
                    VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                """.trimIndent()

                CatalogoInicial.materiales.forEach { m ->
                    val numero = Material.normalizarNumeroParte(m.numeroParte)
                    if (m.id in ids || numero in numeros) return@forEach
                    // El tipo se declara explicito: la lista mezcla textos,
                    // numeros y nulos, y sin ayuda Kotlin infiere un tipo
                    // interseccion que no sirve para execSQL.
                    db.execSQL(
                        sql,
                        arrayOf<Any?>(
                            m.id,
                            m.nombre,
                            m.descripcion,
                            m.numeroParte,
                            m.fabricante,
                            m.categoria.name,
                            m.unidadMedida,
                            m.existencia,
                            m.existenciaMinima,
                            m.ubicacion.almacen,
                            m.ubicacion.pasillo,
                            m.ubicacion.rack,
                            m.ubicacion.nivel,
                            m.ubicacion.posicion,
                            m.medidas,
                            m.medidaClave,
                            m.fotoReferencia,
                            if (m.activo) 1 else 0
                        )
                    )
                    ids += m.id
                    numeros += numero
                    nuevas += m.id
                }
                return nuevas
            }

            private fun insertarUsuarios(db: SupportSQLiteDatabase) {
                val sql = """
                    INSERT INTO usuarios
                    (id, usuario, nombre, rol, hashClave, sal, activo)
                    VALUES (?,?,?,?,?,?,?)
                """.trimIndent()

                CatalogoInicial.usuarios().forEach { u ->
                    db.execSQL(
                        sql,
                        arrayOf<Any?>(
                            u.id,
                            u.usuario,
                            u.nombre,
                            u.rol.name,
                            u.hashClave,
                            u.sal,
                            if (u.activo) 1 else 0
                        )
                    )
                }
            }
        }
    }
}
