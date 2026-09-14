package com.identificador.industrial.ia

import org.junit.Assert.assertTrue
import org.junit.Test

class ExtractorNumeroParteTest {

    @Test
    fun reconstruyeNumeroDeParteSeparadoPorElOcr() {
        val candidatos = ExtractorNumeroParte.candidatos(
            """
            SKF
            6205 2RS1
            Made in France
            """.trimIndent()
        )

        assertTrue("Debe reconstruir 6205-2RS1 normalizado", "62052RS1" in candidatos)
    }
}
