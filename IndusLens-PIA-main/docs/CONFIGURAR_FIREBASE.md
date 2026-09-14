# Activar registro y verificacion por correo

IndusLens ya contiene el flujo completo de interfaz y codigo para:

- crear una cuenta con nombre, correo y contrasena;
- enviar el correo de verificacion;
- impedir el acceso mientras el correo no este confirmado;
- reenviar el enlace;
- recuperar la contrasena;
- conservar una sesion verificada entre reinicios.

Firebase necesita asociar la app a un proyecto antes de poder enviar mensajes.
La configuracion se hace una sola vez:

1. Entra a [Firebase Console](https://console.firebase.google.com/) y crea o abre
   el proyecto **IndusLens**.
2. Agrega una aplicacion Android con el paquete exacto
   `com.identificador.industrial`.
3. Descarga `google-services.json` y colocalo en `app/google-services.json`.
4. En **Authentication > Sign-in method**, habilita **Correo/Contrasena**.
5. En **Authentication > Templates**, personaliza en espanol el mensaje de
   verificacion si lo desean.
6. Sincroniza Gradle y ejecuta la app.

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
