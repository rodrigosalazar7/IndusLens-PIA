package com.identificador.industrial.datos

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.identificador.industrial.datos.modelo.Categoria
import com.identificador.industrial.datos.modelo.Material
import com.identificador.industrial.datos.modelo.Ubicacion
import com.identificador.industrial.sesion.Rol
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Fuente central del catalogo y los roles.
 *
 * Room sigue siendo la cache local para que la camara y la consulta funcionen
 * sin red. Firestore es la fuente compartida: el administrador publica los
 * cambios y los demas telefonos los reciben automaticamente.
 */
class BaseRemota(contexto: Context) {

    private val firestore: FirebaseFirestore? = if (
        FirebaseApp.getApps(contexto.applicationContext).isNotEmpty()
    ) {
        FirebaseFirestore.getInstance()
    } else {
        null
    }

    val configurada: Boolean get() = firestore != null

    val sesionRemotaActiva: Boolean
        get() = firestore != null &&
            FirebaseAuth.getInstance().currentUser?.isEmailVerified == true

    suspend fun obtenerOCrearRol(
        uid: String,
        correo: String,
        nombre: String
    ): Rol {
        val db = firestore ?: return Rol.OPERADOR
        val referencia = db.collection(COLECCION_USUARIOS).document(uid)

        return try {
            val documento = referencia.get().esperarResultado()
            if (documento.exists()) {
                Rol.entries.firstOrNull { it.name == documento.getString("rol") }
                    ?: Rol.OPERADOR
            } else {
                referencia.set(
                    mapOf(
                        "correo" to correo,
                        "nombre" to nombre,
                        "rol" to Rol.OPERADOR.name,
                        "activo" to true
                    )
                ).esperarFin()
                Rol.OPERADOR
            }
        } catch (_: Throwable) {
            // La autenticacion no se pierde si Firestore esta temporalmente
            // fuera de linea. Sin rol remoto nunca se conceden privilegios.
            Rol.OPERADOR
        }
    }

    suspend fun obtenerMateriales(): List<Material> {
        val db = firestore ?: return emptyList()
        return db.collection(COLECCION_MATERIALES)
            .get()
            .esperarResultado()
            .documents
            .mapNotNull { it.aMaterial() }
    }

    fun observarMateriales(): Flow<List<Material>> = callbackFlow {
        val db = firestore
        if (db == null) {
            close()
            return@callbackFlow
        }

        val registro = db.collection(COLECCION_MATERIALES)
            .addSnapshotListener { resultado, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(resultado?.documents.orEmpty().mapNotNull { it.aMaterial() })
            }

        awaitClose { registro.remove() }
    }

    suspend fun guardar(material: Material) {
        val db = firestore ?: return
        db.collection(COLECCION_MATERIALES)
            .document(material.id)
            .set(material.aMapa())
            .esperarFin()
    }

    suspend fun guardarTodos(materiales: List<Material>) {
        val db = firestore ?: return
        materiales.chunked(MAXIMO_LOTE).forEach { lote ->
            val escritura = db.batch()
            lote.forEach { material ->
                escritura.set(
                    db.collection(COLECCION_MATERIALES).document(material.id),
                    material.aMapa()
                )
            }
            escritura.commit().esperarFin()
        }
    }

    suspend fun eliminar(id: String) {
        val db = firestore ?: return
        db.collection(COLECCION_MATERIALES).document(id).delete().esperarFin()
    }

    private fun Material.aMapa(): Map<String, Any?> = mapOf(
        "nombre" to nombre,
        "descripcion" to descripcion,
        "numeroParte" to numeroParte,
        "fabricante" to fabricante,
        "categoria" to categoria.name,
        "unidadMedida" to unidadMedida,
        "existencia" to existencia,
        "existenciaMinima" to existenciaMinima,
        "almacen" to ubicacion.almacen,
        "pasillo" to ubicacion.pasillo,
        "rack" to ubicacion.rack,
        "nivel" to ubicacion.nivel,
        "posicion" to ubicacion.posicion,
        "medidas" to medidas,
        "medidaClave" to medidaClave,
        "fotoReferencia" to fotoReferencia,
        "activo" to activo,
        "actualizadoEn" to System.currentTimeMillis()
    )

    private fun DocumentSnapshot.aMaterial(): Material? {
        val nombre = getString("nombre") ?: return null
        val numeroParte = getString("numeroParte") ?: return null
        return Material(
            id = id,
            nombre = nombre,
            descripcion = getString("descripcion").orEmpty(),
            numeroParte = numeroParte,
            fabricante = getString("fabricante") ?: "Generico",
            categoria = Categoria.desdeNombre(getString("categoria").orEmpty()),
            unidadMedida = getString("unidadMedida") ?: "Pieza",
            existencia = getLong("existencia")?.toInt() ?: 0,
            existenciaMinima = getLong("existenciaMinima")?.toInt() ?: 0,
            ubicacion = Ubicacion(
                almacen = getString("almacen") ?: "A",
                pasillo = getString("pasillo") ?: "01",
                rack = getString("rack") ?: "A",
                nivel = getLong("nivel")?.toInt() ?: 1,
                posicion = getLong("posicion")?.toInt() ?: 1
            ),
            medidas = getString("medidas"),
            medidaClave = getString("medidaClave"),
            fotoReferencia = getString("fotoReferencia"),
            activo = getBoolean("activo") ?: true
        )
    }

    companion object {
        private const val COLECCION_USUARIOS = "usuarios"
        private const val COLECCION_MATERIALES = "materiales"
        private const val MAXIMO_LOTE = 400
    }
}

private suspend fun <T> Task<T>.esperarResultado(): T =
    suspendCancellableCoroutine { continuacion ->
        addOnCompleteListener { tarea ->
            if (tarea.isSuccessful) {
                continuacion.resume(tarea.result)
            } else {
                continuacion.resumeWithException(
                    tarea.exception ?: IllegalStateException("Fallo la operacion remota")
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
                    tarea.exception ?: IllegalStateException("Fallo la operacion remota")
                )
            }
        }
    }
