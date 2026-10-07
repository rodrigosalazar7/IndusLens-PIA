# Avance - 7 de octubre de 2026

Segun el cronograma, el 7 de octubre corresponden las actividades 28 a 31:

| N.º | Actividad | Resp. | Estado |
|----|-----------|-------|--------|
| 28 | Estructura MVVM: Repository y ViewModel del catalogo | R | Cumplida (ya existia; se amplio el ViewModel) |
| 29 | Carga y validacion del catalogo inicial de prueba | E | Cumplida hoy: se agrego la validacion |
| 30 | Listado de materiales con busqueda por texto | Ed | Cumplida hoy: ahora disponible para todos los roles |
| 31 | Pantalla de detalle del material con su ficha completa | A | Cumplida (ya existia) |

## Resumen de cambios hasta hoy

**Del 24 de agosto al 9 de septiembre** (entrega de medio curso adelantada):
diseno base, estructura por capas, Room, login con tres roles y contrasenas
con SHA-256 y sal, sesion persistente, registro con correo en Firebase,
camara CameraX, OCR con ML Kit, reconocimiento con EfficientNet-Lite y
Gemini, y similitud visual con MobileNet.

**13 y 14 de septiembre:**
- OCR: mejor recuperacion del numero de parte cuando se lee partido o con
  confusiones (O por 0).
- Alta de una pieza nueva directamente desde un resultado sin coincidencia.
- Catalogo semilla con 4 tornillos de prueba, con fotos y huellas visuales
  precargadas en el APK.
- IA: se descartan pistas genericas sin categoria.
- Login: se quito el recuadro con credenciales de demostracion.

**7 de octubre (rama `feature/catalogo-29-31-consulta-y-validacion`):**
- Validacion del catalogo inicial antes de cargarlo en la base.
- Nueva pantalla **Catalogo de materiales** para todos los roles, con busqueda
  por texto y filtro por categoria.
- Pruebas automaticas del validador.

## Explicacion del codigo modificado hoy

### 1. `datos/ValidadorCatalogo.kt` (nuevo) - actividad 29

**Problema:** la tabla `materiales` tiene un indice unico sobre el numero de
parte. Si el catalogo inicial trajera dos piezas con el mismo numero, la
insercion fallaria y la app se cerraria la primera vez que se abre.

**Solucion:** antes de sembrar, `ValidadorCatalogo.validar(lista)` revisa cada
material y devuelve dos listas: los **validos** y los **problemas** (con el
motivo). Revisa:
- clave con formato `M-000` y sin repetir;
- numero de parte presente y sin repetir, comparado **normalizado**
  (`TOR-001` y `TOR 001` cuentan como el mismo, igual que en el OCR);
- nombre, fabricante y unidad de medida no vacios;
- existencias no negativas;
- ubicacion completa, con nivel y posicion desde 1.

### 2. `datos/local/BaseDatos.kt` (modificado) - actividad 29

En `Sembrado.onCreate` ahora se valida primero y solo se insertan los
materiales validos. Los descartados se reportan en Logcat con
`Log.w("BaseDatos", ...)`. Las huellas visuales de materiales descartados
tampoco se cargan, para no dejar datos huerfanos.

### 3. `ui/pantallas/CatalogoViewModel.kt` (modificado) - actividades 28 y 30

Se agrego el **filtro por categoria**:
- `categoria`: la categoria elegida (`null` = todas).
- `alternarCategoria(c)`: elige una categoria; si se pulsa de nuevo, la quita.
- `categoriasDisponibles`: solo las categorias que tienen materiales, para no
  mostrar filtros que siempre darian una lista vacia.

La lista `materiales` combina (`combine`) el resultado de la busqueda por
texto con la categoria elegida. La busqueda sigue usando `debounce(250)` para
no consultar en cada tecla y `flatMapLatest` para descartar busquedas viejas.

### 4. `ui/pantallas/PantallaCatalogo.kt` (nuevo) - actividad 30

Pantalla de **solo consulta** para operador, almacenista y administrador.
Muestra el buscador, una fila de chips de categoria (`FilterChip`) y la lista
de materiales. Al tocar un material abre su ficha (actividad 31).

### 5. `ui/pantallas/PantallaAdmin.kt` (modificado)

Para no duplicar codigo, el buscador y la lista se separaron en dos
componentes reutilizables: `BuscadorCatalogo` y `ListaMateriales`. Los usan
la pantalla de administracion y la de consulta. Si no se pasa `onEditar`, la
lista no muestra el boton de editar (modo solo lectura).

### 6. Navegacion y menu

- `navegacion/Rutas.kt`: nueva ruta `CATALOGO`.
- `navegacion/NavegacionApp.kt`: se registra `PantallaCatalogo` en el grafo.
- `ui/pantallas/PantallaMenu.kt`: nueva tarjeta **Catalogo de materiales**,
  visible para todos los roles.

### 7. `test/.../datos/ValidadorCatalogoTest.kt` (nuevo)

Cuatro pruebas: el catalogo real de la app es valido; se descarta un numero
de parte repetido aunque cambie el formato; se descarta una clave repetida; y
se descartan datos vacios o fuera de rango.

## Verificacion

- Compilacion `debug` correcta el 7 de octubre de 2026.
- 11 pruebas automaticas ejecutadas: 11 correctas, 0 fallos (4 nuevas del validador).
- Cambios integrados en `main`.

## Pendiente

- Probar la nueva pantalla de catalogo en un telefono fisico.
- Proximas actividades (12 de octubre): 32 y 33, CameraX y pantalla de
  captura, que ya existen desde septiembre y solo requieren revision.
