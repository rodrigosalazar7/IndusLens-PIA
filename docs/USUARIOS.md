# Integridad de usuarios - actividad 14

## Modelo y reglas

`UsuarioEntity` conserva identificador, usuario, nombre, rol, hash de contrasena,
sal y estado activo. `Rol` vive ahora en `datos/modelo`.

- Crear cuentas con `UsuarioEntity.crear(...)`: elimina espacios exteriores
  del usuario/nombre y convierte el usuario a minusculas con `Locale.ROOT`.
- La autenticacion usa la misma normalizacion. ` ADMIN ` y `admin` buscan la
  misma cuenta, sin modificar la contrasena escrita.
- El constructor y `copy` rechazan identificadores, usuarios, nombres y
  credenciales vacios. El usuario debe estar ya normalizado.
- El indice unico y `COLLATE NOCASE` protegen la unicidad. SQLite NOCASE cubre
  el caso ASCII; la normalizacion de Kotlin sigue siendo la entrada obligatoria
  para nombres Unicode. No se promete equivalencia entre acentos o alfabetos.
- Los disparadores de SQLite validan insercion y actualizacion: campos
  obligatorios no vacios, usuario sin espacios exteriores ASCII ni mayusculas
  ASCII, rol admitido y estado 0/1. La validacion Kotlin cubre ademas el espacio
  en blanco reconocido por `isBlank`/`trim`.
- El DAO usa `ABORT`: un duplicado produce error, no sustituye la cuenta previa.
  Un lote con conflicto se revierte completo.
- Un rol desconocido causa error; ya no se convierte silenciosamente en operador.

## Migracion 4 a 5

La version de Room sube a 5 y se conserva el camino de migraciones desde la 1.
No se usa migracion destructiva.

1. Leer y validar las cuentas antes de reemplazar la tabla.
2. Normalizar usuarios y nombres. Conservar IDs, roles, hashes, sales y estado.
3. Si hay colisiones antiguas, conservar el nombre de la cuenta que ya estaba
   normalizada. Las demas reciben sufijos `-2`, `-3`, etc.; se evitan nombres que
   otra cuenta ya tenia. El orden por ID hace reproducible la asignacion.
4. Crear la tabla nueva, copiar todas las cuentas, crear indice unico y triggers.
5. Room valida el esquema exportado y confirma la transaccion.

Ejemplo: si existen `Admin`, `admin` y `admin-2`, se conservan `admin` y `admin-2`;
`Admin` pasa a `admin-3`. Su ID y su contrasena no cambian. El equipo debe informar
el nuevo nombre a una persona afectada antes de desplegar una base con colisiones.
No se fusionan cuentas ni se pierden referencias de historial, que utilizan el ID.

Si hay campos historicos obligatorios vacios o un rol/estado invalido, se aborta
la migracion. La base anterior se conserva; debe revisarse y corregirse con copia
previa. No desinstalar ni borrar datos como solucion automatica.

## Pruebas y alcance

`UsuarioTest` valida normalizacion, independencia del idioma, idempotencia,
campos vacios, constructor/copy, credenciales y conversion de roles.
`UsuariosIntegracionTest` usa SQLite/Room reales en Android: migracion, historial,
colisiones, constraints, rollback, actualizacion desde la version 1, instalacion
nueva, autenticacion y atomicidad del DAO.

No se agrega una pantalla de registro/administracion de usuarios ni sincronizacion
en nube. La app sigue siendo local, con las tres cuentas de demostracion. El hash
de contrasenas existente se conserva por compatibilidad y no se certifica como
seguridad de produccion.
