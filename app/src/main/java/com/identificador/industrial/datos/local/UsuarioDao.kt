package com.identificador.industrial.datos.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.identificador.industrial.datos.modelo.UsuarioEntity

@Dao
interface UsuarioDao {

    /** La comparacion de usuario no distingue mayusculas: "Admin" y "admin" son el mismo. */
    @Query("SELECT * FROM usuarios WHERE usuario = :usuario COLLATE NOCASE AND activo = 1 LIMIT 1")
    suspend fun buscarPorUsuario(usuario: String): UsuarioEntity?

    @Query("SELECT * FROM usuarios WHERE id = :id AND activo = 1 LIMIT 1")
    suspend fun buscarActivoPorId(id: String): UsuarioEntity?

    @Query("SELECT COUNT(*) FROM usuarios")
    suspend fun contar(): Int

    /** Una colision se rechaza; nunca sustituye la cuenta que ya existe. */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertarTodos(usuarios: List<UsuarioEntity>)
}
