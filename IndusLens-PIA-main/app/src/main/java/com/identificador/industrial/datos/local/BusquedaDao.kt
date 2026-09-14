package com.identificador.industrial.datos.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.identificador.industrial.datos.modelo.Busqueda
import kotlinx.coroutines.flow.Flow

@Dao
interface BusquedaDao {

    @Query("SELECT * FROM historial_busquedas ORDER BY fechaHora DESC LIMIT :limite")
    fun observarRecientes(limite: Int = 100): Flow<List<Busqueda>>

    @Insert
    suspend fun insertar(busqueda: Busqueda): Long

    @Query("DELETE FROM historial_busquedas")
    suspend fun borrarTodo()
}
