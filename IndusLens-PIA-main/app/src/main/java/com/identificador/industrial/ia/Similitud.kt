package com.identificador.industrial.ia

import kotlin.math.sqrt

/**
 * Comparacion entre huellas visuales.
 */
object Similitud {

    /**
     * Similitud coseno: mide el angulo entre dos vectores, no su longitud.
     *
     * Devuelve 1 cuando apuntan en la misma direccion (imagenes que el modelo
     * describe igual) y baja segun se separan. Se prefiere a la distancia
     * euclidiana porque no le afecta que una foto salga mas clara o mas
     * contrastada que otra: eso cambia la magnitud del vector, no su
     * direccion.
     *
     * Los vectores llegan ya normalizados a longitud 1, asi que el divisor
     * vale practicamente 1 y esto es casi un producto escalar. Aun asi se
     * divide, para que la funcion siga siendo correcta si algun dia se
     * comparan vectores sin normalizar.
     */
    fun coseno(a: FloatArray, b: FloatArray): Float {
        require(a.size == b.size) {
            "No se pueden comparar vectores de distinto tamano (${a.size} y ${b.size}). " +
                "Suele significar que se generaron con modelos diferentes."
        }

        var producto = 0.0
        var normaA = 0.0
        var normaB = 0.0

        for (i in a.indices) {
            producto += a[i].toDouble() * b[i].toDouble()
            normaA += a[i].toDouble() * a[i].toDouble()
            normaB += b[i].toDouble() * b[i].toDouble()
        }

        if (normaA == 0.0 || normaB == 0.0) return 0f
        return (producto / (sqrt(normaA) * sqrt(normaB))).toFloat()
    }
}
