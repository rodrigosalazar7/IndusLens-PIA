# Verificacion de actividades 09, 10 y 11

Fecha de verificacion: **4 de septiembre de 2026**.

## Actividad 09 - Entorno de desarrollo

Estado: **cumplida**.

- Android Studio instalado en macOS Apple Silicon.
- Android SDK configurado localmente.
- Plataforma `android-37.0` instalada.
- AVD `Pixel_8` creado para las pruebas.
- Camara del AVD preparada para usar la webcam de la Mac.
- Gradle Wrapper incluido en el repositorio.
- Compilacion local verificada mediante `./gradlew :app:assembleDebug`.

La ubicacion del SDK se conserva en `local.properties`. Este archivo no se
comparte porque la ruta es diferente en cada computadora.

## Actividad 10 - Flujo de trabajo en Git

Estado: **cumplida**.

- Repositorio remoto: `rodrigosalazar7/Identificador-industrial`.
- Rama estable: `main`.
- Convencion de ramas y commits documentada en `CONTRIBUTING.md`.
- Plantilla de Pull Request agregada.
- Android CI compila `:app:assembleDebug` automaticamente en cada Pull Request
  dirigido a `main`.
- `local.properties`, APK y archivos de compilacion estan excluidos.

## Actividad 11 - Estructura por capas

Estado: **cumplida**.

- Modulo Android `app` creado.
- Paquetes separados para `datos`, `ia`, `navegacion`, `sesion` y `ui`.
- Capa local con Room y DAO.
- Acceso a datos mediante repositorios.
- Interfaz con Compose y estado mediante ViewModel.
- Versiones centralizadas en `gradle/libs.versions.toml`.

La estructura detallada y su flujo de dependencias se encuentran en
`docs/ARQUITECTURA.md`.
