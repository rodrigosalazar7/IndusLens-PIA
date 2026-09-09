package com.identificador.industrial.datos.local

import androidx.room.TypeConverter
import com.identificador.industrial.datos.modelo.Categoria
import com.identificador.industrial.datos.modelo.MetodoIdentificacion
import com.identificador.industrial.datos.modelo.Rol

/**
 * SQLite no conoce los enums de Kotlin, asi que se guardan como texto.
 *
 * Se guarda `name` y no `ordinal` a proposito: si manana alguien reordena
 * las constantes del enum, los datos ya guardados seguirian siendo correctos.
 * Con el ordinal, un rodamiento se convertiria en un tornillo en silencio.
 */
class Convertidores {

    @TypeConverter
    fun categoriaATexto(valor: Categoria): String = valor.name

    @TypeConverter
    fun textoACategoria(valor: String): Categoria = Categoria.desdeNombre(valor)

    @TypeConverter
    fun metodoATexto(valor: MetodoIdentificacion): String = valor.name

    @TypeConverter
    fun textoAMetodo(valor: String): MetodoIdentificacion =
        MetodoIdentificacion.desdeNombre(valor)

    @TypeConverter
    fun rolATexto(valor: Rol): String = valor.name

    @TypeConverter
    fun textoARol(valor: String): Rol =
        Rol.valueOf(valor)
}
