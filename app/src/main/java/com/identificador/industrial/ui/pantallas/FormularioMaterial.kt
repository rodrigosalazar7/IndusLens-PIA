package com.identificador.industrial.ui.pantallas

import com.identificador.industrial.datos.modelo.Categoria
import com.identificador.industrial.datos.modelo.Material
import java.io.Serializable

enum class CampoMaterial { NOMBRE, NUMERO_PARTE, EXISTENCIA, MINIMO, ALMACEN, PASILLO, RACK, NIVEL, POSICION }

/** Texto sin convertir mientras se escribe; también se conserva al recrear el proceso. */
data class FormularioMaterial(
    val id: String? = null,
    val nombre: String = "",
    val descripcion: String = "",
    val numeroParte: String = "",
    val fabricante: String = "",
    val categoria: Categoria = Categoria.TORNILLERIA,
    val unidadMedida: String = "Pieza",
    val existencia: String = "0",
    val existenciaMinima: String = "0",
    val medidas: String = "",
    val medidaClave: String = "",
    val almacen: String = "A",
    val pasillo: String = "01",
    val rack: String = "A",
    val nivel: String = "1",
    val posicion: String = "1"
) : Serializable {
    val esNuevo: Boolean get() = id == null

    fun errores(): Map<CampoMaterial, String> = buildMap {
        if (nombre.isBlank()) put(CampoMaterial.NOMBRE, "Escribe el nombre de la pieza.")
        if (Material.normalizarNumeroParte(numeroParte).isBlank()) {
            put(CampoMaterial.NUMERO_PARTE, "Escribe un código con letras o números.")
        }
        fun numero(campo: CampoMaterial, valor: String, minimo: Int, etiqueta: String) {
            val n = valor.trim().toIntOrNull()
            if (n == null || n < minimo) {
                put(campo, "$etiqueta debe ser un entero entre $minimo y ${Int.MAX_VALUE}.")
            }
        }
        numero(CampoMaterial.EXISTENCIA, existencia, 0, "La existencia")
        numero(CampoMaterial.MINIMO, existenciaMinima, 0, "El mínimo")
        numero(CampoMaterial.NIVEL, nivel, 1, "El nivel")
        numero(CampoMaterial.POSICION, posicion, 1, "La posición")
        if (almacen.isBlank()) put(CampoMaterial.ALMACEN, "Indica el almacén.")
        if (pasillo.isBlank()) put(CampoMaterial.PASILLO, "Indica el pasillo.")
        if (rack.isBlank()) put(CampoMaterial.RACK, "Indica el rack.")
    }

    companion object {
        fun desde(m: Material) = FormularioMaterial(
            id = m.id, nombre = m.nombre, descripcion = m.descripcion,
            numeroParte = m.numeroParte, fabricante = m.fabricante,
            categoria = m.categoria, unidadMedida = m.unidadMedida,
            existencia = m.existencia.toString(), existenciaMinima = m.existenciaMinima.toString(),
            medidas = m.medidas.orEmpty(), medidaClave = m.medidaClave.orEmpty(),
            almacen = m.ubicacion.almacen, pasillo = m.ubicacion.pasillo,
            rack = m.ubicacion.rack, nivel = m.ubicacion.nivel.toString(),
            posicion = m.ubicacion.posicion.toString()
        )
    }
}

/** Usa Long antes de sumar para no desbordar Int al pulsar +10. */
internal fun ajustarExistencia(valor: String, delta: Int): String =
    ((valor.trim().toLongOrNull() ?: 0L).coerceIn(0L, Int.MAX_VALUE.toLong()) + delta)
        .coerceIn(0L, Int.MAX_VALUE.toLong()).toString()
