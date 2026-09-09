package com.identificador.industrial.datos.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.identificador.industrial.datos.modelo.Categoria
import com.identificador.industrial.datos.modelo.Material
import kotlinx.coroutines.flow.Flow

@Dao
interface MaterialDao {

    @Query("SELECT * FROM materiales WHERE activo = 1 ORDER BY nombre ASC")
    fun observarTodos(): Flow<List<Material>>

    @Query("SELECT * FROM materiales WHERE id = :id")
    fun observarPorId(id: String): Flow<Material?>

    @Query("SELECT * FROM materiales WHERE id = :id")
    suspend fun obtenerPorId(id: String): Material?

    /**
     * Busqueda por numero de parte tolerante al formato. El OCR puede leer
     * "6205 2RS1" donde el catalogo dice "6205-2RS1", asi que se comparan
     * ambos lados sin guiones, espacios ni puntos.
     */
    @Query(
        """
        SELECT * FROM materiales
        WHERE activo = 1
          AND REPLACE(REPLACE(REPLACE(UPPER(numeroParte), '-', ''), ' ', ''), '.', '') = :numeroNormalizado
        LIMIT 1
        """
    )
    suspend fun buscarPorNumeroParte(numeroNormalizado: String): Material?

    /** Busqueda libre para el buscador de la pantalla de administracion. */
    @Query(
        """
        SELECT * FROM materiales
        WHERE activo = 1
          AND (nombre LIKE '%' || :texto || '%'
            OR numeroParte LIKE '%' || :texto || '%'
            OR fabricante LIKE '%' || :texto || '%'
            OR descripcion LIKE '%' || :texto || '%')
        ORDER BY nombre ASC
        """
    )
    fun buscar(texto: String): Flow<List<Material>>

    @Query("SELECT * FROM materiales WHERE activo = 1 AND categoria = :categoria ORDER BY nombre ASC")
    fun observarPorCategoria(categoria: Categoria): Flow<List<Material>>

    @Query("SELECT COUNT(*) FROM materiales")
    suspend fun contar(): Int

    @Query("SELECT id FROM materiales")
    suspend fun todosLosIds(): List<String>

    /**
     * Pasillos que existen de verdad en un almacen, segun lo que hay guardado.
     * El plano de la pantalla de ubicacion se dibuja con esto, no con una
     * distribucion inventada.
     */
    @Query("SELECT DISTINCT pasillo FROM materiales WHERE almacen = :almacen AND activo = 1 ORDER BY pasillo")
    suspend fun pasillosDe(almacen: String): List<String>

    @Query(
        """
        SELECT DISTINCT rack FROM materiales
        WHERE almacen = :almacen AND pasillo = :pasillo AND activo = 1
        ORDER BY rack
        """
    )
    suspend fun racksDe(almacen: String, pasillo: String): List<String>

    /** Otras piezas guardadas en el mismo rack, utiles al ir a recogerla. */
    @Query(
        """
        SELECT * FROM materiales
        WHERE activo = 1 AND almacen = :almacen AND pasillo = :pasillo AND rack = :rack
          AND id != :excluir
        ORDER BY nivel, posicion
        LIMIT 6
        """
    )
    suspend fun vecinosDeRack(
        almacen: String,
        pasillo: String,
        rack: String,
        excluir: String
    ): List<Material>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(material: Material)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarTodos(materiales: List<Material>)

    @Update
    suspend fun actualizar(material: Material)

    @Delete
    suspend fun eliminar(material: Material)
}
