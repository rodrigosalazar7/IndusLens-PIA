package com.identificador.industrial.datos.modelo

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Huella visual de un material: el vector que describe como se ve.
 *
 * Va en su propia tabla y no como columna de `materiales` a proposito. Son unos
 * kilobytes por pieza y el catalogo se consulta constantemente para listar y
 * buscar por texto; cargar ese peso muerto en cada consulta seria tirar
 * memoria y tiempo sin motivo. Aqui solo se lee cuando toca comparar imagenes.
 *
 * No es `data class` porque contiene un ByteArray: en una data class, `equals`
 * compararia la referencia del array y no su contenido, lo que da igualdades
 * falsas muy dificiles de depurar. Como nunca se comparan instancias, es mas
 * honesto no generar esos metodos.
 */
@Entity(
    tableName = "embeddings",
    indices = [Index(value = ["materialId"])]
)
class EmbeddingMaterial(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /**
     * No es clave primaria: una misma pieza tiene VARIAS huellas, una por cada
     * vista fotografiada.
     *
     * Antes se guardaba una sola y cada foto nueva reemplazaba a la anterior.
     * Era fragil: bastaba fotografiar la pieza desde otro angulo, o sobre otro
     * fondo, para que el parecido cayera y no la reconociera. Con varias vistas
     * se compara contra todas y se toma la mejor, que es como funciona de
     * verdad el reconocimiento en un almacen donde nadie coloca la pieza dos
     * veces igual.
     */
    val materialId: String,
    val vector: ByteArray,
    /** Que modelo lo genero. Vectores de modelos distintos no son comparables. */
    val modelo: String,
    val fechaHora: Long
) {
    companion object {

        /**
         * Modelo y encuadre activos.
         *
         * El sufijo no es decorativo: identifica tambien COMO se preparo la
         * imagen. Al empezar a recortar la foto al marco guia, los vectores
         * calculados antes dejaron de ser comparables con los nuevos aunque el
         * modelo fuera el mismo. Cambiando esta cadena, las huellas antiguas
         * quedan ignoradas solas, sin migraciones ni datos corruptos.
         */
        const val MODELO_ACTUAL = "mobilenet_v3_small_recorte70"

        fun deFloats(
            materialId: String,
            vector: FloatArray,
            modelo: String = MODELO_ACTUAL
        ): EmbeddingMaterial = EmbeddingMaterial(
            materialId = materialId,
            vector = aBytes(vector),
            modelo = modelo,
            fechaHora = System.currentTimeMillis()
        )

        /** Cuantas vistas conviene tener por pieza para un reconocimiento solido. */
        const val VISTAS_RECOMENDADAS = 4

        fun aBytes(valores: FloatArray): ByteArray {
            val buffer = ByteBuffer.allocate(valores.size * 4).order(ByteOrder.LITTLE_ENDIAN)
            valores.forEach { buffer.putFloat(it) }
            return buffer.array()
        }

        fun aFloats(bytes: ByteArray): FloatArray {
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            return FloatArray(bytes.size / 4) { buffer.getFloat() }
        }
    }

    fun comoFloats(): FloatArray = aFloats(vector)
}
