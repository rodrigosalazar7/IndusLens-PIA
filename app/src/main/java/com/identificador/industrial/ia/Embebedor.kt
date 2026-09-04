package com.identificador.industrial.ia

import android.content.Context
import android.graphics.Bitmap
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.imageembedder.ImageEmbedder

/**
 * Convierte una fotografia en un vector que describe como se ve la pieza.
 *
 * Usa MobileNet v3 sobre el runtime de TensorFlow Lite, con el modelo
 * empaquetado en los assets del APK: no hay servidor ni conexion de por medio.
 *
 * Por que un embedding y no un clasificador entrenado:
 *
 * Un clasificador solo reconoce las clases con las que se le entreno, y
 * anadir una pieza nueva al almacen obligaria a reunir cientos de fotos y
 * reentrenar el modelo entero. Con embeddings, el modelo nunca cambia: se
 * limita a describir la imagen, y para dar de alta una pieza basta guardar el
 * vector de UNA sola fotografia suya. Identificar es entonces buscar el vector
 * mas parecido, y el catalogo puede crecer sin tocar nada.
 */
class Embebedor(contexto: Context) {

    private val motor: ImageEmbedder = ImageEmbedder.createFromOptions(
        contexto,
        ImageEmbedder.ImageEmbedderOptions.builder()
            .setBaseOptions(
                BaseOptions.builder()
                    .setModelAssetPath(ARCHIVO_MODELO)
                    .build()
            )
            // Normaliza el vector a longitud 1. Asi la comparacion depende solo
            // de la forma y no de la intensidad de la imagen, y la similitud
            // coseno se reduce a un producto escalar.
            .setL2Normalize(true)
            .setQuantize(false)
            .setRunningMode(RunningMode.IMAGE)
            .build()
    )

    fun vector(imagen: Bitmap): FloatArray {
        val entrada = BitmapImageBuilder(imagen).build()
        val resultado = motor.embed(entrada)
        val huellas = resultado.embeddingResult().embeddings()
        require(huellas.isNotEmpty()) { "El modelo no devolvio ningun vector" }
        return huellas[0].floatEmbedding()
    }

    fun cerrar() {
        motor.close()
    }

    companion object {
        const val ARCHIVO_MODELO = "mobilenet_v3_small.tflite"
    }
}
