package com.identificador.industrial.datos.local

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.identificador.industrial.datos.modelo.NombreUsuario
import com.identificador.industrial.datos.modelo.Rol
import com.identificador.industrial.datos.modelo.UsuarioEntity

/** Conserva identificadores, claves y referencias del historial al normalizar las cuentas. */
object MigracionUsuarios4a5 : Migration(4, 5) {
    override fun migrate(connection: SQLiteConnection) {
        data class Anterior(val usuarioOriginal: String, val cuenta: UsuarioEntity)

        val anteriores = buildList {
            connection.prepare("SELECT id, usuario, nombre, rol, hashClave, sal, activo FROM usuarios").use { s ->
                while (s.step()) {
                    val original = s.getText(1)
                    require(s.getLong(6) in 0L..1L) { "Estado de usuario invalido" }
                    add(Anterior(original, UsuarioEntity.crear(
                        id = s.getText(0), usuario = original, nombre = s.getText(2),
                        rol = Rol.valueOf(s.getText(3)), hashClave = s.getText(4),
                        sal = s.getText(5), activo = s.getLong(6) == 1L
                    )))
                }
            }
        }

        // Una cuenta que ya usa el nombre canonico lo conserva. Las colisiones
        // reciben -2, -3, etc. sin ocupar nombres de otras cuentas existentes.
        val reservados = anteriores.map { it.cuenta.usuario }.toMutableSet()
        val usados = mutableSetOf<String>()
        val cuentas = anteriores.sortedWith(
            compareBy<Anterior> { it.usuarioOriginal != NombreUsuario.normalizar(it.usuarioOriginal) }
                .thenBy { it.cuenta.id }
        ).map { anterior ->
            val base = anterior.cuenta.usuario
            var nombre = base
            var sufijo = 2
            while (nombre in usados) {
                do { nombre = "$base-${sufijo++}" } while (nombre in reservados)
            }
            usados.add(nombre)
            reservados.add(nombre)
            anterior.cuenta.copy(usuario = nombre)
        }

        connection.execSQL("""
            CREATE TABLE usuarios_v5 (
                id TEXT NOT NULL PRIMARY KEY,
                usuario TEXT COLLATE NOCASE NOT NULL,
                nombre TEXT NOT NULL, rol TEXT NOT NULL,
                hashClave TEXT NOT NULL, sal TEXT NOT NULL, activo INTEGER NOT NULL
            )
        """.trimIndent())
        cuentas.forEach { u ->
            connection.prepare("INSERT INTO usuarios_v5 VALUES (?,?,?,?,?,?,?)").use { s ->
                s.bindText(1, u.id)
                s.bindText(2, u.usuario)
                s.bindText(3, u.nombre)
                s.bindText(4, u.rol.name)
                s.bindText(5, u.hashClave)
                s.bindText(6, u.sal)
                s.bindLong(7, if (u.activo) 1L else 0L)
                s.step()
            }
        }
        // Room ejecuta la migracion en una transaccion: un fallo revierte el cambio.
        connection.execSQL("DROP TABLE usuarios")
        connection.execSQL("ALTER TABLE usuarios_v5 RENAME TO usuarios")
        connection.execSQL("CREATE UNIQUE INDEX index_usuarios_usuario ON usuarios (usuario)")
        IntegridadUsuarios.sentencias.forEach(connection::execSQL)
    }
}
