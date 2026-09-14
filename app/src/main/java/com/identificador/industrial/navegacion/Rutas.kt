package com.identificador.industrial.navegacion

/**
 * Rutas de las 10 pantallas del proyecto.
 *
 * Las que reciben datos usan un argumento en la ruta ("detalle/{materialId}")
 * y ofrecen una funcion para construir la ruta ya rellenada.
 */
object Rutas {

    // 1. Inicio de sesion
    const val LOGIN = "login"

    // 2. Menu principal
    const val MENU = "menu"

    // 3. Camara para identificar pieza
    const val CAMARA = "camara"

    // 4. Procesamiento con IA
    const val PROCESANDO = "procesando"

    // 5. Resultado de identificacion
    const val RESULTADO = "resultado"

    // 6. Piezas similares
    const val SIMILARES = "similares"

    // 7. Detalle del material
    const val DETALLE = "detalle/{materialId}"
    fun detalle(materialId: String) = "detalle/$materialId"

    // 8. Localizacion dentro del almacen
    const val UBICACION = "ubicacion/{materialId}"
    fun ubicacion(materialId: String) = "ubicacion/$materialId"

    // 9. Historial de busquedas
    const val HISTORIAL = "historial"

    // 10. Administracion de materiales
    const val ADMIN = "admin"

    // Ensenar una pieza a la app: se fotografia una vez para que aprenda
    // a reconocerla por su forma.
    const val ENROLAR = "enrolar/{materialId}"
    fun enrolar(materialId: String) = "enrolar/$materialId"

    // La persona senala en el catalogo que pieza es, cuando la app no supo.
    const val ELEGIR = "elegir"

    // Medir la pieza sobre la foto usando un objeto de tamano conocido.
    const val MEDIR = "medir"

    // Alta y edicion de materiales. Sin argumento, es un alta.
    //
    // "aprender" distingue el alta que nace de una identificacion fallida: en
    // ese caso, al guardar el material nuevo se le ensena de inmediato la foto
    // que ya se tenia, para no obligar a repetirla.
    const val EDITAR = "editar?materialId={materialId}&aprender={aprender}"
    fun editar(materialId: String? = null, aprenderFoto: Boolean = false) =
        "editar?materialId=${materialId.orEmpty()}&aprender=$aprenderFoto"

    const val ARG_MATERIAL_ID = "materialId"
    const val ARG_APRENDER = "aprender"
}
