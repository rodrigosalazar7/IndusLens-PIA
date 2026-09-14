package com.identificador.industrial.ia

import android.content.Context
import android.graphics.Bitmap
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import com.identificador.industrial.datos.modelo.Categoria

/**
 * Reconocimiento general mediante Firebase AI Logic.
 *
 * Se usa como complemento del clasificador local, no como reemplazo. Si no
 * hay Internet, Firebase no esta habilitado o la cuota se agota, el flujo
 * continua con EfficientNet dentro del telefono.
 */
class ReconocedorEnLinea(contexto: Context) {

    private val configurado = FirebaseApp.getApps(contexto.applicationContext).isNotEmpty()

    private val motor by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI())
            .generativeModel(MODELO)
    }

    suspend fun analizar(imagen: Bitmap): Pista? {
        if (!configurado) return null

        val respuesta = motor.generateContent(
            content {
                image(imagen)
                text(INSTRUCCION)
            }
        )
        return interpretar(respuesta.text)
    }

    companion object {
        private const val MODELO = "gemini-3.7-flash"

        private const val INSTRUCCION = """
            Identifica el objeto principal centrado en la fotografia.
            Responde en espanol con una sola linea y exactamente este formato:
            NOMBRE|FAMILIA
            NOMBRE debe ser un sustantivo corto y concreto, por ejemplo mango,
            martillo, tornillo o taladro. FAMILIA debe ser exactamente una de:
            Herramienta, Tornilleria, Transmision, Rodamiento, Sello, Electrico,
            Filtro, Neumatica, Lubricante u Otra. No inventes marca, medidas,
            modelo ni numero de parte. No uses Markdown ni agregues explicaciones.
        """

        fun interpretar(texto: String?): Pista? {
            val limpia = texto.orEmpty()
                .lineSequence()
                .firstOrNull { it.isNotBlank() }
                ?.trim()
                ?.removePrefix("`")
                ?.removeSuffix("`")
                ?: return null
            val partes = limpia.split('|', limit = 2)
            val nombre = partes.firstOrNull()
                ?.trim()
                ?.trim('.', ':', '-', '*', '#')
                ?.takeIf { it.length in 2..60 }
                ?: return null
            val categoria = partes.getOrNull(1)?.let(::categoriaDesdeRespuesta)
            return Pista(
                etiqueta = nombre.lowercase(),
                confianza = 0f,
                categoria = categoria,
                origen = OrigenPista.FIREBASE_AI
            )
        }

        private fun categoriaDesdeRespuesta(valor: String): Categoria? = when (
            valor.lowercase().trim().trim('.', ':', '-', '*', '#')
        ) {
            "herramienta" -> Categoria.HERRAMIENTA
            "tornilleria", "tornillería" -> Categoria.TORNILLERIA
            "transmision", "transmisión" -> Categoria.TRANSMISION
            "rodamiento" -> Categoria.RODAMIENTO
            "sello" -> Categoria.SELLO
            "electrico", "eléctrico" -> Categoria.ELECTRICO
            "filtro" -> Categoria.FILTRO
            "neumatica", "neumática" -> Categoria.NEUMATICA
            "lubricante" -> Categoria.LUBRICANTE
            else -> null
        }
    }
}
