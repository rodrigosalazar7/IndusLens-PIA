package com.identificador.industrial.datos

import com.identificador.industrial.datos.modelo.Material
import com.identificador.industrial.datos.modelo.UsuarioEntity
import com.identificador.industrial.sesion.Rol

/**
 * Datos con los que se llena la base la primera vez que se abre la app.
 *
 * El catalogo de materiales se deja vacio a proposito: la demostracion en
 * clase muestra en vivo como se da de alta una pieza que la app no conoce
 * (por ejemplo, fotografiando un martillo), y un catalogo ya lleno le resta
 * sentido a esa parte. Los usuarios de prueba si se siembran, porque hacen
 * falta para entrar a la app.
 */
object CatalogoInicial {

    val materiales: List<Material> = emptyList()

    /**
     * Usuarios iniciales. Las contrasenas se cifran al sembrar, nunca se
     * guardan tal cual. Son las mismas credenciales de prueba de la fase 1.
     */
    fun usuarios(): List<UsuarioEntity> = listOf(
        crearUsuario("u1", "operador", "Juan Ramirez", Rol.OPERADOR, "1234"),
        crearUsuario("u2", "almacen", "Maria Lopez", Rol.ALMACENISTA, "1234"),
        crearUsuario("u3", "admin", "Supervisor", Rol.ADMINISTRADOR, "admin")
    )

    private fun crearUsuario(
        id: String,
        usuario: String,
        nombre: String,
        rol: Rol,
        claveEnClaro: String
    ): UsuarioEntity {
        val sal = Claves.generarSal()
        return UsuarioEntity(
            id = id,
            usuario = usuario,
            nombre = nombre,
            rol = rol,
            hashClave = Claves.hash(claveEnClaro, sal),
            sal = sal
        )
    }
}
