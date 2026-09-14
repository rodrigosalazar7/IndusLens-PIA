package com.identificador.industrial.sesion

import android.util.Patterns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.identificador.industrial.datos.RepositorioUsuarios
import com.identificador.industrial.datos.modelo.Rol
import com.identificador.industrial.datos.RepositorioMateriales
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

data class Usuario(
    val id: String,
    val usuario: String,
    val nombre: String,
    val rol: Rol,
    /** Las cuentas nuevas por correo viven en Firebase; las demo viven en Room. */
    val remoto: Boolean = false
) {
    /** Solo el administrador puede dar de alta o editar materiales del catalogo. */
    val puedeAdministrar: Boolean get() = rol == Rol.ADMINISTRADOR
}

/**
 * Maneja el inicio de sesion y quien es el usuario activo.
 *
 * Admite dos caminos deliberadamente complementarios:
 * - cuentas locales de demostracion, validadas contra Room y disponibles sin red;
 * - cuentas reales por correo, registradas en Firebase y bloqueadas hasta que
 *   la persona confirme el enlace de verificacion.
 *
 * La sesion activa se conserva al cerrar la app, sin guardar la contrasena.
 */
class SesionViewModel(
    private val repositorio: RepositorioUsuarios,
    private val autenticacionCorreo: AutenticacionCorreo,
    private val sesionPersistida: SesionPersistida,
    private val repositorioMateriales: RepositorioMateriales
) : ViewModel() {

    var usuarioActivo by mutableStateOf<Usuario?>(null)
        private set

    var cargando by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    var aviso by mutableStateOf<String?>(null)
        private set

    var correoPendiente by mutableStateOf<String?>(null)
        private set

    val correoConfigurado: Boolean get() = autenticacionCorreo.configurada

    val haySesion: Boolean get() = usuarioActivo != null

    init {
        val guardado = sesionPersistida.cargar()
        if (guardado?.remoto == true) {
            // Firebase conserva su token. Se valida antes de abrir el menu por
            // si la cuenta fue eliminada o perdio la verificacion.
            cargando = true
            viewModelScope.launch {
                try {
                    val restaurado = autenticacionCorreo.restaurarSesion()
                    if (restaurado == null) {
                        sesionPersistida.borrar()
                    } else {
                        activar(restaurado)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    error = "No se pudo restaurar la sesión. Entra de nuevo."
                    sesionPersistida.borrar()
                } finally {
                    cargando = false
                }
            }
        } else if (guardado != null) {
            // Nunca publicar en Firebase al utilizar una cuenta de demostración.
            repositorioMateriales.desconectarBaseRemota()
            autenticacionCorreo.cerrarSesion()
            ejecutar {
                val vigente = repositorio.restaurarActivo(guardado.id)
                if (vigente == null) sesionPersistida.borrar() else activar(vigente)
            }
        }
    }

    fun iniciarSesion(identificador: String, clave: String) {
        if (cargando) return

        val acceso = identificador.trim()
        if (acceso.isBlank() || clave.isEmpty()) {
            error = "Escribe tu correo o usuario y contrasena"
            return
        }

        limpiarMensajes()
        ejecutar {
            if ('@' in acceso) {
                if (!correoValido(acceso)) {
                    error = "El correo no tiene un formato valido"
                    return@ejecutar
                }
                when (val resultado = autenticacionCorreo.iniciarSesion(acceso, clave)) {
                    is ResultadoCorreo.Autenticado -> activar(resultado.usuario)
                    is ResultadoCorreo.VerificacionPendiente -> {
                        correoPendiente = resultado.correo
                        aviso = "Tu correo aun no esta verificado. Abre el enlace que te " +
                            "enviamos y despues pulsa Ya verifique, entrar."
                    }
                    is ResultadoCorreo.Error -> error = resultado.mensaje
                    is ResultadoCorreo.CorreoEnviado -> aviso =
                        "Enviamos un correo a ${resultado.correo}."
                }
            } else {
                val encontrado = repositorio.autenticar(acceso, clave)
                if (encontrado == null) {
                    error = "Usuario o contrasena incorrectos"
                } else {
                    activar(encontrado)
                }
            }
        }
    }

    fun registrar(nombre: String, correo: String, clave: String, confirmacion: String) {
        if (cargando) return

        val email = correo.trim().lowercase()
        when {
            nombre.isBlank() -> error = "Escribe tu nombre"
            !correoValido(email) -> error = "Escribe un correo valido"
            clave.length < 6 -> error = "La contrasena debe tener al menos 6 caracteres"
            clave != confirmacion -> error = "Las contrasenas no coinciden"
            else -> {
                limpiarMensajes()
                ejecutar {
                    when (val resultado = autenticacionCorreo.registrar(nombre, email, clave)) {
                        is ResultadoCorreo.VerificacionPendiente -> {
                            correoPendiente = resultado.correo
                            aviso = "Cuenta creada. Enviamos un enlace de verificacion a " +
                                "${resultado.correo}; confirma el correo antes de entrar."
                        }
                        is ResultadoCorreo.Error -> error = resultado.mensaje
                        is ResultadoCorreo.Autenticado -> activar(resultado.usuario)
                        is ResultadoCorreo.CorreoEnviado -> aviso =
                            "Enviamos un correo a ${resultado.correo}."
                    }
                }
            }
        }
    }

    fun reenviarVerificacion(correo: String, clave: String) {
        if (cargando) return

        val email = correo.trim().lowercase()
        if (!correoValido(email) || clave.isEmpty()) {
            error = "Escribe el correo y la contrasena de la cuenta"
            return
        }

        limpiarMensajes()
        ejecutar {
            when (val resultado = autenticacionCorreo.reenviarVerificacion(email, clave)) {
                is ResultadoCorreo.CorreoEnviado -> {
                    correoPendiente = email
                    aviso = "Enviamos un nuevo enlace de verificacion a $email."
                }
                is ResultadoCorreo.Autenticado -> activar(resultado.usuario)
                is ResultadoCorreo.Error -> error = resultado.mensaje
                is ResultadoCorreo.VerificacionPendiente -> {
                    correoPendiente = resultado.correo
                    aviso = "Revisa tu bandeja de entrada y correo no deseado."
                }
            }
        }
    }

    fun enviarRestablecimiento(correo: String) {
        if (cargando) return

        val email = correo.trim().lowercase()
        if (!correoValido(email)) {
            error = "Escribe tu correo para restablecer la contrasena"
            return
        }

        limpiarMensajes()
        ejecutar {
            when (val resultado = autenticacionCorreo.enviarRestablecimiento(email)) {
                is ResultadoCorreo.CorreoEnviado -> aviso =
                    "Si existe una cuenta para $email, recibiras instrucciones para cambiar la contrasena."
                is ResultadoCorreo.Error -> error = resultado.mensaje
                else -> Unit
            }
        }
    }

    fun limpiarError() {
        error = null
    }

    fun limpiarMensajes() {
        error = null
        aviso = null
    }

    fun cerrarSesion() {
        repositorioMateriales.desconectarBaseRemota()
        autenticacionCorreo.cerrarSesion()
        sesionPersistida.borrar()
        usuarioActivo = null
        error = null
        aviso = null
        correoPendiente = null
        cargando = false
    }

    private suspend fun activar(usuario: Usuario) {
        if (!usuario.remoto) {
            repositorioMateriales.desconectarBaseRemota()
            autenticacionCorreo.cerrarSesion()
        }
        usuarioActivo = usuario
        correoPendiente = null
        error = null
        aviso = null
        sesionPersistida.guardar(usuario)
        if (usuario.remoto) {
            try {
                repositorioMateriales.conectarBaseRemota(usuario.puedeAdministrar)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // El acceso y la identificacion siguen disponibles con Room.
                // Una escritura administrativa remota si mostrara su error.
            }
        }
    }

    private fun ejecutar(operacion: suspend () -> Unit) {
        cargando = true
        viewModelScope.launch {
            try {
                operacion()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
                error = "Ocurrio un problema inesperado. Intenta de nuevo."
            } finally {
                cargando = false
            }
        }
    }

    private fun correoValido(correo: String): Boolean =
        Patterns.EMAIL_ADDRESS.matcher(correo).matches()
}
