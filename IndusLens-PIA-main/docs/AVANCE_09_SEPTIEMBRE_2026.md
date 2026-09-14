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
- Camara CameraX y seleccion de imagen desde el telefono.
- OCR local para buscar numeros de parte.
- Modelo EfficientNet-Lite incluido dentro de la app para reconocer tipos de
  objetos sin enviar la fotografia a Internet.
- Resultado destacado como **La IA reconoce: Martillo** cuando el modelo
  devuelve la clase `hammer`.
- MobileNet y similitud coseno para aprender piezas concretas del catalogo.

## Guion corto de demostracion

1. Entrar con `operador / 1234` para demostrar el acceso sin Internet.
2. Cerrar y abrir la app para mostrar que la sesion se conserva.
3. Ir a **Identificar pieza** y fotografiar un martillo centrado, ocupando la
   mayor parte del marco.
4. Mostrar la pantalla de analisis y el resultado general reconocido por IA.
5. Si Firebase ya esta conectado, crear una cuenta con correo real, abrir el
   enlace recibido y demostrar que antes de verificar no permite entrar.

## Condicion para que el martillo salga claro

Usar fondo sencillo, buena luz y encuadrar cabeza y mango completos. El modelo
es general y devuelve confianza, no inventa un numero de parte. Para reconocer
un martillo especifico del almacen, se agrega al catalogo y se le ensenan
varias vistas desde su ficha.
