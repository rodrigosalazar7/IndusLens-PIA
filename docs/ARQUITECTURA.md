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
│   ├── modelo/        Entidades y tipos del negocio, incluido Rol
│   ├── local/         Room, DAO, convertidores y migraciones
│   └── Repositorio*   Acceso unico a los datos
├── ia/                OCR, embeddings, similitud y procesamiento de fotos
├── navegacion/        Rutas y grafo de Navigation Compose
├── sesion/            Estado del usuario activo y acceso a la autenticacion
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

`Rol` pertenece a `datos/modelo/Rol.kt`: lo comparten la entidad de usuario,
los convertidores de Room, el catalogo inicial y la sesion. Los datos ya no
dependen de `SesionViewModel` para declarar este tipo. Se conservan los valores
`OPERADOR`, `ALMACENISTA` y `ADMINISTRADOR`, por lo que mover el archivo no cambia
los permisos ni los valores almacenados.

El proyecto conserva un solo modulo, adecuado para la entrega de semestre.
Este ajuste no pretende una arquitectura de dominio completamente independiente:
`RepositorioUsuarios` todavia devuelve el objeto de sesion `Usuario`.

- [Diseno compartido](DISENO.md): controles, formas, tipografia y espaciados.
- [Integridad de usuarios](USUARIOS.md): reglas y migracion de la version 4 a 5.
- [Entrega Android](ENTREGA_ANDROID.md): API 26, debug, release y pruebas.

## Archivos de configuracion

- `settings.gradle.kts`: declara el proyecto `IndusLens` y el modulo `app`.
- `build.gradle.kts`: configuracion comun de plugins.
- `app/build.gradle.kts`: SDK, variantes y dependencias Android.
- `gradle/libs.versions.toml`: catalogo central de versiones.
- `gradle/wrapper/`: version reproducible de Gradle para todo el equipo.
