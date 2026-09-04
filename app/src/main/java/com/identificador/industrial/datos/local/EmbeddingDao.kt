package com.identificador.industrial.datos.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.identificador.industrial.datos.modelo.EmbeddingMaterial
import kotlinx.coroutines.flow.Flow

@Dao
interface EmbeddingDao {

    @Query("SELECT * FROM embeddings WHERE modelo = :modelo")
    suspend fun todos(modelo: String): List<EmbeddingMaterial>

    @Query("SELECT COUNT(*) FROM embeddings WHERE materialId = :materialId")
    suspend fun contarDeMaterial(materialId: String): Int

    /**
     * Flujo, no consulta puntual: al volver de ensenar la pieza la ficha debe
     * reflejarlo sola, sin que nadie tenga que acordarse de refrescarla.
     */
    @Query("SELECT COUNT(*) FROM embeddings WHERE materialId = :materialId")
    fun observarVistas(materialId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM embeddings WHERE modelo = :modelo")
    suspend fun contar(modelo: String): Int

    @Query("SELECT DISTINCT materialId FROM embeddings WHERE modelo = :modelo")
    suspend fun materialesConHuella(modelo: String): List<String>

    /** Se anade una vista mas; las anteriores se conservan. */
    @Insert
    suspend fun guardar(embedding: EmbeddingMaterial)

    @Query("DELETE FROM embeddings WHERE materialId = :materialId")
    suspend fun borrarTodasDe(materialId: String)
}
