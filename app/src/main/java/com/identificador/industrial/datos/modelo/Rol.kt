package com.identificador.industrial.datos.modelo

/** Roles del dominio, compartidos por persistencia, sesion e interfaz. */
enum class Rol(val etiqueta: String) {
    OPERADOR("Operador"),
    ALMACENISTA("Almacenista"),
    ADMINISTRADOR("Administrador")
}
