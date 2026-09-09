package com.identificador.industrial

import android.app.Application
import com.identificador.industrial.datos.RepositorioBusquedas
import com.identificador.industrial.datos.RepositorioMateriales
import com.identificador.industrial.datos.RepositorioUsuarios
import com.identificador.industrial.datos.RepositorioVisual
import com.identificador.industrial.datos.local.BaseDatos
import com.identificador.industrial.ia.ClasificadorGenerico
import com.identificador.industrial.ia.Embebedor
import com.identificador.industrial.sesion.AutenticacionCorreo
import com.identificador.industrial.sesion.SesionPersistida

/**
 * Contenedor de dependencias de la app.
 *
 * Se resuelve a mano en lugar de con Hilt o Koin: para el tamano de este
 * proyecto una libreria de inyeccion anadiria mas configuracion que valor,
 * y asi se ve con claridad de donde sale cada objeto.
 *
 * Todo se crea con `lazy`, asi que la base de datos no se abre hasta que
 * alguien la necesita de verdad y el arranque de la app no se penaliza.
 */
class AplicacionIdentificador : Application() {

    private val baseDatos by lazy { BaseDatos.obtener(this) }

    val repositorioMateriales by lazy { RepositorioMateriales(baseDatos.materialDao()) }

    val repositorioUsuarios by lazy { RepositorioUsuarios(baseDatos.usuarioDao()) }

    val autenticacionCorreo by lazy { AutenticacionCorreo(this) }

    val sesionPersistida by lazy { SesionPersistida(this) }

    val repositorioBusquedas by lazy { RepositorioBusquedas(baseDatos.busquedaDao()) }

    val repositorioVisual by lazy {
        RepositorioVisual(baseDatos.embeddingDao(), baseDatos.materialDao())
    }

    /**
     * Un unico motor de embeddings para toda la app.
     *
     * Cargar el modelo cuesta memoria y tiempo, y no tiene sentido repetirlo
     * por cada pantalla que lo necesite. Al ser `lazy`, no se carga hasta que
     * alguien pide la primera huella visual.
     */
    val embebedor by lazy { Embebedor(this) }

    /** Modelo generico que dice que tipo de objeto ve. Tambien bajo demanda. */
    val clasificador by lazy { ClasificadorGenerico(this) }
}
