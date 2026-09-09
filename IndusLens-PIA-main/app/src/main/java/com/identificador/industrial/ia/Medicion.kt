package com.identificador.industrial.ia

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Objeto de tamano conocido que sirve de patron para dar escala a la foto.
 *
 * Sin uno de estos no se puede medir: una fotografia no contiene el tamano
 * absoluto de lo que retrata. Un tornillo pequeno de cerca y uno grande de
 * lejos ocupan los mismos pixeles. Con un objeto de medida conocida dentro del
 * encuadre, la escala queda determinada y el resto es una regla de tres.
 */
enum class Referencia(val etiqueta: String, val milimetros: Float) {
    MONEDA_10_MXN("Moneda de 10 pesos", 28.0f),
    MONEDA_5_MXN("Moneda de 5 pesos", 25.5f),
    MONEDA_2_MXN("Moneda de 2 pesos", 23.0f),
    MONEDA_1_MXN("Moneda de 1 peso", 21.0f),
    TARJETA("Tarjeta bancaria (lado largo)", 85.6f),
    MONEDA_1_EUR("Moneda de 1 euro", 23.25f)
}

/** Resultado de medir algo en la fotografia. */
data class Medida(
    val milimetros: Float
) {
    val centimetros: Float get() = milimetros / 10f

    val pulgadas: Float get() = milimetros / 25.4f

    /**
     * Fraccion comercial de pulgada mas cercana.
     *
     * En taller no se pide "0.37 pulgadas", se pide 3/8. Se aproxima a
     * dieciseisavos, que es la division habitual en llaves y brocas.
     */
    val enFraccion: String
        get() {
            val dieciseisavos = (pulgadas * 16f).roundToInt()
            if (dieciseisavos <= 0) return "menos de 1/16\""

            val entero = dieciseisavos / 16
            var numerador = dieciseisavos % 16
            var denominador = 16

            while (numerador > 0 && numerador % 2 == 0) {
                numerador /= 2
                denominador /= 2
            }

            return when {
                numerador == 0 -> "$entero\""
                entero == 0 -> "$numerador/$denominador\""
                else -> "$entero $numerador/$denominador\""
            }
        }

    /** Metrica normalizada mas proxima, util para tornilleria. */
    val metricaProxima: String?
        get() {
            val metricas = listOf(3, 4, 5, 6, 8, 10, 12, 14, 16, 20, 24)
            val cercana = metricas.minByOrNull { abs(it - milimetros) } ?: return null
            // Solo se sugiere si esta razonablemente cerca; si no, callar.
            return if (abs(cercana - milimetros) <= 0.8f) "M$cercana" else null
        }

    fun formateada(): String = when {
        milimetros < 10f -> String.format("%.1f mm", milimetros)
        else -> String.format("%.1f mm  ·  %.1f cm", milimetros, centimetros)
    }
}

object Medicion {

    /**
     * Calcula la medida real de un trazo a partir del patron.
     *
     * @param pixelesReferencia largo en pixeles del objeto patron marcado
     * @param pixelesPieza      largo en pixeles de la pieza marcada
     * @param referencia        que objeto patron se uso
     *
     * Ambos trazos deben venir de la MISMA fotografia y estar aproximadamente
     * en el mismo plano. Si la moneda esta sobre la mesa y la pieza levantada
     * hacia la camara, la escala no vale: lo mas cercano al objetivo se ve mas
     * grande. Por eso la pantalla pide apoyar las dos cosas juntas.
     */
    fun calcular(
        pixelesReferencia: Float,
        pixelesPieza: Float,
        referencia: Referencia
    ): Medida? {
        if (pixelesReferencia <= 1f || pixelesPieza <= 0f) return null
        val milimetrosPorPixel = referencia.milimetros / pixelesReferencia
        return Medida(pixelesPieza * milimetrosPorPixel)
    }
}
