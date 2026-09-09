package com.identificador.industrial.datos.modelo

/**
 * Posicion fisica de un material dentro del almacen.
 *
 * Se guarda descompuesta en campos y no como una sola cadena, para poder
 * ordenar y filtrar por pasillo o rack cuando se arme la ruta de picking
 * en la pantalla de localizacion.
 */
data class Ubicacion(
    val almacen: String,
    val pasillo: String,
    val rack: String,
    val nivel: Int,
    val posicion: Int
) {
    /** Codigo legible que se muestra al operador, por ejemplo "A-03-B-2-07". */
    val codigo: String
        get() = "$almacen-$pasillo-$rack-$nivel-${posicion.toString().padStart(2, '0')}"

    /** Indicacion en texto para llegar caminando desde la entrada. */
    val comoLlegar: String
        get() = "Almacen $almacen, pasillo $pasillo, rack $rack, nivel $nivel, posicion $posicion"
}
