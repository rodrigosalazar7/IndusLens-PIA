package com.identificador.industrial.datos.modelo

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Registro de una identificacion hecha por un trabajador.
 *
 * `materialId` puede quedar nulo a proposito: cuando la IA no reconoce la
 * pieza, ese intento fallido tambien interesa guardarlo, porque revela huecos
 * en el catalogo (piezas que existen en planta pero nadie ha dado de alta).
 */
@Entity(
    tableName = "historial_busquedas",
    indices = [Index(value = ["fechaHora"]), Index(value = ["materialId"])]
)
data class Busqueda(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val materialId: String?,
    val usuarioId: String,
    val fechaHora: Long,
    val metodo: MetodoIdentificacion,
    val confianza: Float,
    /** Texto que leyo el OCR, si lo hubo. Util para depurar identificaciones raras. */
    val textoDetectado: String? = null,
    /** Ruta de la foto tomada, guardada en el almacenamiento privado de la app. */
    val fotoPath: String? = null
)
