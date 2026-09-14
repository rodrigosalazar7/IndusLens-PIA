package com.identificador.industrial.ia

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Lectura de texto en una imagen (OCR) con ML Kit.
 *
 * El modelo va empaquetado dentro del APK, asi que funciona sin conexion y sin
 * los Servicios de Google Play. Para una app de almacen eso no es un detalle:
 * las naves industriales suelen tener mala cobertura y cero wifi.
 */
class LectorTexto {

    private val reconocedor =
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /**
     * Devuelve todo el texto que encuentre en la imagen, o cadena vacia si no
     * hay ninguno. `fromFilePath` ya interpreta la orientacion EXIF, asi que
     * no hace falta rotar nada aqui.
     */
    suspend fun leer(contexto: Context, uri: Uri): String =
        suspendCancellableCoroutine { continuacion ->
            val imagen = InputImage.fromFilePath(contexto, uri)
            reconocedor.process(imagen)
                .addOnSuccessListener { resultado ->
                    continuacion.resume(resultado.text)
                }
                .addOnFailureListener { error ->
                    continuacion.resumeWithException(error)
                }
        }

    fun cerrar() {
        reconocedor.close()
    }
}
