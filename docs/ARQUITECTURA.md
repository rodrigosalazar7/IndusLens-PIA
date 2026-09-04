# Arquitectura de IndusLens

Documento de evidencia para la actividad 11 del cronograma: creacion del
proyecto con estructura de paquetes por capas.

## Modulo Android

La aplicacion se mantiene en un unico modulo `app`, suficiente para el alcance
del proyecto de semestre. Dentro del paquete
`com.identificador.industrial` las responsabilidades estan separadas asi:

```text
com.identificador.industrial/
├── MainActivity.kt
├── AplicacionIdentificador.kt
├── datos/
│   ├── modelo/        Entidades y tipos del negocio
│   ├── local/         Room, DAO, convertidores y migraciones
│   └── Repositorio*   Acceso unico a los datos
├── ia/                OCR, embeddings, similitud y procesamiento de fotos
├── navegacion/        Rutas y grafo de Navigation Compose
├── sesion/            Usuario activo, autenticacion y roles
└── ui/
    ├── componentes/   Componentes reutilizables
    ├── pantallas/     Pantallas y ViewModel
    └── theme/         Identidad visual de IndusLens
```

## Flujo de dependencias

```text
Pantalla Compose -> ViewModel -> Repositorio -> DAO -> Room/SQLite
Camara o archivo -> procesamiento IA -> Repositorio -> resultado en pantalla
```

Las pantallas no consultan los DAO directamente. Esta separacion permite
probar, mantener o sustituir el origen de los datos sin reescribir la interfaz.

## Archivos de configuracion

- `settings.gradle.kts`: declara el proyecto `IndusLens` y el modulo `app`.
- `build.gradle.kts`: configuracion comun de plugins.
- `app/build.gradle.kts`: SDK, variantes y dependencias Android.
- `gradle/libs.versions.toml`: catalogo central de versiones.
- `gradle/wrapper/`: version reproducible de Gradle para todo el equipo.
