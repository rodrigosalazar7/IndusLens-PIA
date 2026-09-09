package com.identificador.industrial.datos.modelo

import java.util.Locale

/** La misma clave canonica se utiliza al registrar, migrar y autenticar. */
object NombreUsuario {
    fun normalizar(valor: String): String = valor.trim().lowercase(Locale.ROOT)

    fun validar(valor: String) {
        require(valor.isNotBlank()) { "El usuario no puede estar vacio" }
        require(valor == normalizar(valor)) { "El usuario debe estar normalizado" }
    }
}
