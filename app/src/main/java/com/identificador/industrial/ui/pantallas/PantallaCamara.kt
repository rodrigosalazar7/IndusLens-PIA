package com.identificador.industrial.ui.pantallas

import com.identificador.industrial.ui.theme.Espaciado
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import com.identificador.industrial.ui.componentes.BotonPrincipal
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import com.identificador.industrial.ui.componentes.BotonSecundario
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.identificador.industrial.ia.Fotos
import com.identificador.industrial.ui.componentes.PantallaBase
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

/**
 * Pantalla 3: camara.
 *
 * No sabe para que se usa la foto: solo la entrega. Asi la misma pantalla
 * sirve tanto para identificar una pieza como para ensenarle una nueva a la
 * aplicacion, sin duplicar toda la gestion de camara y permisos.
 */
@Composable
fun PantallaCamara(
    titulo: String,
    textoGuia: String,
    onVolver: () -> Unit,
    onFotoTomada: (android.net.Uri) -> Unit
) {
    val contexto = LocalContext.current
    val cicloVida = LocalLifecycleOwner.current
    val alcance = rememberCoroutineScope()

    var permisoConcedido by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(contexto, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var permisoRechazado by remember { mutableStateOf(false) }
    var errorCamara by remember { mutableStateOf<String?>(null) }
    var linternaEncendida by remember { mutableStateOf(false) }
    var capturando by remember { mutableStateOf(false) }
    var camara by remember { mutableStateOf<Camera?>(null) }
    var proveedorCamara by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var intentoCamara by remember { mutableStateOf(0) }

    val vistaPrevia = remember {
        PreviewView(contexto).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
    }
    val captura = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }

    val pedirPermiso = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        permisoConcedido = concedido
        permisoRechazado = !concedido
    }

    // OpenDocument muestra Fotos, Descargas y cualquier proveedor de archivos
    // instalado. En el emulador tambien permite abrir una imagen que se haya
    // arrastrado desde la Mac a su ventana (Android la copia a Descargas).
    val elegirImagen = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            // La identificacion conserva el URI en el historial. Al hacer
            // persistente el permiso la imagen sigue siendo legible aunque se
            // cierre y se vuelva a abrir la aplicacion.
            try {
                contexto.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
                // Algunos proveedores solo dan permiso temporal. Es suficiente
                // para el analisis actual, por eso no se bloquea el flujo.
            }
            onFotoTomada(uri)
        }
    }

    fun abrirSelectorDeImagen() {
        elegirImagen.launch(arrayOf("image/*"))
    }

    LaunchedEffect(Unit) {
        if (!permisoConcedido) pedirPermiso.launch(Manifest.permission.CAMERA)
    }

    LaunchedEffect(permisoConcedido, intentoCamara) {
        if (!permisoConcedido) return@LaunchedEffect
        try {
            val proveedor = proveedorDeCamara(contexto)
            proveedorCamara = proveedor
            val previa = Preview.Builder().build()
            previa.setSurfaceProvider(vistaPrevia.surfaceProvider)
            proveedor.unbindAll()

            // Normalmente se usa la trasera. Si el AVD solo expone la webcam
            // como frontal, se utiliza automaticamente en vez de dejar la
            // pantalla inutilizable.
            val selector = when {
                proveedor.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) ->
                    CameraSelector.DEFAULT_BACK_CAMERA
                proveedor.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) ->
                    CameraSelector.DEFAULT_FRONT_CAMERA
                else -> error("No se encontro ninguna camara disponible")
            }

            camara = proveedor.bindToLifecycle(
                cicloVida,
                selector,
                previa,
                captura
            )
            errorCamara = null
        } catch (e: Exception) {
            errorCamara = e.message ?: "No se pudo abrir la camara"
        }
    }

    // Al salir de la pantalla se apaga la linterna: si no, se queda encendida
    // consumiendo bateria y calentando el equipo sin que nadie lo note.
    DisposableEffect(Unit) {
        onDispose {
            camara?.cameraControl?.enableTorch(false)
            proveedorCamara?.unbindAll()
        }
    }

    fun disparar() {
        // Una sola captura por pulsacion. Sin esta guarda, un segundo disparo
        // -por un toque repetido o por un reintento de CameraX- entrega una
        // foto tardia que pisa el analisis ya en marcha.
        if (capturando) return
        capturando = true

        // El emulador puede mostrar la webcam del host pero dejar esperando
        // para siempre una captura JPEG de ImageCapture. Para las pruebas en
        // computadora se guarda el fotograma que ya muestra PreviewView. La
        // IA recibe el mismo Uri y no necesita ningun camino especial.
        if (esEmulador()) {
            alcance.launch {
                var fotograma = vistaPrevia.bitmap
                val limite = SystemClock.uptimeMillis() + ESPERA_FOTOGRAMA_MS
                while (fotograma == null && SystemClock.uptimeMillis() < limite) {
                    delay(100)
                    fotograma = vistaPrevia.bitmap
                }

                if (fotograma == null) {
                    capturando = false
                    errorCamara =
                        "La webcam esta abierta, pero no entrego ningun fotograma. " +
                            "Comprueba el permiso de Camara de Android Studio en macOS."
                    return@launch
                }

                try {
                    val uri = withContext(Dispatchers.IO) {
                        Fotos.guardarBitmap(contexto, fotograma)
                    }
                    onFotoTomada(uri)
                } catch (e: Exception) {
                    errorCamara = e.message ?: "No se pudo guardar el fotograma de la webcam"
                } finally {
                    fotograma.recycle()
                    capturando = false
                }
            }
            return
        }

        captura.takePicture(
            ContextCompat.getMainExecutor(contexto),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    try {
                        onFotoTomada(Fotos.guardarCaptura(contexto, image))
                    } catch (e: Exception) {
                        errorCamara = e.message ?: "No se pudo guardar la fotografia"
                    } finally {
                        image.close()
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    capturando = false
                    errorCamara = exception.message ?: "Fallo al tomar la fotografia"
                }
            }
        )
    }

    PantallaBase(titulo = titulo, onVolver = onVolver) { modifier ->
        Box(modifier = modifier.fillMaxSize()) {

            when {
                !permisoConcedido -> SinPermiso(
                    rechazado = permisoRechazado,
                    onReintentar = { pedirPermiso.launch(Manifest.permission.CAMERA) },
                    onSeleccionarImagen = { abrirSelectorDeImagen() }
                )

                errorCamara != null -> SinCamara(
                    detalle = errorCamara.orEmpty(),
                    onReintentar = {
                        errorCamara = null
                        intentoCamara++
                    },
                    onSeleccionarImagen = { abrirSelectorDeImagen() }
                )

                else -> {
                    AndroidView(
                        factory = { vistaPrevia },
                        modifier = Modifier.fillMaxSize()
                    )
                    MarcoGuia(textoGuia)
                    ControlesCamara(
                        linternaEncendida = linternaEncendida,
                        hayLinterna = camara?.cameraInfo?.hasFlashUnit() == true,
                        capturando = capturando,
                        onDisparar = { disparar() },
                        onLinterna = {
                            linternaEncendida = !linternaEncendida
                            camara?.cameraControl?.enableTorch(linternaEncendida)
                        },
                        onSeleccionarImagen = { abrirSelectorDeImagen() }
                    )
                }
            }
        }
    }
}

/**
 * Marco que indica donde encuadrar la pieza.
 *
 * No recorta nada: el analisis usa la foto completa. Sirve para que el
 * operador acerque y centre la pieza, que es lo que de verdad mejora la
 * lectura del numero de parte.
 */
@Composable
private fun MarcoGuia(texto: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = MaterialTheme.shapes.large
                    )
            )
            Spacer(Modifier.height(18.dp))
            Text(
                text = texto,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .background(
                        color = Color.Black.copy(alpha = 0.55f),
                        shape = MaterialTheme.shapes.small
                    )
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            )
        }
    }
}

@Composable
private fun ControlesCamara(
    linternaEncendida: Boolean,
    hayLinterna: Boolean,
    capturando: Boolean,
    onDisparar: () -> Unit,
    onLinterna: () -> Unit,
    onSeleccionarImagen: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(vertical = 20.dp, horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onSeleccionarImagen) {
                Text("Abrir imagen", color = Color.White)
            }

            // Boton de disparo grande: se usa con guantes de trabajo.
            Box(
                modifier = Modifier
                    .size(74.dp)
                    .background(color = Color.White, shape = CircleShape)
                    .padding(6.dp)
            ) {
                Button(
                    onClick = onDisparar,
                    enabled = !capturando,
                    shape = CircleShape,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                    modifier = Modifier.fillMaxSize()
                ) {}
            }

            TextButton(onClick = onLinterna, enabled = hayLinterna) {
                Text(
                    text = if (linternaEncendida) "Apagar luz" else "Luz",
                    color = if (hayLinterna) Color.White else Color.White.copy(alpha = 0.4f)
                )
            }
        }
    }
}

@Composable
private fun SinPermiso(
    rechazado: Boolean,
    onReintentar: () -> Unit,
    onSeleccionarImagen: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Espaciado.extraGrande),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (rechazado) {
                "No se concedio acceso a la camara. Puedes intentarlo otra vez " +
                    "o continuar con una imagen de Fotos, Descargas o Archivos."
            } else {
                "Concede el permiso de camara para hacer pruebas en vivo o " +
                    "selecciona una imagen guardada."
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(Espaciado.grande))
        BotonPrincipal(onClick = onReintentar, shape = MaterialTheme.shapes.medium) {
            Text("Conceder permiso")
        }
        Spacer(Modifier.height(Espaciado.medio))
        BotonSecundario(onClick = onSeleccionarImagen, shape = MaterialTheme.shapes.medium) {
            Text("Seleccionar imagen")
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Acepta imagenes de Fotos, Descargas y Archivos.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SinCamara(
    detalle: String,
    onReintentar: () -> Unit,
    onSeleccionarImagen: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Espaciado.extraGrande),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "No se pudo iniciar la camara",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = detalle,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(Espaciado.grande))
        BotonPrincipal(onClick = onReintentar, shape = MaterialTheme.shapes.medium) {
            Text("Reintentar camara")
        }
        Spacer(Modifier.height(Espaciado.medio))
        BotonSecundario(
            onClick = onSeleccionarImagen,
            shape = MaterialTheme.shapes.medium
        ) {
            Text("Seleccionar imagen")
        }
    }
}

/**
 * CameraX entrega su proveedor mediante un ListenableFuture de Guava.
 * Este envoltorio lo convierte en algo que se puede esperar con corrutinas.
 */
private suspend fun proveedorDeCamara(contexto: Context): ProcessCameraProvider =
    suspendCancellableCoroutine { continuacion ->
        val futuro = ProcessCameraProvider.getInstance(contexto)
        futuro.addListener(
            {
                try {
                    continuacion.resume(futuro.get())
                } catch (e: Exception) {
                    continuacion.resumeWithException(e)
                }
            },
            ContextCompat.getMainExecutor(contexto)
        )
    }

/** No depende de una marca concreta de AVD y tambien cubre Genymotion. */
private fun esEmulador(): Boolean =
    Build.FINGERPRINT.startsWith("generic") ||
        Build.FINGERPRINT.contains("emulator", ignoreCase = true) ||
        Build.MODEL.contains("Emulator", ignoreCase = true) ||
        Build.MODEL.contains("Android SDK", ignoreCase = true) ||
        Build.PRODUCT.contains("sdk_gphone", ignoreCase = true)

private const val ESPERA_FOTOGRAMA_MS = 3_000L
