package com.identificador.industrial.datos.modelo

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Locale

/**
 * Un material del catalogo: la pieza que el trabajador quiere identificar.
 *
 * El indice sobre `numeroParte` es lo que hace instantanea la busqueda cuando
 * el OCR consigue leer la etiqueta de la pieza (fase 3), que es el camino de
 * identificacion mas fiable.
 */
@Entity(
    tableName = "materiales",
    indices = [
        Index(value = ["numeroParte"], unique = true),
        Index(value = ["categoria"]),
        Index(value = ["nombre"])
    ]
)
data class Material(
    @PrimaryKey val id: String,
    val nombre: String,
    val descripcion: String,
    val numeroParte: String,
    val fabricante: String,
    val categoria: Categoria,
    val unidadMedida: String,
    val existencia: Int,
    val existenciaMinima: Int,
    @Embedded val ubicacion: Ubicacion,
    /**
     * Medidas nominales de la pieza, tal y como las publica el fabricante.
     *
     * Es un dato del catalogo, no una estimacion: un rodamiento 6205 mide
     * 25 x 52 x 15 mm siempre. En cuanto la pieza esta identificada, esto es
     * infinitamente mas fiable que medir sobre una fotografia, que arrastra
     * errores de pulso y de perspectiva.
     */
    val medidas: String? = null,
    /** Medida clave para pedirla en almacen: "M10", "1/2 pulgada", "25 mm". */
    val medidaClave: String? = null,
    /** Nombre del drawable de referencia. En la fase 4 sera la foto del catalogo. */
    val fotoReferencia: String? = null,
    val activo: Boolean = true
) {
    val sinExistencia: Boolean get() = existencia <= 0

    val bajoMinimo: Boolean get() = existencia in 1..existenciaMinima

    /**
     * Version normalizada del numero de parte para comparar contra lo que
     * devuelve el OCR: sin espacios, guiones ni diferencias de mayusculas.
     * "6205-2RS1" y "6205 2rs1" deben considerarse el mismo numero.
     */
    val numeroParteNormalizado: String
        get() = normalizarNumeroParte(numeroParte)

    companion object {
        fun normalizarNumeroParte(valor: String): String =
            valor.uppercase(Locale.ROOT).filter { it.isLetterOrDigit() }
    }
}
