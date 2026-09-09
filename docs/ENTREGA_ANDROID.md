# Entrega Android - actividad 12

## Compatibilidad acordada

Se mantiene `minSdk = 26` (Android 8.0). No se reduce a API 21 para esta entrega.
La configuracion real del modulo `app` es:

| Propiedad | Valor | Significado |
|---|---|---|
| minSdk | 26 | Version minima permitida para instalar |
| targetSdk | 36 | Version objetivo de comportamiento Android |
| compileSdk | 37 | SDK usado para compilar las dependencias actuales |
| versionName / versionCode | 1.0 / 1 | Identificacion de esta base del proyecto |
| Java/Kotlin bytecode | 17 | No es lo mismo que el JDK que ejecuta Gradle |

La ficha AF1 original menciona API 21. Este documento registra la decision de
mantener API 26; no modifica el PDF del equipo. Declarar compatibilidad minima
no equivale a haber probado todos los dispositivos desde Android 8.0.

## Preparacion del equipo

Las dependencias de verificacion quedan declaradas en el catalogo de versiones:
JUnit 4.13.2, AndroidX Test Runner 1.7.0, extension JUnit 1.3.0, Room Testing 2.8.4
y Espresso 3.7.0. El BOM de kotlinx.serialization 1.8.1 alinea la biblioteca de
ejecucion de la app y las pruebas de migracion, evitando una incompatibilidad
entre serializadores de Room y el core transitivo anterior.

Abrir la carpeta del repositorio en Android Studio y sincronizar Gradle.
`local.properties` contiene la ruta del SDK de cada computadora y no se sube a
Git. Se requieren la plataforma Android 37 y las herramientas de compilacion
que solicite el proyecto. Usar siempre el Gradle Wrapper incluido.

El archivo `gradle/gradle-daemon-jvm.properties` solicita JDK 25 para el proceso
de Gradle. El bytecode de la aplicacion sigue siendo Java/Kotlin 17. La primera
sincronizacion puede descargar el JDK y dependencias aunque el codigo funcione
sin conexion una vez instalado.

## Debug y release

| Variante | Uso | Firma | Resultado |
|---|---|---|---|
| debug | Desarrollo, pruebas y demostracion de semestre | Automatica, clave de depuracion local | APK instalable |
| release | Preparacion de distribucion | Requiere clave del equipo; no esta configurada en Git | APK sin firmar al compilar por consola |

Comandos desde la raiz del repositorio:

```bash
./gradlew :app:assembleDebug
./gradlew :app:assembleRelease
```

Salidas principales:

```text
app/build/outputs/apk/debug/app-arm64-v8a-debug.apk
app/build/outputs/apk/debug/app-armeabi-v7a-debug.apk
app/build/outputs/apk/debug/app-x86_64-debug.apk
app/build/outputs/apk/debug/app-universal-debug.apk
app/build/outputs/apk/release/app-arm64-v8a-release-unsigned.apk
```

Los otros APK release tambien se generan por arquitectura y en variante
universal. La configuracion actual no activa ofuscacion/minificacion en release.
Compilar release no significa que ya este firmado o listo para publicar.

## Elegir el APK

- `arm64-v8a`: dispositivos Android ARM de 64 bits y AVD ARM en Mac Apple Silicon.
- `armeabi-v7a`: dispositivos ARM de 32 bits compatibles con Android 8.0 o superior.
- `x86_64`: emuladores/dispositivos de arquitectura Intel o AMD compatibles.
- `universal`: incluye las tres arquitecturas; es mayor de 100 MB en esta base.

Para la demostracion se recomienda el APK debug de la arquitectura del equipo.
Consultar la arquitectura con `adb shell getprop ro.product.cpu.abi`; no elegir
x86_64 solo porque se trata de un emulador.

```bash
adb install -r app/build/outputs/apk/debug/app-arm64-v8a-debug.apk
```

`-r` actualiza una instalacion con la misma firma sin borrar sus datos. Si aparece
un error de firma incompatible, no desinstalar automaticamente: desinstalar
borra el inventario y el historial locales. Acordar antes una copia/recuperacion.
Cada computadora puede tener una clave debug distinta.

## Firma release para la entrega final

1. En Android Studio: **Build > Generate Signed App Bundle or APK > APK**.
2. El equipo designa al responsable de custodiar el keystore y su copia segura.
3. Seleccionar o crear la clave fuera del repositorio; elegir la variante release.
4. Conservar la misma clave para futuras actualizaciones y verificar la firma
   con `apksigner verify --verbose archivo.apk`.
5. Instalar y probar el APK firmado en un dispositivo de entrega.

No se creo una clave release ni se guardaron contrasenas como parte de estas
actividades. No subir keystores, archivos `.jks` ni credenciales a Git. Los APK
son artefactos de compilacion, no archivos fuente.

## Verificacion repetible

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:connectedDebugAndroidTest
./gradlew :app:assembleDebug :app:assembleRelease
```

La segunda orden necesita un dispositivo o emulador. Las pruebas usan una base
separada llamada `pruebas-integridad-usuarios.db`, nunca `identificador.db`.
Gradle puede reinstalar el paquete al ejecutar pruebas conectadas; guardar la
informacion importante y usar un emulador de pruebas, no el dispositivo de entrega.
Los informes quedan en `app/build/reports/tests/testDebugUnitTest/` y
`app/build/reports/androidTests/connected/debug/`.

La CI existente compila debug en cada PR dirigida a `main`. Una compilacion
local no demuestra que una ejecucion remota de CI haya terminado correctamente.

Referencias: [variantes de compilacion](https://developer.android.com/build/build-variants)
y [firma de aplicaciones](https://developer.android.com/studio/publish/app-signing).
