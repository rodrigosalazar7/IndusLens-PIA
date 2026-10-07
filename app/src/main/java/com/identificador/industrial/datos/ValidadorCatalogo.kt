package com.identificador.industrial.datos

import com.identificador.industrial.datos.modelo.Material

/**
 * Revisa el catalogo inicial antes de sembrarlo en la base.
 *
 * La siembra corre una sola vez, al crear la base, y la tabla `materiales`
 * tiene un indice unico sobre `numeroParte`. Si en `CatalogoInicial` se
 * colara un numero de parte repetido, el INSERT fallaria y la app se cerraria
 * en la primera apertura sin que nadie sepa por que. Con esta revision, los
 * materiales defectuosos se descartan con un motivo claro y el resto se carga.
 */
object ValidadorCatalogo {

    /** Formato de las claves del catalogo: M-001, M-027... */
    private val FORMATO_CLAVE = Regex("^M-\\d{3,}$")

    data class Problema(val materialId: String, val motivo: String)

    data class Resultado(
        val validos: List<Material>,
        val problemas: List<Problema>
    ) {
        val sinProblemas: Boolean get() = problemas.isEmpty()
    }

    fun validar(materiales: List<Material>): Resultado {
        val validos = mutableListOf<Material>()
        val problemas = mutableListOf<Problema>()
        val clavesVistas = mutableSetOf<String>()
        val numerosVistos = mutableSetOf<String>()

        for (m in materiales) {
            val motivos = problemasDe(m).toMutableList()

            // Los duplicados se comparan normalizados, igual que el OCR:
            // "6205-2RS1" y "6205 2RS1" son el mismo numero de parte.
            if (m.id in clavesVistas) motivos += "clave repetida"
            if (m.numeroParteNormalizado in numerosVistos) {
                motivos += "numero de parte repetido (${m.numeroParte})"
            }

            if (motivos.isEmpty()) {
                validos += m
                clavesVistas += m.id
                numerosVistos += m.numeroParteNormalizado
            } else {
                motivos.forEach { problemas += Problema(m.id, it) }
            }
        }
        return Resultado(validos, problemas)
    }

    /** Reglas que se revisan sobre cada material por separado. */
    private fun problemasDe(m: Material): List<String> = buildList {
        if (!FORMATO_CLAVE.matches(m.id)) add("la clave no tiene el formato M-000")
        if (m.nombre.isBlank()) add("falta el nombre")
        if (m.numeroParteNormalizado.isEmpty()) add("falta el numero de parte")
        if (m.fabricante.isBlank()) add("falta el fabricante")
        if (m.unidadMedida.isBlank()) add("falta la unidad de medida")
        if (m.existencia < 0) add("la existencia es negativa")
        if (m.existenciaMinima < 0) add("la existencia minima es negativa")

        val u = m.ubicacion
        if (u.almacen.isBlank() || u.pasillo.isBlank() || u.rack.isBlank()) {
            add("la ubicacion esta incompleta")
        }
        if (u.nivel < 1 || u.posicion < 1) add("nivel y posicion empiezan en 1")
    }
}
