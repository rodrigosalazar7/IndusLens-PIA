package com.identificador.industrial.ia

/**
 * Saca posibles numeros de parte del texto crudo que devuelve el OCR.
 *
 * El problema real: una etiqueta fotografiada no devuelve un numero limpio,
 * devuelve algo como
 *
 *     SKF
 *     6205-2RS1
 *     Made in France
 *     25x52x15
 *
 * Hay que quedarse con "6205-2RS1" y descartar el resto. Y con frecuencia el
 * OCR ademas parte el numero en dos trozos ("6205" y "2RS1") porque en la
 * pieza estan grabados con una separacion. Por eso tambien se prueban las
 * uniones de palabras contiguas.
 *
 * Todo se normaliza a solo letras y digitos en mayusculas, exactamente igual
 * que hace la consulta a la base de datos. Que ambos lados se normalicen del
 * mismo modo es lo que permite que "6205 2rs1" encuentre a "6205-2RS1".
 */
object ExtractorNumeroParte {

    private const val LARGO_MINIMO = 3
    private const val LARGO_MAXIMO = 30

    /** Palabras frecuentes en etiquetas que nunca son un numero de parte. */
    private val RUIDO = setOf(
        "MADEIN", "MADE", "CHINA", "JAPAN", "FRANCE", "GERMANY", "ITALY", "USA",
        "MEXICO", "LOTE", "LOT", "BATCH", "QTY", "PCS", "MAX", "MIN", "TYPE",
        "MODEL", "MODELO", "SERIE", "SERIAL", "REV", "DATE", "FECHA"
    )

    fun candidatos(textoOcr: String): List<String> {
        if (textoOcr.isBlank()) return emptyList()

        val encontrados = LinkedHashSet<String>()

        textoOcr.lines().forEach { linea ->
            val palabras = linea
                .split(Regex("\\s+"))
                .map { normalizar(it) }
                .filter { it.isNotEmpty() }

            palabras.forEach { palabra ->
                if (esCandidato(palabra)) encontrados += palabra
            }

            // Uniones de palabras contiguas, para los numeros que el OCR partio.
            for (i in 0 until palabras.size - 1) {
                val unido = palabras[i] + palabras[i + 1]
                if (esCandidato(unido)) encontrados += unido
            }
        }

        return encontrados.sortedWith(porEspecificidad)
    }

    /**
     * Se prueban primero los candidatos mas especificos.
     *
     * Un texto que mezcla letras y digitos ("6205-2RS1") tiene muchisimas mas
     * papeletas de ser un numero de parte que uno de solo digitos ("25"), que
     * bien puede ser una medida. A igualdad de tipo, gana el mas largo, porque
     * un numero completo identifica mejor que un fragmento suyo.
     */
    private val porEspecificidad = compareByDescending<String> { mixto(it) }
        .thenByDescending { it.length }

    private fun normalizar(valor: String): String =
        valor.uppercase().filter { it.isLetterOrDigit() }

    private fun mixto(valor: String): Boolean =
        valor.any { it.isLetter() } && valor.any { it.isDigit() }

    private fun esCandidato(valor: String): Boolean {
        if (valor.length !in LARGO_MINIMO..LARGO_MAXIMO) return false
        if (valor in RUIDO) return false
        // Sin ningun digito casi seguro es una palabra, no una referencia.
        if (valor.none { it.isDigit() }) return false
        // Solo digitos y muy corto suele ser una medida o una cantidad.
        if (valor.all { it.isDigit() } && valor.length < 4) return false
        return true
    }
}
