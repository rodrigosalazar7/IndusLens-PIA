package com.identificador.industrial.sesion

import com.identificador.industrial.datos.modelo.Rol

import android.content.Context

/** Guarda solo los datos visibles de la sesion; nunca conserva contrasenas. */
class SesionPersistida(contexto: Context, archivo: String = ARCHIVO) {

    private val preferencias = contexto.applicationContext.getSharedPreferences(
        archivo,
        Context.MODE_PRIVATE
    )

    fun guardar(usuario: Usuario) {
        preferencias.edit()
            .putString(CLAVE_ID, usuario.id)
            .putString(CLAVE_USUARIO, usuario.usuario)
            .putString(CLAVE_NOMBRE, usuario.nombre)
            .putString(CLAVE_ROL, usuario.rol.name)
            .putBoolean(CLAVE_REMOTO, usuario.remoto)
            .apply()
    }

    fun cargar(): Usuario? {
        val id = preferencias.getString(CLAVE_ID, null) ?: return null
        val usuario = preferencias.getString(CLAVE_USUARIO, null) ?: return null
        val nombre = preferencias.getString(CLAVE_NOMBRE, null) ?: return null
        val rol = preferencias.getString(CLAVE_ROL, null)
            ?.let { nombreRol -> Rol.entries.firstOrNull { it.name == nombreRol } }
            ?: Rol.OPERADOR

        return Usuario(
            id = id,
            usuario = usuario,
            nombre = nombre,
            rol = rol,
            remoto = preferencias.getBoolean(CLAVE_REMOTO, false)
        )
    }

    fun borrar() {
        preferencias.edit().clear().apply()
    }

    companion object {
        private const val ARCHIVO = "sesion_induslens"
        private const val CLAVE_ID = "id"
        private const val CLAVE_USUARIO = "usuario"
        private const val CLAVE_NOMBRE = "nombre"
        private const val CLAVE_ROL = "rol"
        private const val CLAVE_REMOTO = "remoto"
    }
}
