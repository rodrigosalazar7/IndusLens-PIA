package com.identificador.industrial.ia

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Verifica el modelo OCR incluido en el APK con una etiqueta controlada, no con una pieza real. */
class LecturaEtiquetaTest {
    @Test fun modeloLocalLeeElCodigoDeUnaImagen() = runBlocking {
        val contexto = InstrumentationRegistry.getInstrumentation().targetContext
        val imagen = Bitmap.createBitmap(1200, 600, Bitmap.Config.ARGB_8888)
        val lienzo = Canvas(imagen)
        lienzo.drawColor(Color.WHITE)
        val pincel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            typeface = Typeface.MONOSPACE
            textAlign = Paint.Align.CENTER
            textSize = 38f
        }
        lienzo.drawText("INDUSLENS - ETIQUETA DE PRUEBA", 600f, 170f, pincel)
        pincel.textSize = 110f
        lienzo.drawText("DEMO-001", 600f, 350f, pincel)
        val archivo = File(contexto.cacheDir, "demo-etiqueta.png")
        archivo.outputStream().use { imagen.compress(Bitmap.CompressFormat.PNG, 100, it) }
        imagen.recycle()
        val lector = LectorTexto()
        try {
            val leido = withTimeout(30_000) { lector.leer(contexto, Uri.fromFile(archivo)) }
            assertTrue(leido, "DEMO001" in ExtractorNumeroParte.candidatos(leido))
        } finally { lector.cerrar() }
    }
}
