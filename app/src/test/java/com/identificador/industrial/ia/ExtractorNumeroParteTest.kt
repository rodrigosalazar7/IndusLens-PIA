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

    @Test
    fun reconstruyeNumeroDeParteSeparadoEnTresTrozos() {
        val candidatos = ExtractorNumeroParte.candidatos(
            """
            SKF
            6205 2 RS1
            Made in France
            """.trimIndent()
        )

        assertTrue("Debe unir los tres trozos", "62052RS1" in candidatos)
    }

    @Test
    fun ofreceLecturaAlternativaParaConfusionCeroOChe() {
        // El OCR leyo una "O" donde el grabado tiene un "0".
        val candidatos = ExtractorNumeroParte.candidatos("62O5-2RS1")

        assertTrue(
            "Debe ofrecer 62052RS1 como lectura alternativa de 62O52RS1",
            "62052RS1" in candidatos
        )
    }
}
