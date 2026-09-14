package com.identificador.industrial.datos

import com.identificador.industrial.datos.modelo.Categoria
import com.identificador.industrial.datos.modelo.Material
import com.identificador.industrial.datos.modelo.Ubicacion
import com.identificador.industrial.datos.modelo.UsuarioEntity
import com.identificador.industrial.datos.modelo.Rol

/**
 * Kit de demostracion para instalaciones nuevas y actualizaciones.
 * Solo se agregan piezas ausentes: se respetan IDs, codigos, existencias,
 * ubicaciones, bajas y referencias de cualquier registro ya guardado.
 *
 * El catalogo de materiales se dejaba vacio a proposito, para que la
 * demostracion en clase mostrara en vivo como se da de alta una pieza que
 * la app no conoce. Ahora trae sembrados 4 tornillos de prueba (fotografiados
 * para el examen), con el fin de que la identificacion por parecido visual
 * funcione desde la primera instalacion, sin tener que ensenarlos a mano en
 * cada telefono. Sus huellas visuales viven en
 * `app/src/main/assets/huellas_iniciales.json`, generadas con
 * `herramientas/generar_huellas.py` a partir de esas mismas fotos. Una de
 * esas mismas fotos de cada tornillo tambien se guarda sin procesar en
 * `app/src/main/assets/fotos_catalogo/`, y se referencia en `fotoReferencia`,
 * para que la ficha del material y la pantalla de resultado puedan
 * mostrarla: asi la persona compara a simple vista si la pieza que tiene
 * enfrente es la misma que senala la app.
 *
 * Los usuarios de prueba se siguen sembrando siempre, porque hacen falta
 * para entrar a la app.
 */
object CatalogoInicial {

    val materiales: List<Material> = listOf(
        Material(
            id = "M-027",
            nombre = "Tuerca moleteada con brida ranurada",
            descripcion = "Cuerpo cilindrico moleteado con cabeza metalica " +
                "ranurada y rosca interna, para sujecion manual.",
            numeroParte = "TOR-001",
            fabricante = "Catalogo propio",
            categoria = Categoria.TORNILLERIA,
            unidadMedida = "Pieza",
            existencia = 20,
            existenciaMinima = 5,
            ubicacion = Ubicacion(
                almacen = "A",
                pasillo = "01",
                rack = "A",
                nivel = 1,
                posicion = 1
            ),
            fotoReferencia = "M-027.jpg"
        ),
        Material(
            id = "M-028",
            nombre = "Tornillo autorroscante cabeza ovalada",
            descripcion = "Tornillo de cabeza ovalada pequena y rosca " +
                "completa tipo autorroscante, cuerpo corto.",
            numeroParte = "TOR-002",
            fabricante = "Catalogo propio",
            categoria = Categoria.TORNILLERIA,
            unidadMedida = "Pieza",
            existencia = 30,
            existenciaMinima = 10,
            ubicacion = Ubicacion(
                almacen = "A",
                pasillo = "01",
                rack = "A",
                nivel = 1,
                posicion = 2
            ),
            fotoReferencia = "M-028.jpg"
        ),
        Material(
            id = "M-029",
            nombre = "Tornillo de hombro con cabeza plana",
            descripcion = "Tornillo con cabeza plana ovalada, cuerpo liso " +
                "sin rosca (hombro espaciador) y rosca corta de menor " +
                "diametro en la punta.",
            numeroParte = "TOR-003",
            fabricante = "Catalogo propio",
            categoria = Categoria.TORNILLERIA,
            unidadMedida = "Pieza",
            existencia = 15,
            existenciaMinima = 5,
            ubicacion = Ubicacion(
                almacen = "A",
                pasillo = "01",
                rack = "A",
                nivel = 1,
                posicion = 3
            ),
            fotoReferencia = "M-029.jpg"
        ),
        Material(
            id = "M-030",
            nombre = "Tornillo metrico cabeza ranurada",
            descripcion = "Tornillo pequeno de rosca metrica completa, " +
                "cabeza cilindrica achaflanada con ranura para " +
                "desatornillador plano.",
            numeroParte = "TOR-004",
            fabricante = "Catalogo propio",
            categoria = Categoria.TORNILLERIA,
            unidadMedida = "Pieza",
            existencia = 40,
            existenciaMinima = 10,
            ubicacion = Ubicacion(
                almacen = "A",
                pasillo = "01",
                rack = "A",
                nivel = 1,
                posicion = 4
            ),
            fotoReferencia = "M-030.jpg"
        )
    )

    /**
     * Usuarios iniciales. Las contrasenas se cifran al sembrar, nunca se
     * guardan tal cual. Son las mismas credenciales de prueba de la fase 1.
     */
    fun usuarios(): List<UsuarioEntity> = listOf(
        crearUsuario("u1", "operador", "Juan Ramirez", Rol.OPERADOR, "1234"),
        crearUsuario("u2", "almacen", "Maria Lopez", Rol.ALMACENISTA, "1234"),
        crearUsuario("u3", "admin", "Supervisor", Rol.ADMINISTRADOR, "admin")
    )

    private fun crearUsuario(
        id: String,
        usuario: String,
        nombre: String,
        rol: Rol,
        claveEnClaro: String
    ): UsuarioEntity {
        val sal = Claves.generarSal()
        return UsuarioEntity.crear(
            id = id,
            usuario = usuario,
            nombre = nombre,
            rol = rol,
            hashClave = Claves.hash(claveEnClaro, sal),
            sal = sal
        )
    }
}
