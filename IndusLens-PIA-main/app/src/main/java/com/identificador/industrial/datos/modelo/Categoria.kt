package com.identificador.industrial.datos.modelo

/**
 * Familia a la que pertenece un material. Sirve para filtrar el catalogo y,
 * en la fase 4, para acotar la busqueda visual: comparar un rodamiento solo
 * contra rodamientos reduce mucho los falsos positivos.
 */
enum class Categoria(val etiqueta: String) {
    RODAMIENTO("Rodamientos"),
    TRANSMISION("Transmision"),
    SELLO("Sellos y retenes"),
    TORNILLERIA("Tornilleria"),
    NEUMATICA("Neumatica"),
    ELECTRICO("Material electrico"),
    SENSOR("Sensores"),
    FILTRO("Filtros"),
    HERRAMIENTA("Herramientas"),
    LUBRICANTE("Lubricantes");

    companion object {
        fun desdeNombre(nombre: String): Categoria =
            entries.firstOrNull { it.name == nombre } ?: HERRAMIENTA
    }
}
