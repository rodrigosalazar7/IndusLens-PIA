# Diseno reutilizable - actividad 13

Se conserva la identidad de IndusLens: isotipo industrial, azul profundo
`#0E3B5C`, azul `#2E86C1` y ambar `#E8A33D`. El fondo de la aplicacion sigue
siendo oscuro; el informe institucional se entrega sobre fondo blanco.

## Una fuente por decision visual

| Archivo | Responsabilidad |
|---|---|
| `ui/theme/Color.kt` | Paleta de IndusLens |
| `ui/theme/Type.kt` | Jerarquia tipografica; sans del sistema y codigos monoespaciados |
| `ui/theme/Shape.kt` | Cinco escalas de esquinas: 4, 8, 12, 16 y 28 dp |
| `ui/theme/Dimensiones.kt` | Espacios comunes; altura minima de controles de 52 dp |
| `ui/theme/Theme.kt` | Conecta colores, tipografia y formas a MaterialTheme |
| `ui/componentes/Controles.kt` | BotonPrincipal, BotonSecundario y CampoTexto |
| `ui/componentes/Componentes.kt` | PantallaBase, TarjetaAccion e Insignia |

## Cambios aplicados

- Login, busqueda y edicion usan `CampoTexto`, incluidos teclado, contrasena,
  iconos, texto de ayuda y estado de error.
- Los botones rectangulares usan `BotonPrincipal` o `BotonSecundario`.
- Login y guardado reutilizan el estado `cargando`: deshabilita la accion y
  presenta un indicador accesible para evitar envios repetidos.
- Las alturas de 50/52 dp pasan a minimas; el contenido puede crecer si la
  persona aumenta el tamano del texto.
- El espacio del error de login tambien puede crecer; no queda limitado a 34 dp.
- Los iconos de las barras del sistema usan contraste claro sobre el fondo oscuro.
- Tarjetas, fotos, etiquetas y agrupaciones obtienen sus esquinas del tema.
- Se unifican los margenes y separaciones comunes. Medidas de fotos, dibujo del
  plano y tamanos especificos del visor no son espaciado de interfaz.

El disparador circular de la camara conserva un `Button` especializado con
`CircleShape`; no debe convertirse en un boton rectangular. Los botones de
texto, iconos y chips mantienen sus componentes Material correspondientes.

## Revisar y extender

La galeria `app/src/debug/.../ui/componentes/ControlesPreview.kt` permite revisar
controles normales, deshabilitados, cargando y con error, a escala de fuente
normal y 1.5 en Android Studio. Solo existe en debug y no agrega una ruta a la app.

Las pruebas `ControlesTest` verifican altura minima, crecimiento con texto grande,
accion habilitada, bloqueo durante carga y transmision de entrada del campo.
Esto no reemplaza una auditoria completa de accesibilidad o de todas las pantallas.

Al crear otra pantalla, usar `PantallaBase`, los controles compartidos y
`MaterialTheme`. No copiar otra implementacion de botones/campos ni agregar
radios de esquina locales para casos que ya cubra el tema.
