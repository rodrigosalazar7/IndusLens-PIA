package com.identificador.industrial.datos.modelo

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

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
    @ColumnInfo(collate = ColumnInfo.NOCASE) val usuario: String,
    val nombre: String,
    val rol: Rol,
    val hashClave: String,
    val sal: String,
    val activo: Boolean = true
) {
    init {
        require(id.isNotBlank()) { "El identificador de usuario es obligatorio" }
        NombreUsuario.validar(usuario)
        require(nombre.isNotBlank()) { "El nombre es obligatorio" }
        require(hashClave.isNotBlank() && sal.isNotBlank()) { "Las credenciales son obligatorias" }
    }

    companion object {
        /** Entrada para nuevas cuentas; la lectura de Room ya contiene datos canonicos. */
        fun crear(
            id: String,
            usuario: String,
            nombre: String,
            rol: Rol,
            hashClave: String,
            sal: String,
            activo: Boolean = true
        ): UsuarioEntity = UsuarioEntity(
            id = id,
            usuario = NombreUsuario.normalizar(usuario),
            nombre = nombre.trim(),
            rol = rol,
            hashClave = hashClave,
            sal = sal,
            activo = activo
        )
    }
}
