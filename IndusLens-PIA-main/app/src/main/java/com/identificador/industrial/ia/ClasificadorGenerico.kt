package com.identificador.industrial.ia

import android.content.Context
import android.graphics.Bitmap
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.imageclassifier.ImageClassifier
import com.identificador.industrial.datos.modelo.Categoria

/** Lo que el modelo generico cree ver en la foto. */
data class Pista(
    val etiqueta: String,
    val confianza: Float,
    /** Familia del catalogo a la que apunta, si se pudo deducir. */
    val categoria: Categoria?
)

/**
 * Reconocimiento generico de la imagen, para cuando la pieza no se ha visto
 * nunca antes.
 *
 * Que quede claro lo que esto es y lo que no es:
 *
 * NO puede decir "esto es un DIN 933 M10x40 grado 8.8". Esa informacion no
 * esta en la fotografia: dos tornillos identicos a la vista pueden ser de
 * grados y metricas distintas. No lo saca esta app ni ninguna otra.
 *
 * SI puede decir "esto parece un tornillo", que ya permite acotar el catalogo
 * a la familia correcta y que la persona elija entre unas pocas piezas en vez
 * de entre cientos.
 *
 * Sobre el modelo: se usa EfficientNet-Lite entrenado con ImageNet, cuyo
 * vocabulario de mil categorias incluye herrajes y herramientas reales
 * ("screw", "nail", "chain", "hammer", "power drill"). Se probo antes el
 * etiquetador generico de ML Kit y no servia: su vocabulario es domestico y
 * ante un tornillo respondia "habitacion" o "telefono movil", porque esas son
 * las palabras que conoce.
 *
 * Aun asi, sigue siendo una pista y no una afirmacion. Con tornilleria,
 * cadenas y herramientas acierta razonablemente; con rodamientos, retenes o
 * contactores se pierde, porque son objetos que casi no aparecen en las
 * fotografias con las que se entreno.
 */
class ClasificadorGenerico(contexto: Context) {

    private val motor: ImageClassifier = ImageClassifier.createFromOptions(
        contexto,
        ImageClassifier.ImageClassifierOptions.builder()
            .setBaseOptions(
                BaseOptions.builder()
                    .setModelAssetPath(ARCHIVO_MODELO)
                    .build()
            )
            .setMaxResults(MAXIMO_PISTAS)
            .setScoreThreshold(UMBRAL_MINIMO)
            .setRunningMode(RunningMode.IMAGE)
            .build()
    )

    /**
     * Recibe la imagen YA recortada al marco guia. Si se le pasara la foto
     * completa describiria el escenario en lugar de la pieza.
     */
    fun analizar(imagen: Bitmap): List<Pista> {
        val resultado = motor.classify(BitmapImageBuilder(imagen).build())
        val clasificaciones = resultado.classificationResult().classifications()
        if (clasificaciones.isEmpty()) return emptyList()

        return clasificaciones[0].categories()
            .map { categoria ->
                val nombre = categoria.categoryName().orEmpty()
                Pista(
                    etiqueta = enCastellano(nombre),
                    confianza = categoria.score(),
                    categoria = categoriaDe(nombre)
                )
            }
            .filter { it.etiqueta.isNotBlank() }
            .take(MAXIMO_PISTAS)
    }

    fun cerrar() {
        motor.close()
    }

    companion object {

        const val ARCHIVO_MODELO = "efficientnet_lite0.tflite"

        /**
         * Por debajo de esto, el modelo esta adivinando y mas vale callar.
         *
         * Se subio desde 0.08 tras ver el resultado con una tuerca hexagonal:
         * respondia "lupa 19%", "interruptor 17%", "foco 10%". ImageNet no
         * tiene ninguna clase de tuerca, asi que buscaba el objeto redondo con
         * agujero que mas se le pareciera. Esos numeros tan bajos son el
         * modelo diciendo "no se", y presentarlos como pista solo confunde.
         *
         * Un tornillo real, en cambio, se reconoce al 94%.
         */
        private const val UMBRAL_MINIMO = 0.30f
        private const val MAXIMO_PISTAS = 4

        /**
         * Traduccion de las etiquetas del modelo a las familias del catalogo.
         *
         * Deliberadamente corta y conservadora: se prefiere no sugerir ninguna
         * familia antes que sugerir la equivocada, porque una pista erronea
         * manda al trabajador a buscar entre las piezas que no son.
         */
        private val CORRESPONDENCIAS: List<Pair<String, Categoria>> = listOf(
            "screw" to Categoria.TORNILLERIA,
            "nail" to Categoria.TORNILLERIA,
            "bolt" to Categoria.TORNILLERIA,
            "nut" to Categoria.TORNILLERIA,
            "thimble" to Categoria.TORNILLERIA,
            "chain" to Categoria.TRANSMISION,
            "gear" to Categoria.TRANSMISION,
            "cog" to Categoria.TRANSMISION,
            "belt" to Categoria.TRANSMISION,
            "pulley" to Categoria.TRANSMISION,
            "wheel" to Categoria.RODAMIENTO,
            "bearing" to Categoria.RODAMIENTO,
            "washer" to Categoria.SELLO,
            "gasket" to Categoria.SELLO,
            "rubber" to Categoria.SELLO,
            "hammer" to Categoria.HERRAMIENTA,
            "screwdriver" to Categoria.HERRAMIENTA,
            "power drill" to Categoria.HERRAMIENTA,
            "plunger" to Categoria.HERRAMIENTA,
            "hatchet" to Categoria.HERRAMIENTA,
            "switch" to Categoria.ELECTRICO,
            "socket" to Categoria.ELECTRICO,
            "plug" to Categoria.ELECTRICO,
            "solenoid" to Categoria.ELECTRICO,
            "filter" to Categoria.FILTRO,
            "strainer" to Categoria.FILTRO,
            "valve" to Categoria.NEUMATICA,
            "piston" to Categoria.NEUMATICA,
            "hose" to Categoria.NEUMATICA,
            "pipe" to Categoria.NEUMATICA,
            "syringe" to Categoria.NEUMATICA,
            "oil" to Categoria.LUBRICANTE,
            "grease" to Categoria.LUBRICANTE
        )

        /**
         * Nombres en castellano de las etiquetas de ImageNet que aparecen con
         * piezas industriales. Las que no estan aqui se muestran tal cual: es
         * preferible una palabra en ingles a ocultarle informacion al usuario.
         */
        private val TRADUCCIONES: Map<String, String> = mapOf(
            "screw" to "tornillo",
            "nail" to "clavo o tornillo",
            "nut" to "tuerca",
            "chain" to "cadena",
            "hook" to "gancho",
            "buckle" to "hebilla",
            "hammer" to "martillo",
            "screwdriver" to "destornillador",
            "power drill" to "taladro",
            "plunger" to "desatascador",
            "padlock" to "candado",
            "combination lock" to "candado",
            "switch" to "interruptor",
            "car wheel" to "rueda",
            "paddlewheel" to "rueda de paletas",
            "gasmask" to "mascarilla",
            "syringe" to "jeringa",
            "spring" to "muelle",
            "pill bottle" to "bote",
            "lighter" to "encendedor",
            "can opener" to "abrelatas",
            "corkscrew" to "sacacorchos",
            "wrench" to "llave",
            "matchstick" to "cerilla",
            "pencil sharpener" to "sacapuntas",
            "rubber eraser" to "goma",
            "whistle" to "silbato",
            "safety pin" to "imperdible"
        )

        fun categoriaDe(etiqueta: String): Categoria? {
            val texto = etiqueta.lowercase()
            return CORRESPONDENCIAS.firstOrNull { (clave, _) -> texto.contains(clave) }?.second
        }

        fun enCastellano(etiqueta: String): String {
            val limpia = etiqueta.lowercase().trim()
            TRADUCCIONES[limpia]?.let { return it }
            // Las etiquetas de ImageNet suelen traer sinonimos separados por
            // comas ("hook, claw"); se prueba con el primero.
            val primera = limpia.substringBefore(',').trim()
            return TRADUCCIONES[primera] ?: primera
        }
    }
}
