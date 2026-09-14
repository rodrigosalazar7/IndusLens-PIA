package com.identificador.industrial.datos.modelo

/**
 * Una pieza del catalogo que se parece a la fotografiada, con cuanto se parece.
 */
data class Coincidencia(
    val material: Material,
    val similitud: Float
) {
    /** Porcentaje para mostrar al operador. */
    val porcentaje: Int get() = (similitud * 100).toInt().coerceIn(0, 100)
}
