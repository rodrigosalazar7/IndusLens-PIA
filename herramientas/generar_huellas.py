#!/usr/bin/env python3
"""
Genera las huellas visuales del catalogo a partir de fotografias.

Para que sirve
--------------
La aplicacion puede aprender piezas una a una desde el telefono, pero eso no
escala cuando hay que dar de alta un catalogo entero. Este script hace el
trabajo en bloque: lee una carpeta de fotografias, calcula el vector de cada
pieza y deja un archivo que la app carga la primera vez que se instala.

Requisitos imprescindibles
---------------------------
1. Se usa EXACTAMENTE el mismo modelo que la aplicacion Android
   (mobilenet_v3_small.tflite, el que esta en app/src/main/assets).

2. Se aplica EXACTAMENTE el mismo recorte central que usa la app antes de
   calcular el vector (ver `Fotos.kt`, `recortarCentro` con
   `PROPORCION_MARCO = 0.7f`): un cuadrado centrado cuyo lado es el 70% del
   lado menor de la imagen.

Ninguno de los dos puntos es un detalle menor. Dos modelos distintos
producen vectores en espacios distintos, y lo mismo pasa si una de las dos
fuentes recorta la imagen y la otra no: el enrolamiento hecho desde la app
siempre usa la foto recortada al marco guia, asi que una huella calculada
sobre la foto completa no es comparable con lo que la app calcula al
identificar una pieza, aunque el modelo sea el mismo.

Por eso el campo "modelo" que se escribe en el JSON no es el nombre pelado
del modelo, sino el mismo identificador que usa la app
(`EmbeddingMaterial.MODELO_ACTUAL`, hoy "mobilenet_v3_small_recorte70"): si
algun dia cambia el modelo o el recorte en Android, ese identificador
tambien debe actualizarse aqui, y las huellas viejas quedan ignoradas solas
por la app en vez de compararse por error contra un espacio distinto.

Como se usa
-----------
    pip install mediapipe pillow

    python generar_huellas.py --fotos ./fotos_catalogo

Las fotografias deben llamarse como la clave del material:

    fotos_catalogo/
        M-001.jpg      -> rodamiento 6205-2RS1
        M-002.jpg      -> rodamiento 6203-2Z
        M-016.png      -> electrovalvula SY5120

El resultado se escribe en app/src/main/assets/huellas_iniciales.json y la
aplicacion lo carga sola al crear la base de datos.

Consejos para las fotografias
-----------------------------
- Fondo liso y de color distinto al de la pieza.
- Toda la pieza dentro del encuadre, sin recortes.
- Luz difusa; los reflejos fuertes sobre metal confunden al modelo.
- Una sola pieza por fotografia.
"""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

EXTENSIONES = {".jpg", ".jpeg", ".png", ".bmp", ".webp"}

# Nombre del archivo .tflite en assets (no cambia aunque cambie el recorte).
NOMBRE_ARCHIVO_MODELO = "mobilenet_v3_small"

# Identificador que se escribe en el JSON y que la app compara contra
# EmbeddingMaterial.MODELO_ACTUAL. Debe coincidir siempre con ese valor.
IDENTIFICADOR_HUELLA = "mobilenet_v3_small_recorte70"

# Debe coincidir con Fotos.PROPORCION_MARCO en la app Android.
PROPORCION_MARCO = 0.7


def rutas_por_defecto() -> tuple[Path, Path]:
    """Deduce donde estan el modelo y la carpeta de assets del proyecto."""
    raiz = Path(__file__).resolve().parent.parent
    assets = raiz / "app" / "src" / "main" / "assets"
    return assets / f"{NOMBRE_ARCHIVO_MODELO}.tflite", assets / "huellas_iniciales.json"


def construir_embebedor(ruta_modelo: Path):
    try:
        from mediapipe.tasks import python as mp_python
        from mediapipe.tasks.python import vision
    except ImportError:
        sys.exit(
            "Falta MediaPipe. Instalalo con:\n\n    pip install mediapipe\n"
        )

    opciones = vision.ImageEmbedderOptions(
        base_options=mp_python.BaseOptions(model_asset_path=str(ruta_modelo)),
        # Los mismos ajustes que en Android. Si estos dos no coinciden, los
        # vectores no son comparables aunque el modelo sea el mismo.
        l2_normalize=True,
        quantize=False,
    )
    return vision.ImageEmbedder.create_from_options(opciones)


def recortar_centro(imagen_pil, proporcion: float):
    """Replica Fotos.recortarCentro: cuadrado centrado, lado = min(w,h)*proporcion."""
    ancho, alto = imagen_pil.size
    lado = max(1, int(min(ancho, alto) * proporcion))
    if lado >= ancho and lado >= alto:
        return imagen_pil
    x = (ancho - lado) // 2
    y = (alto - lado) // 2
    return imagen_pil.crop((x, y, x + lado, y + lado))


def calcular(embebedor, ruta_imagen: Path) -> list[float]:
    import numpy as np
    import mediapipe as mp
    from PIL import Image, ImageOps

    with Image.open(ruta_imagen) as bruta:
        # Respeta la orientacion EXIF, igual que hace la app con las fotos
        # elegidas desde la galeria.
        derecha = ImageOps.exif_transpose(bruta).convert("RGB")
        recortada = recortar_centro(derecha, PROPORCION_MARCO)
        arreglo = np.asarray(recortada)

    imagen = mp.Image(image_format=mp.ImageFormat.SRGB, data=arreglo)
    resultado = embebedor.embed(imagen)
    if not resultado.embeddings:
        raise RuntimeError("el modelo no devolvio ningun vector")
    return [float(v) for v in resultado.embeddings[0].embedding]


def main() -> int:
    modelo_def, salida_def = rutas_por_defecto()

    parser = argparse.ArgumentParser(
        description="Calcula las huellas visuales del catalogo de materiales."
    )
    parser.add_argument(
        "--fotos", required=True, type=Path,
        help="carpeta con las fotografias, nombradas como la clave del material",
    )
    parser.add_argument("--modelo", type=Path, default=modelo_def)
    parser.add_argument("--salida", type=Path, default=salida_def)
    args = parser.parse_args()

    if not args.modelo.exists():
        sys.exit(f"No se encontro el modelo en {args.modelo}")
    if not args.fotos.is_dir():
        sys.exit(f"No se encontro la carpeta de fotografias {args.fotos}")

    imagenes = sorted(
        p for p in args.fotos.iterdir() if p.suffix.lower() in EXTENSIONES
    )
    if not imagenes:
        sys.exit(f"No hay imagenes en {args.fotos}")

    print(f"Modelo    : {args.modelo.name}  (etiqueta: {IDENTIFICADOR_HUELLA})")
    print(f"Recorte   : cuadrado centrado al {int(PROPORCION_MARCO * 100)}%")
    print(f"Imagenes  : {len(imagenes)}")
    print()

    embebedor = construir_embebedor(args.modelo)
    huellas, fallos = [], []

    for imagen in imagenes:
        material_id = imagen.stem.strip()
        try:
            vector = calcular(embebedor, imagen)
        except Exception as e:
            fallos.append((imagen.name, str(e)))
            print(f"  [fallo] {imagen.name}: {e}")
            continue

        huellas.append(
            {"materialId": material_id, "modelo": IDENTIFICADOR_HUELLA, "vector": vector}
        )
        print(f"  [ok]    {material_id}  ({len(vector)} dimensiones)")

    if not huellas:
        sys.exit("\nNo se pudo calcular ninguna huella.")

    args.salida.parent.mkdir(parents=True, exist_ok=True)
    args.salida.write_text(
        json.dumps(huellas, separators=(",", ":")), encoding="utf-8"
    )

    print()
    print(f"Escritas {len(huellas)} huellas en {args.salida}")
    if fallos:
        print(f"{len(fallos)} imagenes fallaron.")
    print("Recompila la app para que el archivo entre en el APK.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
