package com.identificador.industrial.sesion

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

sealed interface ResultadoCorreo {
    data class Autenticado(val usuario: Usuario) : ResultadoCorreo
    data class VerificacionPendiente(val correo: String) : ResultadoCorreo
    data class CorreoEnviado(val correo: String) : ResultadoCorreo
    data class Error(val mensaje: String) : ResultadoCorreo
}

/**
 * Registro e inicio de sesion por correo mediante Firebase Authentication.
 *
 * La configuracion se considera opcional porque la demostracion debe seguir
 * funcionando sin Internet con las cuentas locales. Firebase queda activo en
 * cuanto existe app/google-services.json; no se guardan claves ni tokens por
 * cuenta propia, de eso se encarga el SDK.
 */
class AutenticacionCorreo(contexto: Context) {

    private val motor: FirebaseAuth? = if (
        FirebaseApp.getApps(contexto.applicationContext).isNotEmpty()
    ) {
        FirebaseAuth.getInstance().apply { setLanguageCode("es") }
    } else {
        null
    }

    val configurada: Boolean get() = motor != null

    suspend fun registrar(nombre: String, correo: String, clave: String): ResultadoCorreo {
        val auth = motor ?: return faltaConfiguracion()

        return try {
            val credencial = auth.createUserWithEmailAndPassword(correo, clave)
                .esperarResultado()
            val usuario = credencial.user
                ?: return ResultadoCorreo.Error("No se pudo crear la cuenta")

            // El nombre es informativo. Si por alguna razon no se actualiza,
            // la cuenta y su verificacion siguen siendo utilizables.
            try {
                usuario.updateProfile(
                    UserProfileChangeRequest.Builder()
                        .setDisplayName(nombre.trim())
                        .build()
                ).esperarFin()
            } catch (_: Throwable) {
                // Se usa la parte anterior a @ como nombre de respaldo.
            }

            try {
                usuario.sendEmailVerification().esperarFin()
                ResultadoCorreo.VerificacionPendiente(correo)
            } catch (e: Throwable) {
                ResultadoCorreo.Error(
                    "La cuenta se creo, pero no se pudo enviar la verificacion. " +
                        "Intenta reenviarla. ${mensajeDe(e)}"
                )
            } finally {
                // Nadie entra a la app hasta confirmar que el correo le pertenece.
                auth.signOut()
            }
        } catch (e: Throwable) {
            auth.signOut()
            ResultadoCorreo.Error(mensajeDe(e, creandoCuenta = true))
        }
    }

    suspend fun iniciarSesion(correo: String, clave: String): ResultadoCorreo {
        val auth = motor ?: return faltaConfiguracion()

        return try {
            val credencial = auth.signInWithEmailAndPassword(correo, clave)
                .esperarResultado()
            val usuarioFirebase = credencial.user
                ?: return ResultadoCorreo.Error("No se pudo iniciar sesion")

            // El enlace puede haberse abierto fuera de la app. reload evita
            // decidir con el valor antiguo que Firebase conserva en memoria.
            usuarioFirebase.reload().esperarFin()
            val actualizado = auth.currentUser
                ?: return ResultadoCorreo.Error("No se pudo actualizar la cuenta")

            if (!actualizado.isEmailVerified) {
                auth.signOut()
                ResultadoCorreo.VerificacionPendiente(correo)
            } else {
                ResultadoCorreo.Autenticado(actualizado.aUsuario())
            }
        } catch (e: Throwable) {
            auth.signOut()
            ResultadoCorreo.Error(mensajeDe(e))
        }
    }

    suspend fun reenviarVerificacion(correo: String, clave: String): ResultadoCorreo {
        val auth = motor ?: return faltaConfiguracion()
        var conservarSesion = false

        return try {
            val usuario = auth.signInWithEmailAndPassword(correo, clave)
                .esperarResultado()
                .user
                ?: return ResultadoCorreo.Error("No se encontro la cuenta")

            usuario.reload().esperarFin()
            val actualizado = auth.currentUser ?: usuario
            if (actualizado.isEmailVerified) {
                conservarSesion = true
                ResultadoCorreo.Autenticado(actualizado.aUsuario())
            } else {
                actualizado.sendEmailVerification().esperarFin()
                ResultadoCorreo.CorreoEnviado(correo)
            }
        } catch (e: Throwable) {
            ResultadoCorreo.Error(mensajeDe(e))
        } finally {
            if (!conservarSesion) auth.signOut()
        }
    }

    suspend fun enviarRestablecimiento(correo: String): ResultadoCorreo {
        val auth = motor ?: return faltaConfiguracion()

        return try {
            // Siempre se muestra el mismo mensaje. Asi la pantalla no revela
            // que direcciones tienen una cuenta registrada.
            auth.sendPasswordResetEmail(correo).esperarFin()
            ResultadoCorreo.CorreoEnviado(correo)
        } catch (e: Throwable) {
            if (e is FirebaseNetworkException) {
                ResultadoCorreo.Error(mensajeDe(e))
            } else {
                ResultadoCorreo.CorreoEnviado(correo)
            }
        }
    }

    suspend fun restaurarSesion(): Usuario? {
        val auth = motor ?: return null
        val actual = auth.currentUser ?: return null

        return try {
            actual.reload().esperarFin()
            auth.currentUser?.takeIf { it.isEmailVerified }?.aUsuario()
        } catch (_: Throwable) {
            null
        }
    }

    fun cerrarSesion() {
        motor?.signOut()
    }

    private fun com.google.firebase.auth.FirebaseUser.aUsuario(): Usuario {
        val correo = email.orEmpty()
        return Usuario(
            id = uid,
            usuario = correo,
            nombre = displayName?.takeIf { it.isNotBlank() }
                ?: correo.substringBefore('@').ifBlank { "Operador" },
            rol = Rol.OPERADOR,
            remoto = true
        )
    }

    private fun faltaConfiguracion() = ResultadoCorreo.Error(
        "El acceso por correo aun no esta conectado. Agrega app/google-services.json " +
            "y habilita Correo/Contrasena en Firebase Authentication."
    )

    private fun mensajeDe(error: Throwable, creandoCuenta: Boolean = false): String {
        if (error is FirebaseNetworkException) {
            return "No hay conexion. Revisa Internet y vuelve a intentarlo."
        }

        val codigo = (error as? FirebaseAuthException)?.errorCode.orEmpty()
        return when (codigo) {
            "ERROR_INVALID_EMAIL" -> "El correo no tiene un formato valido."
            "ERROR_WEAK_PASSWORD" -> "Usa una contrasena de al menos 6 caracteres."
            "ERROR_EMAIL_ALREADY_IN_USE" -> "Ese correo ya tiene una cuenta."
            "ERROR_USER_DISABLED" -> "La cuenta esta deshabilitada."
            "ERROR_TOO_MANY_REQUESTS" ->
                "Hubo demasiados intentos. Espera un momento antes de reintentar."

            "ERROR_OPERATION_NOT_ALLOWED" ->
                "Habilita el proveedor Correo/Contrasena en Firebase Authentication."

            "ERROR_WRONG_PASSWORD", "ERROR_USER_NOT_FOUND", "ERROR_INVALID_CREDENTIAL" ->
                "Correo o contrasena incorrectos."

            else -> if (creandoCuenta) {
                "No se pudo crear la cuenta. Intenta de nuevo."
            } else {
                "No se pudo completar el acceso. Intenta de nuevo."
            }
        }
    }
}

private suspend fun <T> Task<T>.esperarResultado(): T =
    suspendCancellableCoroutine { continuacion ->
        addOnCompleteListener { tarea ->
            if (tarea.isSuccessful) {
                continuacion.resume(tarea.result)
            } else {
                continuacion.resumeWithException(
                    tarea.exception ?: IllegalStateException("La operacion no termino correctamente")
                )
            }
        }
    }

private suspend fun Task<Void>.esperarFin(): Unit =
    suspendCancellableCoroutine { continuacion ->
        addOnCompleteListener { tarea ->
            if (tarea.isSuccessful) {
                continuacion.resume(Unit)
            } else {
                continuacion.resumeWithException(
                    tarea.exception ?: IllegalStateException("La operacion no termino correctamente")
                )
            }
        }
    }
