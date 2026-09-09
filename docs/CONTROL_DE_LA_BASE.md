# Control total de la base de IndusLens

Esta copia usa dos capas:

- **Firestore** es la base central bajo la cuenta de Firebase del propietario.
- **Room** es una cache local para que el catalogo y la identificacion sigan
  funcionando cuando un telefono pierde Internet.

El propietario conserva el control porque las reglas incluidas solo permiten
modificar `materiales` a perfiles con rol `ADMINISTRADOR`. Una cuenta nueva no
puede darse permisos a si misma: siempre nace como `OPERADOR`.

## Crear la base nueva

1. Abre el proyecto ya creado **IndusLens Control** (`induslens-control-rodaa`)
   en [Firebase Console](https://console.firebase.google.com/). Se creo desde
   la cuenta propietaria, sin organizacion superior, Analytics ni Gemini.
2. Agrega una app Android con el paquete `com.identificador.industrial.control` y
   coloca el archivo descargado en `app/google-services.json`.
3. Activa **Authentication > Correo/Contrasena**.
4. Crea **Firestore Database** en modo de produccion.
5. Publica `firestore.rules` y `firestore.indexes.json` con Firebase CLI, o
   copia las reglas en la pestana **Firestore > Rules** de la consola.
6. Desde IndusLens crea tu cuenta, abre el correo de verificacion e inicia
   sesion una vez. Esto crea `usuarios/{uid}` con rol `OPERADOR`.
7. En Firebase Console abre **Authentication > Users** y copia tu UID.
8. En Firestore abre `usuarios/{tu UID}` y cambia `rol` a
   `ADMINISTRADOR`. Conserva `activo` en `true`.
9. Cierra sesion y vuelve a entrar. La app mostrara Administrador y publicara
   automaticamente el catalogo inicial si la coleccion `materiales` esta vacia.

Desde ese momento, los cambios hechos en **Administracion de materiales** se
guardan primero en Firestore y llegan a los demas telefonos. Si las reglas
rechazan una escritura, la cache local tampoco se modifica, evitando que se
vean datos distintos entre dispositivos.

## Estructura de la base

### `usuarios/{uid}`

- `correo`: correo verificado.
- `nombre`: nombre mostrado en la app.
- `rol`: `OPERADOR`, `ALMACENISTA` o `ADMINISTRADOR`.
- `activo`: permite revocar acceso operativo sin borrar la cuenta.

### `materiales/{id}`

Contiene nombre, numero de parte, fabricante, categoria, existencias, minimo,
medidas, estado y ubicacion completa. El identificador conserva el formato
`M-001`, `M-002`, etcetera.

## Regla de seguridad importante

No cambies las reglas a `allow read, write: if true`. Eso convertiria el
catalogo en una base publica modificable por cualquiera. Para agregar otro
administrador, cambia su rol desde la consola usando la cuenta propietaria.
