package com.identificador.industrial.datos

import com.identificador.industrial.datos.local.EmbeddingDao
import com.identificador.industrial.datos.local.MaterialDao
import com.identificador.industrial.datos.modelo.Coincidencia
import com.identificador.industrial.datos.modelo.EmbeddingMaterial
import com.identificador.industrial.ia.Similitud
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Busqueda de piezas por parecido visual.
 */
class RepositorioVisual(
    private val embeddingDao: EmbeddingDao,
    private val materialDao: MaterialDao
) {

    suspend fun guardarHuella(materialId: String, vector: FloatArray) {
        embeddingDao.guardar(EmbeddingMaterial.deFloats(materialId, vector))
    }

    suspend fun vistasDe(materialId: String): Int =
        embeddingDao.contarDeMaterial(materialId)

    fun observarVistas(materialId: String): Flow<Int> =
        embeddingDao.observarVistas(materialId)

    suspend fun cuantasHuellas(): Int =
        embeddingDao.contar(EmbeddingMaterial.MODELO_ACTUAL)

    suspend fun materialesConHuella(): Set<String> =
        embeddingDao.materialesConHuella(EmbeddingMaterial.MODELO_ACTUAL).toSet()

    suspend fun borrarHuellas(materialId: String) = embeddingDao.borrarTodasDe(materialId)

    /**
     * Devuelve las piezas mas parecidas, de mayor a menor similitud.
     *
     * Se comparan todas las huellas una a una. Con un catalogo de decenas o
     * unos pocos miles de piezas es instantaneo: son unas pocas operaciones por
     * material. Si el almacen creciera a cientos de miles habria que pasar a un
     * indice vectorial aproximado (FAISS, HNSW o pgvector en el servidor);
     * queda anotado, pero meterlo ahora seria complicar sin necesidad.
     *
     * Solo se comparan huellas del modelo activo. Vectores de modelos distintos
     * viven en espacios distintos y compararlos daria numeros sin sentido.
     */
    suspend fun buscarParecidos(vector: FloatArray, limite: Int = 5): List<Coincidencia> {
        val huellas = embeddingDao.todos(EmbeddingMaterial.MODELO_ACTUAL)
        if (huellas.isEmpty()) return emptyList()

        val puntuadas = huellas
            .mapNotNull { huella ->
                val guardado = huella.comoFloats()
                if (guardado.size != vector.size) {
                    // Huella de otra version del modelo: se ignora en vez de
                    // reventar, porque el usuario no puede hacer nada con ese error.
                    null
                } else {
                    huella.materialId to Similitud.coseno(vector, guardado)
                }
            }
            // Una pieza puede tener varias vistas guardadas. Cada material
            // compite con SU MEJOR vista, no con todas: si no, una pieza bien
            // fotografiada desde seis angulos ocuparia ella sola la lista de
            // candidatas y taparia a las demas.
            .groupBy { it.first }
            .map { (materialId, puntuaciones) -> materialId to puntuaciones.maxOf { it.second } }
            .sortedByDescending { it.second }
            .take(limite)

        return puntuadas.mapNotNull { (materialId, similitud) ->
            materialDao.obtenerPorId(materialId)?.let { material ->
                Coincidencia(material, similitud)
            }
        }
    }
}
