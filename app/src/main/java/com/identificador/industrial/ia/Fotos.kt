package com.identificador.industrial.ia

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.camera.core.ImageProxy
import java.io.File
import java.io.FileOutputStream

/**
 * Guardado y carga de las fotografias tomadas.
 */
object Fotos {

    /**
     * Convierte la captura de CameraX en un JPEG ya derecho.
     *
     * CameraX no rota los pixeles: entrega la imagen tal como la vio el sensor
     * y aparte informa cuantos grados hay que girarla. Aqui se aplica el giro
     * de verdad antes de guardar, para que el archivo quede correcto y nadie
     * mas rio abajo tenga que preocuparse de la orientacion.
     */
    fun guardarCaptura(contexto: Context, imagen: ImageProxy): Uri {
        val buffer = imagen.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)

        val original = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        val grados = imagen.imageInfo.rotationDegrees
        val derecha = if (grados == 0) original else rotar(original, grados.toFloat())

        val uri = guardarBitmap(contexto, derecha)

        if (derecha !== original) original.recycle()
        return uri
    }

    /**
     * Guarda un fotograma que ya esta en memoria.
     *
     * Se usa principalmente con PreviewView en el emulador: algunas webcams
     * virtuales muestran video correctamente pero no completan la captura
     * JPEG de CameraX. Guardar el fotograma visible permite probar el mismo
     * OCR y los mismos modelos sin mantener un segundo flujo de analisis.
     */
    fun guardarBitmap(contexto: Context, mapa: Bitmap): Uri {
        val archivo = File(contexto.cacheDir, "captura_${System.currentTimeMillis()}.jpg")
        FileOutputStream(archivo).use { salida ->
            mapa.compress(Bitmap.CompressFormat.JPEG, 90, salida)
        }
        return Uri.fromFile(archivo)
    }

    /**
     * Carga una imagen reducida para mostrarla en pantalla.
     *
     * Se reduce al vuelo porque una foto de camara son varios megapixeles y
     * cargarla entera solo para verla en una tarjeta pequena es la via rapida
     * a un OutOfMemoryError.
     *
     * Tambien se respeta la orientacion EXIF, necesaria para las imagenes
     * elegidas de la galeria. Las capturas propias ya se guardan derechas y
     * sin EXIF, asi que no se giran dos veces.
     */
    fun cargarReducida(contexto: Context, uri: Uri, anchoMaximo: Int = 1024): Bitmap? {
        // Primera pasada: solo se miden las dimensiones, sin cargar pixeles.
        //
        // Cuidado con el valor de retorno: con inJustDecodeBounds activo,
        // decodeStream devuelve null SIEMPRE, por diseno; lo unico que hace es
        // rellenar outWidth y outHeight. Por eso aqui se comprueba que exista
        // el flujo y que las medidas sean validas, y nunca el resultado del
        // decode: hacerlo abortaria la carga en todos los casos.
        val opciones = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val flujoMedida = contexto.contentResolver.openInputStream(uri) ?: return null
        flujoMedida.use { BitmapFactory.decodeStream(it, null, opciones) }
        if (opciones.outWidth <= 0 || opciones.outHeight <= 0) return null

        var muestreo = 1
        while (opciones.outWidth / muestreo > anchoMaximo) muestreo *= 2

        // Segunda pasada: ahora si se cargan los pixeles, ya reducidos.
        val flujoCarga = contexto.contentResolver.openInputStream(uri) ?: return null
        val mapa = flujoCarga.use { entrada ->
            BitmapFactory.decodeStream(
                entrada, null,
                BitmapFactory.Options().apply { inSampleSize = muestreo }
            )
        } ?: return null

        val grados = orientacionExif(contexto, uri)
        return if (grados == 0f) mapa else rotar(mapa, grados)
    }

    /**
     * Carga la foto de referencia de un material del catalogo, guardada en
     * `assets/fotos_catalogo/<archivo>`.
     *
     * Es la misma fotografia con la que se calculo la huella visual de esa
     * pieza (ver `herramientas/generar_huellas.py`). Mostrarla junto al
     * resultado le permite a la persona comparar a simple vista si la pieza
     * que tiene enfrente es la misma que identifico la app, en vez de confiar
     * a ciegas en un numero de parecido.
     *
     * Devuelve null si el material no trae foto de referencia (campo
     * `fotoReferencia` vacio) o si el archivo no existe entre los assets;
     * cualquiera de los dos casos es normal y no debe verse como un error.
     */
    fun cargarDeAssets(
        contexto: Context,
        nombreArchivo: String,
        anchoMaximo: Int = 512
    ): Bitmap? = try {
        contexto.assets.open("fotos_catalogo/$nombreArchivo").use { flujo ->
            val opciones = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            // Los assets no son un Uri: no se pueden abrir dos flujos y medir
            // aparte como en cargarReducida, asi que se decodifica de una vez
            // y solo se reduce si hiciera falta.
            val bytes = flujo.readBytes()
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opciones)
            var muestreo = 1
            while (opciones.outWidth / muestreo > anchoMaximo) muestreo *= 2
            BitmapFactory.decodeByteArray(
                bytes, 0, bytes.size,
                BitmapFactory.Options().apply { inSampleSize = muestreo }
            )
        }
    } catch (e: java.io.IOException) {
        null
    }

    private fun orientacionExif(contexto: Context, uri: Uri): Float =
        try {
            contexto.contentResolver.openInputStream(uri)?.use { entrada ->
                when (ExifInterface(entrada).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f
        } catch (e: Exception) {
            // Una imagen sin EXIF legible no es un error: se asume derecha.
            0f
        }

    /**
     * Carga la foto recortada a la zona del marco guia.
     *
     * Esto importa mas de lo que parece. Cuando alguien encuadra un tornillo
     * en el marco naranja, el resto de la fotografia sigue siendo la nave, la
     * mesa o la pared, y ocupa la mayor parte de los pixeles. Si se manda la
     * imagen completa al modelo, este describe el escenario y no la pieza:
     * responde "habitacion" o "silla" en vez de "tornillo".
     *
     * Recortando al centro se analiza lo que la persona quiso ensenar.
     */
    fun cargarParaAnalisis(
        contexto: Context,
        uri: Uri,
        anchoMaximo: Int = 512
    ): Bitmap? {
        val completa = cargarReducida(contexto, uri, anchoMaximo) ?: return null
        return recortarCentro(completa, PROPORCION_MARCO)
    }

    /**
     * Recorta un cuadrado centrado cuyo lado es una fraccion del lado menor.
     * La proporcion aproxima el tamano del marco guia sobre la vista previa.
     */
    private fun recortarCentro(mapa: Bitmap, proporcion: Float): Bitmap {
        val lado = (minOf(mapa.width, mapa.height) * proporcion).toInt().coerceAtLeast(1)
        val x = (mapa.width - lado) / 2
        val y = (mapa.height - lado) / 2
        if (lado >= mapa.width && lado >= mapa.height) return mapa
        val recorte = Bitmap.createBitmap(mapa, x, y, lado, lado)
        if (recorte !== mapa) mapa.recycle()
        return recorte
    }

    private const val PROPORCION_MARCO = 0.7f

    private fun rotar(mapa: Bitmap, grados: Float): Bitmap {
        val matriz = Matrix().apply { postRotate(grados) }
        return Bitmap.createBitmap(mapa, 0, 0, mapa.width, mapa.height, matriz, true)
    }
}
