package com.identificador.industrial.datos

import com.identificador.industrial.datos.local.Convertidores
import com.identificador.industrial.datos.modelo.NombreUsuario
import com.identificador.industrial.datos.modelo.Rol
import com.identificador.industrial.datos.modelo.UsuarioEntity
import java.util.Locale
import org.junit.Assert.*
import org.junit.Test

class UsuarioTest {
    private fun cuenta(usuario: String = "admin") = UsuarioEntity.crear(
        "u1", usuario, " Administrador ", Rol.ADMINISTRADOR, "hash-original", "sal-original"
    )

    @Test fun normalizaMayusculasYEspacios() {
        assertEquals("admin", cuenta(" \tAdMiN\n ").usuario)
        assertEquals("Administrador", cuenta().nombre)
    }

    @Test fun normalizacionNoDependeDelIdiomaDelTelefono() {
        val anterior = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"))
            assertEquals("industrial", NombreUsuario.normalizar(" INDUSTRIAL "))
        } finally { Locale.setDefault(anterior) }
    }

    @Test fun normalizarDosVecesNoCambiaElResultado() {
        val valor = NombreUsuario.normalizar(" ADMIN ")
        assertEquals(valor, NombreUsuario.normalizar(valor))
    }

    @Test fun rechazaUsuarioVacioOConSoloEspacios() {
        listOf("", " ", "\t\n").forEach { valor ->
            assertThrows(IllegalArgumentException::class.java) { cuenta(valor) }
        }
    }

    @Test fun constructorYCopyImpidenEvitarLaNormalizacion() {
        assertThrows(IllegalArgumentException::class.java) { cuenta().copy(usuario = "ADMIN") }
        assertThrows(IllegalArgumentException::class.java) { cuenta().copy(usuario = " admin ") }
    }

    @Test fun rechazaCamposObligatoriosVacios() {
        assertThrows(IllegalArgumentException::class.java) { cuenta().copy(id = " ") }
        assertThrows(IllegalArgumentException::class.java) { cuenta().copy(nombre = " ") }
        assertThrows(IllegalArgumentException::class.java) { cuenta().copy(hashClave = " ") }
        assertThrows(IllegalArgumentException::class.java) { cuenta().copy(sal = " ") }
    }

    @Test fun conservaCredencialesEstadoYRol() {
        val u = cuenta("ADMIN").copy(activo = false)
        assertEquals("u1", u.id)
        assertEquals("hash-original", u.hashClave)
        assertEquals("sal-original", u.sal)
        assertEquals(Rol.ADMINISTRADOR, u.rol)
        assertFalse(u.activo)
    }

    @Test fun convertidorSoloAceptaRolesValidos() {
        val c = Convertidores()
        Rol.entries.forEach { assertEquals(it, c.textoARol(c.rolATexto(it))) }
        assertThrows(IllegalArgumentException::class.java) { c.textoARol("INVITADO") }
    }
}
