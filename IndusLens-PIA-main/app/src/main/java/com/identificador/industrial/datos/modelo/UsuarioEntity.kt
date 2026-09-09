package com.identificador.industrial.datos.modelo

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.identificador.industrial.sesion.Rol

/**
 * Usuario que puede entrar a la app.
 *
 * La contrasena NUNCA se guarda en texto plano: se guarda el hash junto con
 * una sal distinta por usuario.
 *
 * Aviso honesto: SHA-256 es un hash rapido, pensado para integridad, no para
 * contrasenas. Un despliegue real deberia usar una funcion lenta y deliberada
 * como bcrypt, scrypt o Argon2, que encarecen los ataques por fuerza bruta.
 * Se usa SHA-256 aqui porque no anade dependencias externas y el proyecto es
 * academico; queda documentado como deuda tecnica consciente.
 */
@Entity(
    tableName = "usuarios",
    indices = [Index(value = ["usuario"], unique = true)]
)
data class UsuarioEntity(
    @PrimaryKey val id: String,
    val usuario: String,
    val nombre: String,
    val rol: Rol,
    val hashClave: String,
    val sal: String,
    val activo: Boolean = true
)
