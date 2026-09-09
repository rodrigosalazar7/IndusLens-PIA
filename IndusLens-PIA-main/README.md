# IndusLens · Identificador industrial inteligente

Aplicacion Android que identifica piezas y componentes industriales a partir de
una fotografia, y muestra su informacion y su ubicacion dentro del almacen.

**Flujo:** Fotografia → la IA identifica la pieza → consulta a la base de datos →
muestra informacion y ubicacion.

## Colaboracion del equipo

- [Guia de contribucion](CONTRIBUTING.md): ramas, commits y Pull Requests.
- [Arquitectura](docs/ARQUITECTURA.md): estructura de paquetes y dependencias.
- [Actividades 09-11](docs/VERIFICACION_ACTIVIDADES_09_11.md): evidencia al 4 de septiembre de 2026.
- Cada Pull Request dirigido a `main` se compila automaticamente con Android CI.

---

## Como abrir el proyecto

1. Instala Android Studio (incluye el JDK 17, el SDK de Android y Gradle).
2. Abre Android Studio → **Open** → selecciona esta carpeta
   (`Identificador-industrial`), no una subcarpeta.
3. Espera a que termine el *Gradle sync*. La primera vez descarga el SDK y las
   dependencias, puede tardar varios minutos.
4. Conecta un telefono con **depuracion USB** activada, o crea un emulador en
   *Device Manager*, y pulsa **Run**.

### Probar en una Mac sin telefono Android

La aplicacion Android se puede probar completa en un AVD. En el Pixel 8 de
Android Studio abre **Device Manager → Edit → Additional settings** y asigna
`Webcam0` a la camara trasera. Antes de iniciar el AVD se puede comprobar que
esa fuente sea la camara integrada:

```bash
arch -arm64 "$HOME/Library/Android/sdk/emulator/emulator" -webcam-list
```

La salida debe indicar `Camara FaceTime HD = webcam0`. Si aparece la camara del
iPhone, desconecta temporalmente **Continuity Camera** y reinicia el emulador;
el AVD conserva el nombre `webcam0`, pero macOS puede cambiar que dispositivo
ocupa esa posicion. La app guarda el fotograma visible de `PreviewView` en el
emulador, porque algunas webcams virtuales muestran video pero no completan la
captura JPEG de CameraX. En un telefono real sigue usando `ImageCapture`.

Tambien se pueden probar imagenes sin camara:

1. Arrastra un JPG, PNG o WebP desde Finder hasta la ventana del emulador.
2. Android lo copia a **Downloads**.
3. En la pantalla de camara pulsa **Abrir imagen**.
4. Abre el menu lateral del selector, entra a **Downloads** y elige el archivo.

El selector tambien acepta Fotos y otros proveedores de archivos. Todo el
procesamiento es local: seleccionar una imagen no la sube a Internet.

### Compilar desde la consola

No hace falta abrir el IDE para generar el APK:

```bash
cd ruta/al/Identificador-industrial
./gradlew assembleDebug
```

El APK queda en `app/build/outputs/apk/debug/app-debug.apk`.

Para instalarlo en un telefono conectado por USB:

```bash
%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe install -r app/build/outputs/apk/debug/app-debug.apk
```

### Cuentas de prueba

| Usuario    | Contrasena | Rol           |
|------------|------------|---------------|
| `operador` | `1234`     | Operador      |
| `almacen`  | `1234`     | Almacenista   |
| `admin`    | `admin`    | Administrador |

Solo el administrador ve habilitada la pantalla de administracion de materiales.

Tambien se pueden crear cuentas con correo. El enlace de verificacion se envia
con Firebase Authentication y la app no permite entrar hasta que el correo se
confirma. La configuracion inicial esta en
[`docs/CONFIGURAR_FIREBASE.md`](docs/CONFIGURAR_FIREBASE.md).

---

## Estado del proyecto

| Fase | Contenido | Estado |
|------|-----------|--------|
| 1 | Estructura, navegacion, login, sesion persistente y verificacion por correo | **Terminada** |
| 2 | Base de datos Room, modelo de materiales y catalogo de ejemplo | **Terminada y verificada** |
| 3 | CameraX + OCR con ML Kit, busqueda por numero de parte | **Terminada y verificada** |
| 4 | TensorFlow Lite, embeddings y busqueda por similitud visual | **Terminada y verificada** |
| 5 | Ubicacion en almacen, historial y alta de materiales | **Terminada y verificada** |
| 6 | Compilacion del APK firmado | Pendiente |

Las pantallas que aun no tienen logica se pueden abrir y recorrer: muestran que
va a contener cada una y en que fase se construye.

---

## Las 10 pantallas

| # | Pantalla | Ruta | Fase |
|---|----------|------|------|
| 1 | Inicio de sesion | `login` | 1 |
| 2 | Menu principal | `menu` | 1 |
| 3 | Camara para identificar pieza | `camara` | 3 · lista |
| 4 | Procesamiento con IA | `procesando` | 3 · lista |
| 5 | Resultado de identificacion | `resultado` | 3 · lista |
| 6 | Piezas similares | `similares` | 4 · lista |
| 7 | Detalle del material | `detalle/{materialId}` | 2 · lista |
| 8 | Localizacion en almacen | `ubicacion/{materialId}` | 5 · lista |
| 9 | Historial de busquedas | `historial` | 5 · lista |
| 10 | Administracion de materiales | `admin` | 2 · lista |

---

## Edicion del catalogo

El administrador tiene el lapiz en dos sitios: en cada fila del catalogo y en
la barra de la ficha del material. El segundo importa mas de lo que parece: se
suele llegar a la ficha tras identificar una pieza y descubrir alli mismo que
la existencia no cuadra con lo que hay en el rack. Poder corregirlo en ese
momento evita que el error siga en el sistema durante meses.

Se puede cambiar todo: nombre, numero de parte, fabricante, descripcion,
categoria, medidas, existencias y la ubicacion completa (almacen, pasillo,
rack, nivel y posicion).

La existencia lleva botones de **-10, -1, +1 y +10** ademas del teclado.
Contar en el rack y corregir de uno en uno es la operacion mas frecuente del
almacen, y hacerlo tecleando es lento y propenso a erratas. Nunca baja de cero:
una existencia negativa no significa nada.

## Decisiones de la fase 5

- **El plano del almacen se dibuja con los pasillos y racks que existen de
  verdad** en el catalogo, no con una distribucion inventada. Sirve para
  situarse ("estoy en el tercero de cinco pasillos"); un plano bonito pero
  falso no ayudaria a nadie a encontrar nada.
- **Dar de baja no borra.** El material se marca como inactivo: el historial
  apunta a el y borrarlo dejaria registros huerfanos. Ademas, en un almacen,
  "esta pieza ya no se usa" y "esta pieza nunca existio" no son lo mismo.
- **La clave nueva se calcula sobre el maximo existente**, no contando
  materiales. Si se da de baja una pieza intermedia, contar generaria una clave
  ya usada y el alta fallaria por duplicado.
- **El historial resuelve el nombre de la pieza al mostrarlo**, no guarda una
  copia. Asi, si alguien corrige el catalogo, el historial refleja la
  correccion en vez de arrastrar datos viejos.

## Base de datos

Tres tablas en SQLite mediante Room. El esquema se exporta a `app/schemas`.

| Tabla | Contenido |
|-------|-----------|
| `materiales` | Catalogo de piezas. 26 registros de ejemplo se insertan al crear la base |
| `usuarios` | Cuentas y roles. La contrasena se guarda como hash con sal por usuario |
| `historial_busquedas` | Registro de identificaciones (la pantalla llega en la fase 5) |
| `embeddings` | Huellas visuales. Varias por pieza, una por cada vista fotografiada. Tabla aparte a proposito: son varios kilobytes cada una y el catalogo se consulta constantemente |

Detalles de diseno que conviene conocer antes de tocar el codigo:

- **Los enums se guardan por nombre, no por posicion.** Si alguien reordena las
  constantes de `Categoria`, los datos ya guardados siguen siendo correctos.
  Con el ordinal, un rodamiento se convertiria en un tornillo en silencio.
- **`numeroParte` tiene indice unico**, y la consulta que lo busca ignora
  guiones, espacios y mayusculas. El OCR de la fase 3 puede leer `6205 2RS1`
  donde el catalogo dice `6205-2RS1`, y deben considerarse el mismo numero.
- **La ubicacion se guarda descompuesta** en almacen, pasillo, rack, nivel y
  posicion, no como una sola cadena, para poder ordenar por pasillo cuando se
  arme la ruta de recogida.
- **El catalogo semilla incluye cuatro rodamientos casi identicos a la vista.**
  Es deliberado: son el caso dificil que justifica dar prioridad al OCR sobre
  el parecido visual.
- **Las contrasenas nunca se guardan en claro.** Se usa SHA-256 con sal e
  iteraciones. Queda documentado en el codigo que lo correcto en produccion
  seria bcrypt, scrypt o Argon2; es deuda tecnica asumida, no un descuido.

---

## Como funciona la identificacion

La app combina dos metodos y da prioridad al primero:

1. **Lectura de texto (OCR).** Casi toda pieza industrial trae grabado o
   etiquetado su numero de parte. Si se puede leer, se busca ese numero
   directamente en la base de datos y la identificacion es exacta.
2. **Similitud visual.** Si la pieza no tiene marcas legibles, se calcula un
   *embedding* de la foto con MobileNet (TensorFlow Lite) y se compara con
   similitud coseno contra los vectores del catalogo, devolviendo las 5 piezas
   mas parecidas para que el usuario elija.

El segundo metodo no requiere entrenar ningun modelo: para dar de alta una pieza
nueva basta **una sola fotografia** de referencia, de la que se calcula su vector.
Por eso la busqueda visual escala sin reentrenamientos.

### De texto leido a numero de parte

Una etiqueta fotografiada no devuelve un numero limpio. Devuelve algo asi:

```
SKF
6205-2RS1
Made in France
25x52x15
LOT 240817
```

`ExtractorNumeroParte` se queda con `6205-2RS1` aplicando tres reglas:

1. Se descarta lo que no tenga ningun digito: casi siempre es una palabra.
2. Se descartan los numeros puros de menos de cuatro cifras: suelen ser medidas
   o cantidades, no referencias.
3. Se prueban tambien las **uniones de palabras contiguas**, porque el OCR
   parte con frecuencia el numero en dos trozos cuando en la pieza estan
   grabados con separacion.

Los candidatos se ordenan por especificidad: primero los que mezclan letras y
digitos, luego los mas largos. Se consulta uno a uno y gana el primero que
exista en el catalogo.

Ambos lados se normalizan igual, a solo letras y digitos en mayusculas. Eso es
lo que permite que una lectura de `6205 2rs1` encuentre al catalogado
`6205-2RS1`.

### Busqueda por parecido visual

Cuando el OCR no da resultado, la app calcula un *embedding* de la fotografia
con MobileNet v3 y lo compara con similitud coseno contra las huellas guardadas
del catalogo.

Como se ensena una pieza:

1. Abre su ficha en el catalogo.
2. Pulsa **Ensenar esta pieza**.
3. Fotografiala.
4. Repite con **Anadir otra vista** girando la pieza y cambiando el fondo.

No hay entrenamiento de por medio: el modelo nunca cambia, solo describe
imagenes. Por eso el catalogo puede crecer sin reentrenar nada.

**Cuantas vistas hacen falta.** Una sola ya permite reconocer la pieza, pero
solo desde angulos parecidos al fotografiado. Cada material admite varias
huellas y la comparacion se queda con la mejor de todas, asi que con unas
**cuatro vistas** desde lados y fondos distintos el reconocimiento pasa de
funcionar a veces a acertar casi siempre. La ficha del material indica cuantas
lleva.

Cada material compite con su mejor vista, nunca con todas a la vez: si no, una
pieza fotografiada seis veces ocuparia ella sola la lista de candidatas y
taparia al resto.

Para dar de alta muchas piezas a la vez existe
`herramientas/generar_huellas.py`, que calcula las huellas de una carpeta de
fotografias con el mismo modelo y deja un archivo que la app carga al
instalarse. Usar el mismo modelo no es opcional: vectores de modelos distintos
viven en espacios distintos y compararlos produce numeros que parecen validos
pero no significan nada.

### Cuando la pieza no se ha visto nunca

Si no hay numero legible ni huella parecida, la app no se rinde:

1. Un modelo generico (EfficientNet-Lite entrenado con ImageNet) dice **que
   tipo de objeto ve**: tornillo, cadena, martillo. Con una fotografia real de
   un tornillo acierta con soltura; con rodamientos o contactores se pierde,
   porque son objetos que apenas aparecen en las imagenes con las que se
   entreno. Por eso se muestra como pista y nunca como identificacion.
2. Se ofrece el catalogo, filtrado por esa familia cuando la pista sirve, para
   que la persona **senale cual es**.
3. Al senalarla, la fotografia se guarda como vista de referencia de esa pieza.

Ese tercer punto es lo importante: **cada correccion humana ensena una pieza a
la app**. No hace falta enrolar el catalogo entero de antemano; se aprende
usandola, y a las pocas semanas un almacen tiene sus piezas reconocidas.

**Detalle que costo encontrar:** la imagen se recorta al marco guia antes de
analizarla. Con la fotografia completa, el modelo describia la nave o la mesa
-respondia "habitacion" ante un tornillo- porque el escenario ocupa la mayoria
de los pixeles. El enrolamiento y la identificacion usan exactamente el mismo
recorte; si difirieran, los vectores no serian comparables y nada se
reconoceria.

### Las medidas de una pieza identificada

En cuanto la app sabe que pieza es, **no necesita medirla: ya lo sabe**. Cada
material del catalogo guarda sus medidas nominales y su medida clave, y se
muestran en la ficha y en el resultado.

Un 6205 mide 25 x 52 x 15 mm siempre. Ese dato del fabricante es exacto,
mientras que medir sobre una fotografia arrastra errores de pulso y de
perspectiva. Medir solo tiene sentido cuando la pieza no esta en el catalogo.

### Medir una pieza desconocida

Una fotografia no contiene el tamano absoluto de lo que retrata: un tornillo
pequeno de cerca y uno grande de lejos ocupan los mismos pixeles. Por eso medir
con una sola foto, sin nada mas, es imposible.

Con un objeto de tamano conocido dentro del encuadre si se puede, y de forma
exacta. La pantalla de medicion pide marcar primero el patron (una moneda, una
tarjeta bancaria) y despues la pieza, y devuelve milimetros, centimetros,
pulgadas y la **fraccion comercial** mas cercana, que es como se piden las
medidas en taller.

Condicion importante: el patron y la pieza deben estar apoyados en el mismo
plano. Si la moneda esta en la mesa y la pieza levantada hacia la camara, la
perspectiva falsea la escala.

#### Que hace la app con el parecido

| Situacion | Respuesta |
|-----------|-----------|
| Numero de parte leido y encontrado | Pieza identificada, 99% |
| Sin numero, parecido igual o mayor que 88% | Pieza identificada, con aviso de que fue por forma |
| Sin numero, parecido menor que 88% | Lista de candidatas para que decida la persona |
| Sin numero y sin huellas en el catalogo | Lo dice y explica como ensenar piezas |

El umbral del 88% es conservador a proposito. Equivocarse en un almacen no
cuesta un clic: cuesta que alguien monte la pieza incorrecta. Ante la duda es
preferible mostrar varias candidatas.

Y siempre que la identificacion venga de un parecido visual, la pantalla lo
dice con todas las letras y pide confirmar el numero de parte. Un rodamiento
6205 y un 6203 son casi identicos a la vista.

---

## Tecnologias

| Capa | Tecnologia |
|------|-----------|
| Lenguaje | Kotlin 2.4.10 |
| Interfaz | Jetpack Compose (BOM 2026.08.00), Material 3 |
| Navegacion | Navigation Compose 2.9.8 |
| Camara | CameraX 1.6.1 |
| OCR | ML Kit Text Recognition 16.0.1 (modelo empaquetado, funciona sin red) |
| Vision | MobileNet v3 (huellas) y EfficientNet-Lite (tipo de objeto), sobre TensorFlow Lite via MediaPipe Tasks 1.0.0 |
| Base de datos | Room 2.8.4 sobre SQLite, preparada para Supabase |
| Autenticacion | Local con Room + Firebase Auth para correo verificado |
| Procesador de anotaciones | KSP 2.3.9 |
| Procesamiento | Python, para generar los embeddings del catalogo *(fase 4)* |
| Compilacion | AGP 9.3.0, Gradle 9.7.0, JDK 17 |
| SDK | minSdk 26 (Android 8.0), compileSdk 37, targetSdk 36 |

### Notas de configuracion

Tres cosas que no son obvias y que hacen fallar la compilacion si se cambian:

1. **No se aplica el plugin `org.jetbrains.kotlin.android`.** Desde AGP 9 el
   soporte de Kotlin viene integrado en el plugin de Android, y aplicarlo aparte
   provoca un error de compilacion. Sus opciones (como `jvmTarget`) van dentro
   del bloque `android { kotlin { ... } }`. El plugin de Compose si se aplica
   por separado.
2. **`compileSdk` debe ser 37.** Las librerias del Compose BOM 2026.08 lo exigen.
   En el SDK Manager el paquete se llama `platforms;android-37.0`, con version
   menor. `targetSdk` se deja en 36 a proposito.
3. **Los iconos de Material se piden aparte** (`material-icons-core`). Material 3
   ya no los arrastra como dependencia. Se usan solo los del set base, porque
   `material-icons-extended` esta en proceso de retiro.

---

## Estructura

```
app/src/main/java/com/identificador/industrial/
├── MainActivity.kt                 Punto de entrada
├── AplicacionIdentificador.kt      Contenedor de dependencias
├── navegacion/
│   ├── Rutas.kt                    Rutas de las 10 pantallas
│   └── NavegacionApp.kt            Grafo de navegacion
├── datos/
│   ├── modelo/                     Material, Ubicacion, Categoria, Usuario...
│   ├── local/                      Room: entidades, DAO, base y convertidores
│   ├── CatalogoInicial.kt          Los 26 materiales de ejemplo
│   ├── Claves.kt                   Hash y verificacion de contrasenas
│   ├── RepositorioMateriales.kt    Unico acceso al catalogo desde la interfaz
│   └── RepositorioUsuarios.kt      Autenticacion
├── sesion/
│   └── SesionViewModel.kt          Login, usuario activo y roles
└── ui/
    ├── Fabricas.kt                 Inyeccion de repositorios en los ViewModel
    ├── theme/                      Colores, tipografia y tema
    ├── componentes/                Componentes reutilizables
    └── pantallas/                  Las 10 pantallas y sus ViewModel
```

La interfaz nunca habla con los DAO directamente, siempre pasa por un
repositorio. Esa separacion es lo que permitiria cambiar el origen de los datos
por Supabase sin tocar una sola pantalla.
