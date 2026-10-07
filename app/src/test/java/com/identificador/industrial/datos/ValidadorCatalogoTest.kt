package com.identificador.industrial.datos

import com.identificador.industrial.datos.modelo.Categoria
import com.identificador.industrial.datos.modelo.Material
import com.identificador.industrial.datos.modelo.Ubicacion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidadorCatalogoTest {

    private fun material(
        id: String = "M-001",
        numeroParte: String = "TOR-001",
        nombre: String = "Tornillo de prueba",
        existencia: Int = 10,
        nivel: Int = 1
    ) = Material(
        id = id,
        nombre = nombre,
        descripcion = "",
        numeroParte = numeroParte,
        fabricante = "Catalogo propio",
        categoria = Categoria.TORNILLERIA,
        unidadMedida = "Pieza",
        existencia = existencia,
        existenciaMinima = 2,
        ubicacion = Ubicacion("A", "01", "A", nivel, 1)
    )

    @Test
    fun elCatalogoInicialDeLaAppEsValido() {
        val resultado = ValidadorCatalogo.validar(CatalogoInicial.materiales)

        assertTrue("Problemas: ${resultado.problemas}", resultado.sinProblemas)
        assertEquals(CatalogoInicial.materiales.size, resultado.validos.size)
    }

    @Test
    fun descartaNumeroDeParteRepetidoAunqueCambieElFormato() {
        // "TOR 001" y "TOR-001" son el mismo numero para el OCR y para el
        // indice unico, asi que el segundo debe descartarse.
        val resultado = ValidadorCatalogo.validar(
            listOf(
                material(id = "M-001", numeroParte = "TOR-001"),
                material(id = "M-002", numeroParte = "TOR 001")
            )
        )

        assertEquals(listOf("M-001"), resultado.validos.map { it.id })
        assertEquals("M-002", resultado.problemas.single().materialId)
    }

    @Test
    fun descartaClaveRepetida() {
        val resultado = ValidadorCatalogo.validar(
            listOf(
                material(id = "M-001", numeroParte = "TOR-001"),
                material(id = "M-001", numeroParte = "TOR-002")
            )
        )

        assertEquals(1, resultado.validos.size)
    }

    @Test
    fun descartaDatosIncompletosOFueraDeRango() {
        val resultado = ValidadorCatalogo.validar(
            listOf(
                material(id = "X-1", numeroParte = "A1"),
                material(id = "M-002", numeroParte = "A2", nombre = " "),
                material(id = "M-003", numeroParte = "A3", existencia = -1),
                material(id = "M-004", numeroParte = "A4", nivel = 0)
            )
        )

        assertTrue(resultado.validos.isEmpty())
        assertEquals(
            setOf("X-1", "M-002", "M-003", "M-004"),
            resultado.problemas.map { it.materialId }.toSet()
        )
    }
}
