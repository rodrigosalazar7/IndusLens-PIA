# Avance demostrable - 9 de septiembre de 2026

Segun el cronograma, para hoy correspondia completar la actividad 19: sesion
activa, cierre de sesion y persistencia del usuario. El proyecto ahora cubre
esa actividad y adelanta funciones planeadas para finales de septiembre y
octubre.

## Terminado para mostrar hoy

- Inicio de sesion local con contrasenas protegidas y tres roles.
- Persistencia de la sesion activa sin guardar la contrasena.
- Cierre de sesion que limpia el estado local y remoto.
- Registro con correo y contrasena mediante Firebase Authentication.
- Envio, reenvio y comprobacion del correo de verificacion.
- Bloqueo de acceso para correos no verificados.
- Recuperacion de contrasena por correo.
- Proyecto Firebase propio `IndusLens Control` y base central Firestore
  preparada con reglas por rol; no comparte datos con otros proyectos.
- Camara CameraX y seleccion de imagen desde el telefono.
- OCR local para buscar numeros de parte.
- Modelo EfficientNet-Lite incluido dentro de la app para reconocer tipos de
  objetos aun cuando no haya conexion.
- Reconocimiento visual en linea con Gemini mediante Firebase AI Logic. Solo
  se envia el recorte central y unicamente despues de que la persona acepte el
  aviso dentro de la app; si falla Internet, se usa automaticamente el modelo
  local.
- Resultado destacado como **La IA reconoce: Martillo** o **Mango / fruta**
  segun el objeto visible, sin inventar marca, modelo ni numero de parte.
- MobileNet y similitud coseno para aprender piezas concretas del catalogo.

## Verificacion tecnica de hoy

- Android SDK y herramientas de compilacion instalados en esta computadora.
- Compilacion `debug` terminada correctamente el 9 de septiembre de 2026.
- Cuatro pruebas automaticas ejecutadas: 4 correctas, 0 fallos.
- APK separados para telefonos de 64 bits, telefonos antiguos, emulador y un
  APK universal.

## Guion corto de demostracion

1. Entrar con `operador / 1234` para demostrar el acceso sin Internet.
2. Cerrar y abrir la app para mostrar que la sesion se conserva.
3. Ir a **Identificar pieza** y fotografiar un martillo centrado, ocupando la
   mayor parte del marco.
4. Mostrar la pantalla de analisis y el resultado general reconocido por IA.
5. Crear una cuenta con correo real, comprobar que antes de verificar no
   permite entrar, abrir el enlace recibido y volver a la app.

## Condicion para que el martillo salga claro

Usar fondo sencillo, buena luz y encuadrar cabeza y mango completos. La IA en
linea reconoce el nombre y la familia general; no inventa un numero de parte.
Para reconocer un martillo especifico del almacen, se agrega al catalogo y se
le ensenan varias vistas desde su ficha.
