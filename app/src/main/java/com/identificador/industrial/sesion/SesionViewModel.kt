package com.identificador.industrial.sesion

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.identificador.industrial.datos.RepositorioUsuarios
import com.identificador.industrial.datos.modelo.Rol
import kotlinx.coroutines.launch

data class Usuario(
    val id: String,
    val usuario: String,
    val nombre: String,
    val rol: Rol
) {
    /** Solo el administrador puede dar de alta o editar materiales del catalogo. */
    val puedeAdministrar: Boolean get() = rol == Rol.ADMINISTRADOR
}

/**
 * Maneja el inicio de sesion y quien es el usuario activo.
 *
 * Las credenciales se validan contra la tabla `usuarios` de la base de datos,
 * comparando el hash de la contrasena. La lista fija en memoria de la fase 1
 * ya no existe.
 *
 * Pendiente para mas adelante: guardar la sesion en DataStore para no tener
 * que volver a entrar en cada arranque.
 */
class SesionViewModel(
    private val repositorio: RepositorioUsuarios
) : ViewModel() {

    var usuarioActivo by mutableStateOf<Usuario?>(null)
        private set

    var cargando by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    val haySesion: Boolean get() = usuarioActivo != null

    fun iniciarSesion(usuario: String, clave: String) {
        if (cargando) return

        if (usuario.isBlank() || clave.isEmpty()) {
            error = "Escribe tu usuario y contrasena"
            return
        }

        error = null
        cargando = true

        viewModelScope.launch {
            // El calculo del hash es deliberadamente costoso, asi que la
            // corrutina tarda lo suficiente para que el indicador de carga
            // tenga sentido por si solo.
            val encontrado = repositorio.autenticar(usuario, clave)

            cargando = false
            if (encontrado == null) {
                error = "Usuario o contrasena incorrectos"
            } else {
                usuarioActivo = encontrado
            }
        }
    }

    fun limpiarError() {
        error = null
    }

    fun cerrarSesion() {
        usuarioActivo = null
        error = null
        cargando = false
    }
}
