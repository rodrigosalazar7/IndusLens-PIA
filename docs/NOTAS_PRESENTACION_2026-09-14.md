# IndusLens · Preparación de la presentación

Presentación: **14 de septiembre de 2026, 17:30 h (Monterrey)**.

Alcance verificado: prototipo local para el kit del semestre, no sistema empresarial en producción.

## Control del trabajo y Git

- Repositorio del equipo: https://github.com/rodrigosalazar7/IndusLens-PIA
- Rama local de trabajo: `feature/pantallas-presentacion`.
- Último `main` revisado: `6ce10d19e34812e64634ee22ce614c0ab938f5cf`.
- Se integraron los avances del equipo con la rama de actividades 11–14, sin sustituir ni reescribir sus commits.
- Commit de integración **solo local**: `6abb96c`. Padres: `b75e3eb` y `6ce10d1`. Se creó antes de la indicación posterior de esperar autorización para publicar.
- Las mejoras de las pantallas y estas notas quedan como cambios locales pendientes de revisión.
- **No se realizó push, no se abrió un pull request y no se modificó main en GitHub. No publicar hasta recibir autorización.**
- Se conserva intacta la copia anidada `IndusLens-PIA-main/` añadida por el equipo. La compilación corresponde al proyecto raíz. Su limpieza requerirá una decisión del equipo.
- `local.properties`, APK, bases y evidencias generadas siguen fuera del seguimiento de Git.

## Avances del equipo revisados

| Commit | Cambio | Verificación |
| --- | --- | --- |
| `1cdc243`, `4a667ed` | Recuperación de números de parte y ajustes de OCR | Pruebas del extractor conservadas y ejecutadas |
| `c80fff1` | Alta de una pieza desde un resultado sin coincidencia | Flujo conservado, permisos de alta limitados al administrador |
| `6ce10d1` | Ubicación simplificada y catálogo inicial vacío | Se conserva el catálogo vacío en instalaciones nuevas; no se borran datos existentes |
| `81e1e25`, `94fd94e` | Acceso por correo, persistencia de sesión y servicios remotos | Integrados; acceso local probado; servicios remotos no certificados |

GitHub no mostró ejecuciones de Actions ni checks para el último commit de main durante esta revisión. La compilación automática está configurada para pull requests; no se atribuyen resultados de CI a pruebas realizadas localmente.

## Cambios funcionales

### Inicio de sesión y navegación

- Se conserva el acceso por correo del equipo y las tres cuentas de demostración.
- El menú indica si se utiliza una cuenta local.
- Al restaurar una sesión local se vuelve a consultar en Room el estado y rol de la cuenta.
- Una cuenta local cierra cualquier sesión remota anterior para no enviar sus operaciones a Firebase.
- Al salir se vacía la navegación para no volver a pantallas autenticadas con Atrás.
- Los operadores pueden consultar el catálogo y el detalle; el alta, edición y baja se reservan al administrador.

### Catálogo

- Buscador por nombre, número de parte, fabricante y descripción.
- Estados diferenciados de carga, catálogo vacío, búsqueda sin coincidencias y error con reintento.
- Botón visible para registrar la primera pieza y altas posteriores.
- Existencias y ubicación proceden de la base local y se actualizan tras guardar.

### Detalle de pieza

- Accesos visibles a editar inventario y consultar ubicación.
- Datos opcionales vacíos con explicación; distribución adaptable de etiquetas y valores.
- Indicador de baja y bloqueo de acciones que no corresponden a una pieza inactiva.
- Estado de error con reintento.
- La edición conserva la referencia fotográfica existente.
- Se retiraron mensajes que prometían reconocimiento infalible: las referencias visuales requieren comprobación.

### Alta y edición

- Formulario organizado en identificación, inventario, ubicación e información opcional.
- Selector que muestra siempre la categoría elegida.
- Botón de guardar visible sin recorrer todo el formulario.
- Validación junto a cada campo: nombre, código, cantidades enteras no negativas, ubicación y nivel/posición desde 1.
- Ajustes de inventario sin negativos ni desbordamiento numérico.
- Bloqueo de doble guardado y doble baja.
- Advertencia antes de descartar cambios; borrador conservado en el estado de Android.
- Carga fallida o identificador inexistente no se convierten accidentalmente en un alta.
- Duplicados detectados sin distinguir formato del código, también en piezas dadas de baja.
- Se evita reemplazar silenciosamente otra pieza y se detectan cambios ocurridos mientras se editaba.
- Baja lógica que conserva registro, historial y referencias; la pieza deja de participar en catálogo y reconocimiento.

## Verificación técnica

- Compilación debug: correcta.
- Compilación release: correcta; el APK release permanece **sin firma de distribución**.
- Pruebas JVM: **22 aprobadas**.
- Primer pase en Android: **25 aprobadas**.
- Pase final en modo avión, sin red predeterminada activa: **27 pruebas Android aprobadas** (49 junto con JVM). Incluye el modelo OCR local sobre una etiqueta controlada.
- Recorrido manual adicional: selección de imagen desde Descargas sin permiso de cámara → lectura OCR de `DEMO-001` → resultado → detalle → ubicación.
- Edición manual: pieza `M-001`, categoría Herramientas y existencia 12, verificadas después en SQLite y tras reiniciar la app.
- Salir y pulsar Atrás regresa al inicio de Android, no a una pantalla autenticada.
- Registro de fallos de Android consultado al final del recorrido: sin cierres inesperados.
- Modo avión desactivado al terminar, devolviendo la conectividad del emulador a su estado anterior.
- Emulador: Pixel 8, ARM64, Android 37.2 beta; se mantiene `minSdk = 26`.
- No se ha certificado ejecución en un dispositivo físico con API 26 ni en iOS.
- Base Room: se conserva versión 5 y las migraciones anteriores. Sin reinicio destructivo de la base.
- Identificador de instalación conservado del trabajo del equipo: `com.identificador.industrial.control` (nombre en Android: **IndusLens Control**). Convive con la app anterior `com.identificador.industrial`; no la sobrescribe ni importa automáticamente sus datos.

Evidencias locales, excluidas de Git: `app/build/outputs/verificacion-presentacion/`.

Archivos principales de evidencia:

- `compilacion-final.txt`: compilación final correcta.
- `android-tests-final.txt`: resultado `OK (27 tests)`.
- `06-resultado-ocr.png`: identificación sin conexión desde una imagen.
- `07-edicion-final.png`, `08-detalle-final.png`, `09-catalogo-final.png`: revisión visual.
- `base-final-verificada.tar`: respaldo de la base del emulador después de la prueba.

APK debug universal: `app/build/outputs/apk/debug/app-universal-debug.apk`.
Firma de depuración v2 verificada. SHA-256:
`1f7dd3b72304bc449664a0ed9864559c2214d8d1b99ecbf76be9a9afe0f14cf6`.

## Guion de demostración

1. Abrir **IndusLens Control** e iniciar con `admin / admin`.
2. Entrar en **Catálogo de piezas**.
3. Registrar una pieza del kit con un código único, categoría, existencia y ubicación.
4. Guardar y revisar su detalle. Abrir **Ver ubicación en almacén**.
5. Editar la existencia o ubicación y comprobar el cambio.
6. Volver al catálogo, buscar la pieza y probar una búsqueda sin resultados.
7. Cerrar y abrir la app: verificar que la pieza permanece.
8. Como prueba adicional, intentar un código repetido o una cantidad negativa: debe mostrarse el error sin perder lo escrito.

Las piezas de ejemplo del emulador son datos de prueba, no el inventario real del equipo. El kit real debe registrarse en el dispositivo que se llevará a clase.

## Límites y pendientes del equipo

- Correo verificado, recuperación de contraseña, permisos de Firestore, sincronización entre dispositivos y reconocimiento en línea necesitan pruebas con cuentas y configuración autorizadas. No se desplegaron reglas ni se escribieron datos de prueba en Firebase.
- El acceso remoto actual comparte caché con datos locales y no equivale a un sistema multiempresa. No presentar aislamiento entre empresas o resolución de conflictos remotos como funciones terminadas.
- La prueba de OCR usa una etiqueta controlada. El reconocimiento visual debe ensayarse con fotografías del kit real, sus ángulos e iluminación.
- No se verificó cámara física de Mac, iPhone o Android en esta sesión.
- API 26 es el mínimo de instalación, no una afirmación de pruebas físicas realizadas en esa versión.
- Pendiente de autorización: revisar diff, preparar el commit final, publicar la rama y seguir el proceso de revisión del equipo.

## Seguimiento

Monitor de Git cada hora, hasta la presentación. Solo consulta: no integra, no modifica código y no publica. Avisa de cambios nuevos relevantes, fallos o decisiones necesarias; permanece en silencio si no hay novedades.

Las tareas locales programadas requieren el equipo encendido y la aplicación abierta, según la [documentación de automatizaciones](https://learn.chatgpt.com/docs/automations?surface=app).
