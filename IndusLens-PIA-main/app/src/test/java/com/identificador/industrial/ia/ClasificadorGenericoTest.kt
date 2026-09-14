package com.identificador.industrial.ia

import com.identificador.industrial.datos.modelo.Categoria
import org.junit.Assert.assertEquals
import org.junit.Test

class ClasificadorGenericoTest {

    @Test
    fun etiquetaHammerSeMuestraComoMartillo() {
        assertEquals("martillo", ClasificadorGenerico.enCastellano("hammer"))
        assertEquals(Categoria.HERRAMIENTA, ClasificadorGenerico.categoriaDe("hammer"))
    }

    @Test
    fun sinonimoDeImageNetTambienSeTraduce() {
        assertEquals(
            "martillo",
            ClasificadorGenerico.enCastellano("hammer, mallet")
        )
    }
}
