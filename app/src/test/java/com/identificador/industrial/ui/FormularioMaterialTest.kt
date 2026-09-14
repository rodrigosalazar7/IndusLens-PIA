package com.identificador.industrial.ui

import com.identificador.industrial.ui.pantallas.CampoMaterial
import com.identificador.industrial.ui.pantallas.FormularioMaterial
import com.identificador.industrial.ui.pantallas.ajustarExistencia
import org.junit.Assert.*
import org.junit.Test

class FormularioMaterialTest {
    private val valido = FormularioMaterial(nombre = "Martillo", numeroParte = "KIT-001")

    @Test fun aceptaAltaMinimaYCeroExistencias() {
        assertTrue(valido.errores().isEmpty())
    }
    @Test fun noAceptaNombreEnBlancoOCodigoSoloConSimbolos() {
        val errores = valido.copy(nombre = "  ", numeroParte = "- / .").errores()
        assertTrue(CampoMaterial.NOMBRE in errores)
        assertTrue(CampoMaterial.NUMERO_PARTE in errores)
    }
    @Test fun validaAmbasExistenciasSinConvertirTextoInvalidoACero() {
        listOf("", "-1", "1.5", "no", "2147483648").forEach {
            val errores = valido.copy(existencia = it, existenciaMinima = it).errores()
            assertTrue(it, CampoMaterial.EXISTENCIA in errores)
            assertTrue(it, CampoMaterial.MINIMO in errores)
        }
    }
    @Test fun almacenPasilloYRackSonObligatorios() {
        val errores = valido.copy(almacen = " ", pasillo = "", rack = "").errores()
        assertTrue(errores.keys.containsAll(listOf(CampoMaterial.ALMACEN, CampoMaterial.PASILLO, CampoMaterial.RACK)))
    }
    @Test fun nivelYPosicionEmpiezanEnUno() {
        listOf("0", "-2", "", "2.5").forEach {
            val errores = valido.copy(nivel = it, posicion = it).errores()
            assertTrue(CampoMaterial.NIVEL in errores)
            assertTrue(CampoMaterial.POSICION in errores)
        }
    }
    @Test fun admiteNumerosConEspaciosYMaximoInt() {
        assertTrue(valido.copy(existencia = " 2147483647 ", nivel = " 2 ").errores().isEmpty())
    }
    @Test fun ajustesNoBajanDeCeroNiDesbordanInt() {
        assertEquals("0", ajustarExistencia("3", -10))
        assertEquals("2147483647", ajustarExistencia("2147483647", 10))
        assertEquals("1", ajustarExistencia("", 1))
        assertEquals("9", ajustarExistencia("10", -1))
    }
}
