package com.identificador.industrial.datos.modelo

/**
 * Como se llego a identificar una pieza. Se guarda en el historial porque
 * la confianza no significa lo mismo segun el metodo: una lectura de numero
 * de parte por OCR es practicamente certera, mientras que un parecido visual
 * del 80% sigue siendo una conjetura.
 */
enum class MetodoIdentificacion(val etiqueta: String) {
    OCR("Numero de parte leido"),
    VISUAL("Parecido visual"),
    MANUAL("Busqueda manual");

    companion object {
        fun desdeNombre(nombre: String): MetodoIdentificacion =
            entries.firstOrNull { it.name == nombre } ?: MANUAL
    }
}
