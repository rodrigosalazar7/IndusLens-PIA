# Verificacion de actividades 11, 12, 13 y 14

Proyecto: IndusLens. Rama local: `feature/app-11-14-base-y-diseno`.
Verificacion realizada el 9 de septiembre de 2026.

## Resultado por actividad

| Actividad | Cambios implementados | Evidencia |
|---|---|---|
| 11 - Estructura por capas | Rol pasa de sesion a datos/modelo; referencias y arquitectura actualizadas | Rol.kt, SesionViewModel.kt, Convertidores.kt, ARQUITECTURA.md |
| 12 - Configuracion Android | minSdk 26 conservado; debug y release documentados; dependencias de pruebas alineadas | app/build.gradle.kts, libs.versions.toml, ENTREGA_ANDROID.md, APK generados |
| 13 - Sistema de diseno | Botones/campos compartidos, formas y espaciados del tema, carga reutilizada, alturas minimas e iconos del sistema con contraste | Controles.kt, Shape.kt, Dimensiones.kt, ControlesPreview.kt, DISENO.md y capturas |
| 14 - Modelo Usuario | Normalizacion comun, rechazo de campos vacios y duplicados, roles estrictos, migracion Room 4 a 5 sin borrar cuentas | UsuarioEntity.kt, NombreUsuario.kt, UsuarioDao.kt, IntegridadUsuarios.kt, MigracionUsuarios4a5.kt, esquema 5 y USUARIOS.md |

## Compilacion y pruebas

Compilacion final correcta:

```bash
./gradlew :app:assembleDebug :app:assembleRelease :app:testDebugUnitTest :app:assembleDebugAndroidTest --max-workers=2 --no-parallel
```

Resultado: `BUILD SUCCESSFUL`. Las pruebas unitarias reportan 8 ejecutadas,
0 fallos, 0 errores y 0 omitidas.

Se instalaron los APK de aplicacion y pruebas con `adb install -r` para mantener
los datos del emulador. Las pruebas instrumentadas se ejecutaron directamente:

```bash
adb shell am instrument -w -r com.identificador.industrial.test/androidx.test.runner.AndroidJUnitRunner
```

Resultado: `OK (12 tests)` en 13.239 segundos. Total: **20 pruebas aprobadas**.

| Grupo | Cantidad | Cobertura verificada |
|---|---|---|
| UsuarioTest | 8 | Normalizacion, idioma turco, idempotencia, vacios, constructor/copy, credenciales y roles |
| UsuariosIntegracionTest | 8 | Esquema 4 a 5 y 1 a 5, conservacion de cuentas/historial, colisiones y sufijos, triggers, rollback, siembra, autenticacion y atomicidad del DAO |
| ControlesTest | 4 | Altura minima, clic habilitado, bloqueo y anuncio de carga, texto a escala 1.5 y entrada de campos |

Se corrigieron dos incompatibilidades de pruebas detectadas durante la ejecucion:
Espresso 3.7.0 evita la API de entrada eliminada en Android reciente, y el BOM de
kotlinx.serialization 1.8.1 alinea el runtime de la app con los serializadores
utilizados por Room Testing. No se deshabilitaron pruebas para obtener el resultado.

## Artefactos y evidencia local

- `app/build/outputs/verificacion-11-14/compilacion-final.txt`: registro de compilacion.
- `app/build/outputs/verificacion-11-14/pruebas-android.txt`: resultados del emulador.
- `app/build/test-results/testDebugUnitTest/TEST-com.identificador.industrial.datos.UsuarioTest.xml`: resultados unitarios.
- `app/build/outputs/verificacion-11-14/login.png` y `editar.png`: capturas de la version final.
- `app/build/outputs/verificacion-11-14/respaldo-base-previa.tar`: copia local previa a actualizar, no destinada a compartirse.
- `app/build/outputs/apk/debug/`: APK instalables de depuracion.
- `app/build/outputs/apk/release/`: APK release sin firmar.

`aapt2` confirma en el APK `minSdkVersion: 26`, `targetSdkVersion: 36` y nombre
IndusLens. `apksigner` valida la firma v2 del APK debug. El release sin firmar
no supera verificacion de firma, como corresponde: falta la clave de entrega.

Los esquemas de materiales, historial_busquedas y embeddings no cambian entre
las versiones 4 y 5. Solo se modifica usuarios y se agregan sus protecciones.
La base de las pruebas tiene un nombre separado; no se borro la base de la app.

Tambien se comparo la copia real del emulador antes y despues de actualizar:
la version cambia de 4 a 5 y las filas son identicas en las cuatro tablas.
Se conservan 26 materiales, 3 usuarios y 3 registros de historial; embeddings
estaba vacia y permanece vacia. La base actualizada contiene ambos triggers
de validacion de usuarios. Esta comprobacion no publica las credenciales.

## Revision visual y limites

Se revisaron login, menu de administrador, listado de 26 materiales y formulario
de edicion en el AVD Pixel_8 ARM64 disponible (imagen Android 37.2 beta 3). Se
comprobo el acceso usando ADMIN y la clave de demostracion. La fuente a escala
1.5 se cubre ademas en las pruebas de controles y las previews del diseno.

No se cambio ni certifico la precision del reconocimiento, no se repitieron
pruebas de camara y no se verifico esta version en un dispositivo fisico API 26.
La prueba del AVD no sustituye esas comprobaciones de entrega.

Los cambios permanecen locales, sin commit ni push de esta rama. La CI remota
no se ejecuto para este reporte. La firma release y la aprobacion del equipo
quedan pendientes; no se agregaron funciones fuera de las cuatro actividades.
