# Colaborar en IndusLens

`main` debe conservar siempre una version compilable y demostrable. Cada cambio
se desarrolla en una rama corta y se integra mediante Pull Request.

## Flujo de trabajo

1. Actualiza `main` antes de comenzar.
2. Crea una rama que identifique el tipo, el modulo y la actividad.
3. Realiza cambios pequenos y con un solo objetivo.
4. Comprueba `./gradlew :app:assembleDebug`.
5. Abre una Pull Request y solicita la revision de otro integrante.
6. Integra el cambio solo cuando Android CI termine correctamente.

## Nombres de ramas

Formato: `<tipo>/<modulo>-<actividad>-<descripcion>`.

Ejemplos:

- `feature/ia-35-integrar-ocr`
- `feature/datos-26-esquema-room`
- `fix/camara-permiso-denegado`
- `docs/proyecto-manual-usuario`

Tipos permitidos: `feature`, `fix`, `docs`, `test`, `build` y `chore`.

Modulos principales: `ui`, `navegacion`, `sesion`, `datos`, `ia`, `camara` y
`ci`. El modulo aparece en el nombre de la rama; no se mantienen ramas de
modulo permanentes, porque acumulan cambios y dificultan la integracion.

## Convencion de commits

Formato: `<tipo>(<modulo>): <descripcion breve>`.

Ejemplos:

- `feat(ia): integrar lectura OCR del numero de parte`
- `fix(camara): manejar permiso rechazado`
- `docs(proyecto): actualizar guia de instalacion`
- `build(ci): compilar Android en cada pull request`

No se deben subir rutas locales, contrasenas, APK, bases de datos generadas ni
archivos de Android Studio. `local.properties` pertenece exclusivamente a cada
computadora y esta excluido mediante `.gitignore`.
