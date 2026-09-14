package com.identificador.industrial.datos

import com.identificador.industrial.datos.local.UsuarioDao
import com.identificador.industrial.datos.modelo.NombreUsuario
import com.identificador.industrial.sesion.Usuario
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RepositorioUsuarios(private val dao: UsuarioDao) {

    /** Restaura desde Room; no confía en un rol o estado activo guardado en preferencias. */
    suspend fun restaurarActivo(id: String): Usuario? = dao.buscarActivoPorId(id)?.let {
        Usuario(it.id, it.usuario, it.nombre, it.rol)
    }

    /**
     * Devuelve el usuario si las credenciales son correctas, o null si no.
     *
     * A proposito no se distingue entre "el usuario no existe" y "la
     * contrasena es incorrecta": informar de la diferencia permitiria
     * averiguar que cuentas existen probando nombres.
     */
    suspend fun autenticar(usuario: String, clave: String): Usuario? {
        val normalizado = NombreUsuario.normalizar(usuario)
        if (normalizado.isBlank() || clave.isEmpty()) return null
        val registro = dao.buscarPorUsuario(normalizado) ?: return null
        // El hash lleva muchas iteraciones a proposito; calcularlo en el hilo
        // de la interfaz congelaria el indicador de carga durante el acceso.
        val coincide = withContext(Dispatchers.Default) {
            Claves.verificar(clave, registro.sal, registro.hashClave)
        }
        if (!coincide) return null
        return Usuario(
            id = registro.id,
            usuario = registro.usuario,
            nombre = registro.nombre,
            rol = registro.rol
        )
    }
}
