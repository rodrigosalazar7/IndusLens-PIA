package com.identificador.industrial.datos.local

import java.util.Locale

/** Reglas que SQLite tambien aplica a escrituras directas, ademas del modelo Kotlin. */
object IntegridadUsuarios {
    val sentencias: List<String> = listOf("INSERT", "UPDATE").map { operacion ->
        """
            CREATE TRIGGER IF NOT EXISTS usuarios_validar_${operacion.lowercase(Locale.ROOT)}
        BEFORE $operacion ON usuarios
        WHEN length(trim(NEW.id)) = 0
          OR length(trim(NEW.usuario)) = 0
          OR NEW.usuario COLLATE BINARY != lower(trim(NEW.usuario))
          OR length(trim(NEW.nombre)) = 0
          OR length(trim(NEW.hashClave)) = 0
          OR length(trim(NEW.sal)) = 0
          OR NEW.rol NOT IN ('OPERADOR', 'ALMACENISTA', 'ADMINISTRADOR')
          OR NEW.activo NOT IN (0, 1)
        BEGIN
            SELECT RAISE(ABORT, 'Datos de usuario invalidos');
        END
        """.trimIndent()
    }
}
