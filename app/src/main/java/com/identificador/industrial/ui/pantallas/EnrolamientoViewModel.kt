package com.identificador.industrial.ui.pantallas

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.identificador.industrial.datos.RepositorioMateriales
import com.identificador.industrial.datos.RepositorioVisual
import com.identificador.industrial.ia.Embebedor
import com.identificador.industrial.ia.Fotos
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface EstadoEnrolamiento {
    data object Inactivo : EstadoEnrolamiento
    data object Procesando : EstadoEnrolamiento
    data class Listo(val nombreMaterial: String, val vistas: Int) : EstadoEnrolamiento
    data class Error(val mensaje: String) : EstadoEnrolamiento
}

/**
 * Ensena una pieza a la aplicacion.
 *
 * Se fotografia el material una vez, se calcula su huella visual y se guarda
 * asociada a su ficha. A partir de ese momento la pieza entra en la busqueda
 * por parecido: si alguien fotografia otra igual sin marcas legibles, aparecera
 * entre las candidatas.
 *
 * Esto es lo que hace que el catalogo crezca sin reentrenar nada. Una foto por
 * pieza, y ya esta.
 */
class EnrolamientoViewModel(
    private val repositorioMateriales: RepositorioMateriales,
    private val repositorioVisual: RepositorioVisual,
    /** Se difiere la carga del modelo al primer uso, y fuera del hilo principal. */
    private val obtenerEmbebedor: () -> Embebedor
) : ViewModel() {

    var estado by mutableStateOf<EstadoEnrolamiento>(EstadoEnrolamiento.Inactivo)
        private set

    fun enrolar(contexto: Context, materialId: String, uri: Uri) {
        if (estado is EstadoEnrolamiento.Procesando) return

        viewModelScope.launch {
            estado = EstadoEnrolamiento.Procesando
            try {
                val material = repositorioMateriales.obtenerPorId(materialId)
                if (material == null) {
                    estado = EstadoEnrolamiento.Error("No existe el material $materialId")
                    return@launch
                }

                val vector = withContext(Dispatchers.Default) {
                    // Mismo recorte que usa la identificacion. Es obligatorio
                    // que coincidan: si se aprende la foto entera y luego se
                    // compara solo el recorte, los vectores viven en encuadres
                    // distintos y la pieza no se reconoce.
                    val imagen = Fotos.cargarParaAnalisis(contexto, uri)
                        ?: error("No se pudo leer la fotografia")
                    obtenerEmbebedor().vector(imagen)
                }

                repositorioVisual.guardarHuella(materialId, vector)
                estado = EstadoEnrolamiento.Listo(
                    nombreMaterial = material.nombre,
                    vistas = repositorioVisual.vistasDe(materialId)
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                // Throwable y no Exception: los fallos nativos de TensorFlow
                // Lite y la falta de memoria son Error, y dejarlos escapar
                // congelaria la pantalla en "Calculando la huella visual".
                Log.e("Enrolamiento", "Fallo al calcular la huella", e)
                estado = EstadoEnrolamiento.Error(
                    e.message ?: "No se pudo calcular la huella visual"
                )
            }
        }
    }

    fun reiniciar() {
        estado = EstadoEnrolamiento.Inactivo
    }
}
