package com.identificador.industrial.navegacion

import android.widget.Toast
import androidx.compose.material3.Text
import com.identificador.industrial.ui.componentes.PantallaBase

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.identificador.industrial.sesion.SesionViewModel
import com.identificador.industrial.ui.Fabricas
import com.identificador.industrial.ui.pantallas.EstadoIdentificacion
import com.identificador.industrial.ui.pantallas.PantallaAdmin
import com.identificador.industrial.ui.pantallas.PantallaCamara
import com.identificador.industrial.ui.pantallas.PantallaDetalle
import com.identificador.industrial.ui.pantallas.PantallaEditarMaterial
import com.identificador.industrial.ui.pantallas.PantallaElegirPieza
import com.identificador.industrial.ui.pantallas.PantallaMedir
import com.identificador.industrial.ui.pantallas.PantallaEnrolar
import com.identificador.industrial.ui.pantallas.IdentificacionViewModel
import com.identificador.industrial.ui.pantallas.PantallaHistorial
import com.identificador.industrial.ui.pantallas.PantallaLogin
import com.identificador.industrial.ui.pantallas.PantallaMenu
import com.identificador.industrial.ui.pantallas.PantallaProcesando
import com.identificador.industrial.ui.pantallas.PantallaResultado
import com.identificador.industrial.ui.pantallas.PantallaSimilares
import com.identificador.industrial.ui.pantallas.PantallaUbicacion

/**
 * Grafo de navegacion con las 10 pantallas del proyecto.
 *
 * El SesionViewModel se crea aqui, a nivel del grafo, para que sobreviva a los
 * cambios de pantalla y todas puedan consultar quien es el usuario activo.
 */
@Composable
fun NavegacionApp() {
    val navController = rememberNavController()
    val contexto = LocalContext.current
    val sesion: SesionViewModel = viewModel(factory = Fabricas.Sesion)

    // Se crea aqui, fuera del NavHost, para que la foto y su resultado
    // sobrevivan al recorrido camara -> analisis -> resultado.
    val identificacion: IdentificacionViewModel = viewModel(factory = Fabricas.Identificacion)

    NavHost(
        navController = navController,
        startDestination = Rutas.LOGIN
    ) {

        // 1. Inicio de sesion
        composable(Rutas.LOGIN) {
            PantallaLogin(
                sesion = sesion,
                onLoginExitoso = {
                    navController.navigate(Rutas.MENU) {
                        // Se saca el login de la pila: el boton atras desde el
                        // menu debe salir de la app, no regresar al login.
                        popUpTo(Rutas.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        // 2. Menu principal
        composable(Rutas.MENU) {
            PantallaMenu(
                usuario = sesion.usuarioActivo,
                onIdentificar = {
                    identificacion.reiniciar()
                    navController.navigate(Rutas.CAMARA)
                },
                onHistorial = { navController.navigate(Rutas.HISTORIAL) },
                onAdministrar = { navController.navigate(Rutas.ADMIN) },
                onCerrarSesion = {
                    sesion.cerrarSesion()
                    navController.volverAlLogin()
                }
            )
        }

        // 3. Camara para identificar pieza
        composable(Rutas.CAMARA) {
            PantallaCamara(
                titulo = "Identificar pieza",
                textoGuia = "Centra el objeto completo dentro del cuadro",
                onVolver = { navController.popBackStack() },
                onFotoTomada = { uri ->
                    identificacion.registrarFoto(uri)
                    navController.navigate(Rutas.PROCESANDO)
                }
            )
        }

        // 4. Procesamiento con IA
        composable(Rutas.PROCESANDO) {
            PantallaProcesando(
                identificacion = identificacion,
                usuarioId = sesion.usuarioActivo?.id.orEmpty(),
                onVolver = { navController.popBackStack() },
                onListo = {
                    navController.navigate(Rutas.RESULTADO) {
                        // No tiene sentido volver a la pantalla de espera con
                        // el boton atras, asi que se descarta de la pila.
                        popUpTo(Rutas.PROCESANDO) { inclusive = true }
                    }
                }
            )
        }

        // 5. Resultado de identificacion
        composable(Rutas.RESULTADO) {
            PantallaResultado(
                identificacion = identificacion,
                onVolver = { navController.popBackStack() },
                onVerDetalle = { id -> navController.navigate(Rutas.detalle(id)) },
                onVerSimilares = { navController.navigate(Rutas.SIMILARES) },
                onRepetirFoto = {
                    identificacion.reiniciar()
                    navController.navigate(Rutas.CAMARA) {
                        popUpTo(Rutas.CAMARA) { inclusive = true }
                    }
                },
                onElegirPieza = { navController.navigate(Rutas.ELEGIR) },
                onEsPiezaNueva = if (sesion.usuarioActivo?.puedeAdministrar == true) ({
                    navController.navigate(Rutas.editar(materialId = null, aprenderFoto = true))
                }) else null,
                onMedir = { navController.navigate(Rutas.MEDIR) }
            )
        }

        // Medicion sobre la foto con un objeto patron
        composable(Rutas.MEDIR) {
            PantallaMedir(
                identificacion = identificacion,
                onVolver = { navController.popBackStack() }
            )
        }

        // La persona senala la pieza; la app la aprende para la proxima vez.
        composable(Rutas.ELEGIR) {
            PantallaElegirPieza(
                identificacion = identificacion,
                usuarioId = sesion.usuarioActivo?.id.orEmpty(),
                onVolver = { navController.popBackStack() },
                onElegida = { navController.popBackStack() }
            )
        }

        // 6. Piezas similares
        composable(Rutas.SIMILARES) {
            PantallaSimilares(
                identificacion = identificacion,
                onVolver = { navController.popBackStack() },
                onVerDetalle = { id -> navController.navigate(Rutas.detalle(id)) },
                onRepetirFoto = {
                    identificacion.reiniciar()
                    navController.navigate(Rutas.CAMARA) {
                        popUpTo(Rutas.CAMARA) { inclusive = true }
                    }
                }
            )
        }

        // 7. Detalle del material
        composable(
            route = Rutas.DETALLE,
            arguments = listOf(
                navArgument(Rutas.ARG_MATERIAL_ID) { type = NavType.StringType }
            )
        ) { entrada ->
            val materialId = entrada.arguments?.getString(Rutas.ARG_MATERIAL_ID).orEmpty()
            PantallaDetalle(
                materialId = materialId,
                onVolver = { navController.popBackStack() },
                onVerUbicacion = { id -> navController.navigate(Rutas.ubicacion(id)) },
                onEnsenarPieza = { id -> navController.navigate(Rutas.enrolar(id)) },
                puedeEditar = sesion.usuarioActivo?.puedeAdministrar == true,
                onEditar = { id -> navController.navigate(Rutas.editar(id)) }
            )
        }

        // Ensenar una pieza para la busqueda visual
        composable(
            route = Rutas.ENROLAR,
            arguments = listOf(
                navArgument(Rutas.ARG_MATERIAL_ID) { type = NavType.StringType }
            )
        ) { entrada ->
            val materialId = entrada.arguments?.getString(Rutas.ARG_MATERIAL_ID).orEmpty()
            PantallaEnrolar(
                materialId = materialId,
                onVolver = { navController.popBackStack() }
            )
        }

        // 8. Localizacion dentro del almacen
        composable(
            route = Rutas.UBICACION,
            arguments = listOf(
                navArgument(Rutas.ARG_MATERIAL_ID) { type = NavType.StringType }
            )
        ) { entrada ->
            val materialId = entrada.arguments?.getString(Rutas.ARG_MATERIAL_ID).orEmpty()
            PantallaUbicacion(
                materialId = materialId,
                onVolver = { navController.popBackStack() }
            )
        }

        // 9. Historial de busquedas
        composable(Rutas.HISTORIAL) {
            PantallaHistorial(
                onVolver = { navController.popBackStack() },
                onVerDetalle = { id -> navController.navigate(Rutas.detalle(id)) }
            )
        }

        // 10. Administracion de materiales
        composable(Rutas.ADMIN) {
            PantallaAdmin(
                puedeEditar = sesion.usuarioActivo?.puedeAdministrar == true,
                onVolver = { navController.popBackStack() },
                onVerDetalle = { id -> navController.navigate(Rutas.detalle(id)) },
                onEditar = { id -> navController.navigate(Rutas.editar(id)) },
                onNuevo = { navController.navigate(Rutas.editar(null)) }
            )
        }

        // Alta y edicion de materiales
        composable(
            route = Rutas.EDITAR,
            arguments = listOf(
                navArgument(Rutas.ARG_MATERIAL_ID) {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument(Rutas.ARG_APRENDER) {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { entrada ->
            val materialId = entrada.arguments?.getString(Rutas.ARG_MATERIAL_ID).orEmpty()
            val aprenderFoto = entrada.arguments?.getBoolean(Rutas.ARG_APRENDER) ?: false

            if (sesion.usuarioActivo?.puedeAdministrar != true) {
                PantallaBase("Acceso restringido", onVolver = { navController.popBackStack() }) {
                    Text("Solo un administrador puede registrar o editar piezas.", modifier = it)
                }
                return@composable
            }

            // Solo tiene sentido cuando el alta nace de una identificacion sin
            // acierto: es ahi donde la IA generica pudo adivinar de que tipo de
            // objeto se trata.
            val pistas = if (aprenderFoto) {
                when (val actual = identificacion.estado.collectAsStateWithLifecycle().value) {
                    is EstadoIdentificacion.NoIdentificado -> actual.pistas
                    is EstadoIdentificacion.Similares -> actual.pistas
                    else -> emptyList()
                }
            } else {
                emptyList()
            }
            val pistaPrincipal = pistas.firstOrNull()

            PantallaEditarMaterial(
                materialId = materialId.ifBlank { null },
                sugerenciaNombre = pistaPrincipal?.etiqueta,
                sugerenciaCategoria = pistaPrincipal?.categoria,
                onVolver = { navController.popBackStack() },
                onBaja = {
                    Toast.makeText(contexto, "Pieza dada de baja. Su historial se conserva.", Toast.LENGTH_SHORT).show()
                    navController.popBackStack()
                    if (navController.currentDestination?.route == Rutas.DETALLE) navController.popBackStack()
                },
                onGuardado = { idGuardado ->
                    Toast.makeText(contexto, "Pieza guardada correctamente", Toast.LENGTH_SHORT).show()
                    if (aprenderFoto) {
                        identificacion.confirmarPieza(
                            contexto = contexto,
                            materialId = idGuardado,
                            usuarioId = sesion.usuarioActivo?.id.orEmpty()
                        )
                        // Vuelve hasta el resultado, saltandose la pantalla de
                        // eleccion si se llego a traves de ella.
                        navController.popBackStack(Rutas.RESULTADO, inclusive = false)
                    } else {
                        navController.popBackStack()
                        if (navController.currentDestination?.route != Rutas.DETALLE) {
                            navController.navigate(Rutas.detalle(idGuardado))
                        }
                    }
                }
            )
        }
    }
}

/**
 * Vuelve al login vaciando toda la pila de navegacion, para que al cerrar
 * sesion no quede ninguna pantalla anterior accesible con el boton atras.
 */
private fun NavHostController.volverAlLogin() {
    navigate(Rutas.LOGIN) {
        // El login ya salió de la pila al entrar. Vaciar el grafo impide
        // regresar a pantallas autenticadas después de cerrar sesión.
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}
