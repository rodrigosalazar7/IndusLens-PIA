# Activar registro y verificacion por correo

IndusLens ya contiene el flujo completo de interfaz y codigo para:

- crear una cuenta con nombre, correo y contrasena;
- enviar el correo de verificacion;
- impedir el acceso mientras el correo no este confirmado;
- reenviar el enlace;
- recuperar la contrasena;
- conservar una sesion verificada entre reinicios.

Firebase necesita asociar la app a un proyecto antes de poder enviar mensajes.
La configuracion de entrega quedo terminada el **9 de septiembre de 2026**:

- nombre: `IndusLens Control`;
- ID: `induslens-control-rodaa`;
- paquete Android: `com.identificador.industrial.control`.
- Authentication con Correo/Contrasena habilitado;
- Firestore `(default)` creado en `nam5`, con reglas e indices publicados;
- Firebase AI Logic habilitado con Gemini Developer API en el plan Spark;
- `app/google-services.json` instalado y validado durante la compilacion.

No pertenece ni reutiliza configuracion de ninguna otra aplicacion. Los pasos
siguientes quedan como referencia por si se crea otro proyecto en el futuro:

1. Entra a [Firebase Console](https://console.firebase.google.com/) y abre el
   proyecto **IndusLens Control**.
2. Agrega una aplicacion Android con el paquete exacto
   `com.identificador.industrial.control`.
3. Descarga `google-services.json` y colocalo en `app/google-services.json`.
4. En **Authentication > Sign-in method**, habilita **Correo/Contrasena**.
5. Crea **Firestore Database** en modo de produccion y publica las reglas del
   archivo `firestore.rules` incluido en la raiz.
6. En **Authentication > Templates**, personaliza en espanol el mensaje de
   verificacion si lo desean.
7. Sigue `docs/CONTROL_DE_LA_BASE.md` para convertir tu usuario verificado en
   el primer Administrador.
8. Sincroniza Gradle y ejecuta la app.

El archivo activa automaticamente el complemento de Google al compilar. Si no
esta presente, el proyecto sigue funcionando con las cuentas locales de
demostracion; el registro muestra una explicacion clara en vez de cerrarse.

## Prueba de aceptacion

1. Pulsa **Crear cuenta con correo**.
2. Registra una direccion real y una contrasena de al menos seis caracteres.
3. Comprueba que llegue el mensaje y que la app no permita entrar antes de
   abrir el enlace.
4. Abre el enlace, regresa a IndusLens y pulsa **Ya verifique, entrar**.
5. Cierra y vuelve a abrir la app: la sesion debe continuar activa.
6. Pulsa **Salir**: la sesion local y la de Firebase deben quedar cerradas.

Las cuentas creadas por correo reciben el rol **Operador**. Los permisos de
Administrador no deben concederse desde el telefono; se asignan desde un medio
administrativo confiable.
