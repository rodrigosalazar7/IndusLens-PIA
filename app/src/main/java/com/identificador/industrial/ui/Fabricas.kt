package com.identificador.industrial.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.identificador.industrial.AplicacionIdentificador
import com.identificador.industrial.sesion.SesionViewModel
import com.identificador.industrial.ui.pantallas.CatalogoViewModel
import com.identificador.industrial.ui.pantallas.DetalleViewModel
import com.identificador.industrial.ui.pantallas.EdicionViewModel
import com.identificador.industrial.ui.pantallas.HistorialViewModel
import com.identificador.industrial.ui.pantallas.UbicacionViewModel
import com.identificador.industrial.ui.pantallas.EnrolamientoViewModel
import com.identificador.industrial.ui.pantallas.IdentificacionViewModel

/**
 * Fabricas que inyectan los repositorios en cada ViewModel.
 *
 * Sin esto, los ViewModel solo pueden tener constructor vacio y acabarian
 * creando la base de datos por su cuenta, que es justo lo que complica las
 * pruebas y acopla la interfaz al almacenamiento.
 */
object Fabricas {

    val Sesion = viewModelFactory {
        initializer {
            val app = aplicacion()
            SesionViewModel(
                app.repositorioUsuarios,
                app.autenticacionCorreo,
                app.sesionPersistida,
                app.repositorioMateriales
            )
        }
    }

    val Catalogo = viewModelFactory {
        initializer {
            CatalogoViewModel(aplicacion().repositorioMateriales)
        }
    }

    val Detalle = viewModelFactory {
        initializer {
            DetalleViewModel(
                aplicacion().repositorioMateriales,
                aplicacion().repositorioVisual
            )
        }
    }

    // Ojo con el embebedor: se pasa como funcion, no como objeto ya creado.
    // Evaluarlo aqui cargaria el modelo de MobileNet en el hilo principal, al
    // construir el ViewModel durante el arranque, y congelaria la interfaz
    // mas de un segundo. Asi la carga se difiere al primer uso real, que
    // ocurre en un hilo de fondo.
    val Identificacion = viewModelFactory {
        initializer {
            val app = aplicacion()
            IdentificacionViewModel(
                app.repositorioMateriales,
                app.repositorioBusquedas,
                app.repositorioVisual,
                obtenerEmbebedor = { app.embebedor },
                obtenerClasificador = { app.clasificador },
                obtenerReconocedorEnLinea = { app.reconocedorEnLinea }
            )
        }
    }

    val Historial = viewModelFactory {
        initializer {
            val app = aplicacion()
            HistorialViewModel(app.repositorioBusquedas, app.repositorioMateriales)
        }
    }

    val Ubicacion = viewModelFactory {
        initializer { UbicacionViewModel(aplicacion().repositorioMateriales) }
    }

    val Edicion = viewModelFactory {
        initializer {
            val app = aplicacion()
            EdicionViewModel(app.repositorioMateriales, createSavedStateHandle())
        }
    }

    val Enrolamiento = viewModelFactory {
        initializer {
            val app = aplicacion()
            EnrolamientoViewModel(
                app.repositorioMateriales,
                app.repositorioVisual
            ) { app.embebedor }
        }
    }
}

private fun androidx.lifecycle.viewmodel.CreationExtras.aplicacion(): AplicacionIdentificador =
    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as AplicacionIdentificador
