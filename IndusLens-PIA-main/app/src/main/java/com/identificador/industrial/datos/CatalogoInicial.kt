package com.identificador.industrial.datos

import com.identificador.industrial.datos.modelo.Categoria
import com.identificador.industrial.datos.modelo.Material
import com.identificador.industrial.datos.modelo.Ubicacion
import com.identificador.industrial.datos.modelo.UsuarioEntity
import com.identificador.industrial.sesion.Rol

/**
 * Datos con los que se llena la base la primera vez que se abre la app.
 *
 * Son referencias reales de uso comun en mantenimiento industrial, elegidas a
 * proposito para que el catalogo sea creible en una demostracion y para que la
 * busqueda visual de la fase 4 tenga casos dificiles de verdad: hay cuatro
 * rodamientos distintos que a simple vista se parecen mucho y solo se
 * distinguen por su numero de parte. Ese es justo el escenario que justifica
 * dar prioridad al OCR sobre el parecido visual.
 */
object CatalogoInicial {

    val materiales: List<Material> = listOf(
        Material(
            id = "M-001",
            nombre = "Rodamiento rigido de bolas 6205-2RS1",
            descripcion = "Rodamiento de una hilera con dos tapas de goma. " +
                "Medidas 25 x 52 x 15 mm. El mas usado en motores y bombas.",
            numeroParte = "6205-2RS1",
            fabricante = "SKF",
            categoria = Categoria.RODAMIENTO,
            unidadMedida = "Pieza",
            existencia = 48,
            existenciaMinima = 12,
            ubicacion = Ubicacion("A", "03", "B", 2, 7),
            medidas = "25 x 52 x 15 mm (interior x exterior x ancho)",
            medidaClave = "Eje 25 mm"
        ),
        Material(
            id = "M-002",
            nombre = "Rodamiento rigido de bolas 6203-2Z",
            descripcion = "Rodamiento de una hilera con dos tapas metalicas. " +
                "Medidas 17 x 40 x 12 mm.",
            numeroParte = "6203-2Z",
            fabricante = "SKF",
            categoria = Categoria.RODAMIENTO,
            unidadMedida = "Pieza",
            existencia = 62,
            existenciaMinima = 15,
            ubicacion = Ubicacion("A", "03", "B", 2, 8),
            medidas = "17 x 40 x 12 mm",
            medidaClave = "Eje 17 mm"
        ),
        Material(
            id = "M-003",
            nombre = "Rodamiento rigido de bolas 6008-2RSR",
            descripcion = "Rodamiento de una hilera con tapas de goma. " +
                "Medidas 40 x 68 x 15 mm.",
            numeroParte = "6008-2RSR",
            fabricante = "FAG",
            categoria = Categoria.RODAMIENTO,
            unidadMedida = "Pieza",
            existencia = 9,
            existenciaMinima = 10,
            ubicacion = Ubicacion("A", "03", "B", 3, 1),
            medidas = "40 x 68 x 15 mm",
            medidaClave = "Eje 40 mm"
        ),
        Material(
            id = "M-004",
            nombre = "Rodamiento de rodillos a rotula 22208 E",
            descripcion = "Soporta desalineacion entre eje y soporte. " +
                "Medidas 40 x 80 x 23 mm.",
            numeroParte = "22208-E",
            fabricante = "SKF",
            categoria = Categoria.RODAMIENTO,
            unidadMedida = "Pieza",
            existencia = 6,
            existenciaMinima = 4,
            ubicacion = Ubicacion("A", "03", "C", 1, 3),
            medidas = "40 x 80 x 23 mm",
            medidaClave = "Eje 40 mm"
        ),
        Material(
            id = "M-005",
            nombre = "Chumacera de pie SY 505 M",
            descripcion = "Soporte de pie con rodamiento incorporado para eje " +
                "de 25 mm. Incluye prisioneros de fijacion.",
            numeroParte = "SY-505-M",
            fabricante = "SKF",
            categoria = Categoria.RODAMIENTO,
            unidadMedida = "Pieza",
            existencia = 14,
            existenciaMinima = 4,
            ubicacion = Ubicacion("A", "03", "C", 1, 9),
            medidas = "Eje 25 mm, base 130 x 38 mm",
            medidaClave = "Eje 25 mm"
        ),

        Material(
            id = "M-006",
            nombre = "Banda en V perfil A-42",
            descripcion = "Banda de transmision clasica, perfil A, 42 pulgadas " +
                "de longitud exterior.",
            numeroParte = "A42",
            fabricante = "Gates",
            categoria = Categoria.TRANSMISION,
            unidadMedida = "Pieza",
            existencia = 22,
            existenciaMinima = 6,
            ubicacion = Ubicacion("A", "05", "A", 1, 2),
            medidas = "Perfil A, 42 pulgadas exteriores (1067 mm)",
            medidaClave = "Perfil A-42"
        ),
        Material(
            id = "M-007",
            nombre = "Banda dentada estrecha XPZ 1250",
            descripcion = "Banda de flancos moldeados, mayor transmision de " +
                "potencia en menos espacio. Longitud 1250 mm.",
            numeroParte = "XPZ-1250",
            fabricante = "Gates",
            categoria = Categoria.TRANSMISION,
            unidadMedida = "Pieza",
            existencia = 8,
            existenciaMinima = 3,
            ubicacion = Ubicacion("A", "05", "A", 1, 5),
            medidas = "Perfil XPZ, 1250 mm de longitud",
            medidaClave = "XPZ-1250"
        ),
        Material(
            id = "M-008",
            nombre = "Catarina simple 40B15",
            descripcion = "Rueda dentada para cadena ANSI 40, 15 dientes, con " +
                "buje para maquinar.",
            numeroParte = "40B15",
            fabricante = "Martin",
            categoria = Categoria.TRANSMISION,
            unidadMedida = "Pieza",
            existencia = 11,
            existenciaMinima = 4,
            ubicacion = Ubicacion("A", "05", "B", 2, 1),
            medidas = "Paso 1/2 pulgada, 15 dientes",
            medidaClave = "ANSI 40"
        ),
        Material(
            id = "M-009",
            nombre = "Cadena de rodillos ANSI 40-1R",
            descripcion = "Cadena simple paso 1/2 pulgada. Se surte por metro " +
                "y se corta a la medida solicitada.",
            numeroParte = "40-1R",
            fabricante = "Renold",
            categoria = Categoria.TRANSMISION,
            unidadMedida = "Metro",
            existencia = 35,
            existenciaMinima = 10,
            ubicacion = Ubicacion("A", "05", "B", 2, 4),
            medidas = "Paso 1/2 pulgada (12.7 mm)",
            medidaClave = "ANSI 40"
        ),

        Material(
            id = "M-010",
            nombre = "Reten radial 25x40x7 HMSA10 RG",
            descripcion = "Sello de eje con labio de goma nitrilo y resorte. " +
                "Eje 25 mm, alojamiento 40 mm, ancho 7 mm.",
            numeroParte = "25X40X7-HMSA10-RG",
            fabricante = "SKF",
            categoria = Categoria.SELLO,
            unidadMedida = "Pieza",
            existencia = 30,
            existenciaMinima = 8,
            ubicacion = Ubicacion("B", "01", "A", 1, 6),
            medidas = "25 x 40 x 7 mm (eje x alojamiento x ancho)",
            medidaClave = "Eje 25 mm"
        ),
        Material(
            id = "M-011",
            nombre = "Oring nitrilo 2-214 N70",
            descripcion = "Anillo de seccion circular, dureza 70 Shore A. " +
                "Medida estandar AS568 2-214.",
            numeroParte = "2-214-N70",
            fabricante = "Parker",
            categoria = Categoria.SELLO,
            unidadMedida = "Pieza",
            existencia = 240,
            existenciaMinima = 50,
            ubicacion = Ubicacion("B", "01", "A", 2, 3),
            medidas = "24.99 mm interior, 3.53 mm de seccion",
            medidaClave = "AS568 2-214"
        ),

        Material(
            id = "M-012",
            nombre = "Tornillo hexagonal DIN 933 M10x40 8.8",
            descripcion = "Tornillo de rosca completa, grado 8.8, acabado " +
                "zincado. Metrica 10, largo 40 mm.",
            numeroParte = "DIN933-M10X40-8.8",
            fabricante = "Generico",
            categoria = Categoria.TORNILLERIA,
            unidadMedida = "Pieza",
            existencia = 480,
            existenciaMinima = 100,
            ubicacion = Ubicacion("B", "02", "D", 1, 1),
            medidas = "M10 x 40 mm, llave de 17 mm",
            medidaClave = "M10"
        ),
        Material(
            id = "M-013",
            nombre = "Tornillo Allen DIN 912 M6x20 12.9",
            descripcion = "Tornillo de cabeza cilindrica con hexagono interior, " +
                "grado 12.9, acabado negro.",
            numeroParte = "DIN912-M6X20-12.9",
            fabricante = "Generico",
            categoria = Categoria.TORNILLERIA,
            unidadMedida = "Pieza",
            existencia = 620,
            existenciaMinima = 150,
            ubicacion = Ubicacion("B", "02", "D", 1, 2),
            medidas = "M6 x 20 mm, hexagono interior de 5 mm",
            medidaClave = "M6"
        ),
        Material(
            id = "M-014",
            nombre = "Tuerca hexagonal ISO 4032 M10",
            descripcion = "Tuerca hexagonal clase 8, acabado zincado, metrica 10.",
            numeroParte = "ISO4032-M10",
            fabricante = "Generico",
            categoria = Categoria.TORNILLERIA,
            unidadMedida = "Pieza",
            existencia = 510,
            existenciaMinima = 120,
            ubicacion = Ubicacion("B", "02", "D", 1, 5),
            medidas = "M10, llave de 17 mm, altura 8.4 mm",
            medidaClave = "M10"
        ),
        Material(
            id = "M-015",
            nombre = "Rondana plana DIN 125 A 10.5",
            descripcion = "Arandela plana para tornilleria M10, acabado zincado.",
            numeroParte = "DIN125A-10.5",
            fabricante = "Generico",
            categoria = Categoria.TORNILLERIA,
            unidadMedida = "Pieza",
            existencia = 0,
            existenciaMinima = 100,
            ubicacion = Ubicacion("B", "02", "D", 1, 6),
            medidas = "10.5 mm interior, 20 mm exterior, 2 mm de espesor",
            medidaClave = "Para M10"
        ),

        Material(
            id = "M-016",
            nombre = "Electrovalvula 5/2 SY5120-5DZ-01",
            descripcion = "Valvula de cinco vias y dos posiciones, solenoide " +
                "sencillo 24 VDC, conexion 1/8 pulgada.",
            numeroParte = "SY5120-5DZ-01",
            fabricante = "SMC",
            categoria = Categoria.NEUMATICA,
            unidadMedida = "Pieza",
            existencia = 7,
            existenciaMinima = 3,
            ubicacion = Ubicacion("B", "04", "A", 2, 2),
            medidas = "Conexion de 1/8 pulgada, cuerpo 18 mm",
            medidaClave = "1/8 pulgada"
        ),
        Material(
            id = "M-017",
            nombre = "Cilindro neumatico DSNU-25-100-PPV-A",
            descripcion = "Cilindro redondo ISO 6432, diametro 25 mm, carrera " +
                "100 mm, con amortiguacion regulable.",
            numeroParte = "DSNU-25-100-PPV-A",
            fabricante = "Festo",
            categoria = Categoria.NEUMATICA,
            unidadMedida = "Pieza",
            existencia = 4,
            existenciaMinima = 2,
            ubicacion = Ubicacion("B", "04", "A", 2, 6),
            medidas = "Diametro 25 mm, carrera 100 mm",
            medidaClave = "Diametro 25 mm"
        ),
        Material(
            id = "M-018",
            nombre = "Unidad filtro regulador AW30-03BG",
            descripcion = "Filtro y regulador de aire con manometro, conexion " +
                "3/8 pulgada, purga manual.",
            numeroParte = "AW30-03BG",
            fabricante = "SMC",
            categoria = Categoria.NEUMATICA,
            unidadMedida = "Pieza",
            existencia = 5,
            existenciaMinima = 2,
            ubicacion = Ubicacion("B", "04", "B", 1, 1),
            medidas = "Conexion de 3/8 pulgada",
            medidaClave = "3/8 pulgada"
        ),

        Material(
            id = "M-019",
            nombre = "Contactor 3RT2026-1BB40",
            descripcion = "Contactor tripolar 25 A, 11 kW, bobina 24 VDC, " +
                "contacto auxiliar 1NA + 1NC.",
            numeroParte = "3RT2026-1BB40",
            fabricante = "Siemens",
            categoria = Categoria.ELECTRICO,
            unidadMedida = "Pieza",
            existencia = 12,
            existenciaMinima = 4,
            ubicacion = Ubicacion("C", "01", "A", 1, 4),
            medidas = "45 x 55 x 87 mm, 25 A",
            medidaClave = "25 A"
        ),
        Material(
            id = "M-020",
            nombre = "Contactor LC1D09M7",
            descripcion = "Contactor tripolar 9 A, 4 kW, bobina 220 VAC.",
            numeroParte = "LC1D09M7",
            fabricante = "Schneider Electric",
            categoria = Categoria.ELECTRICO,
            unidadMedida = "Pieza",
            existencia = 3,
            existenciaMinima = 4,
            ubicacion = Ubicacion("C", "01", "A", 1, 5),
            medidas = "45 x 77 x 81 mm, 9 A",
            medidaClave = "9 A"
        ),
        Material(
            id = "M-021",
            nombre = "Variador de frecuencia ACS355-03E-07A3-4",
            descripcion = "Variador trifasico 380-480 V, 3 kW, con panel de " +
                "control integrado.",
            numeroParte = "ACS355-03E-07A3-4",
            fabricante = "ABB",
            categoria = Categoria.ELECTRICO,
            unidadMedida = "Pieza",
            existencia = 2,
            existenciaMinima = 1,
            ubicacion = Ubicacion("C", "01", "C", 2, 1),
            medidas = "Bastidor R1, 3 kW",
            medidaClave = "3 kW"
        ),

        Material(
            id = "M-022",
            nombre = "Sensor fotoelectrico E3Z-D62",
            descripcion = "Sensor difuso con supresion de fondo, alcance 1 m, " +
                "salida NPN, 12-24 VDC.",
            numeroParte = "E3Z-D62",
            fabricante = "Omron",
            categoria = Categoria.SENSOR,
            unidadMedida = "Pieza",
            existencia = 16,
            existenciaMinima = 5,
            ubicacion = Ubicacion("C", "02", "A", 1, 2),
            medidas = "Cuerpo 11 x 21 x 31 mm, alcance 1 m",
            medidaClave = "Alcance 1 m"
        ),
        Material(
            id = "M-023",
            nombre = "Sensor inductivo Bi5-M18-AP6X",
            descripcion = "Deteccion de metales sin contacto, alcance 5 mm, " +
                "cuerpo M18, salida PNP normalmente abierta.",
            numeroParte = "BI5-M18-AP6X",
            fabricante = "Turck",
            categoria = Categoria.SENSOR,
            unidadMedida = "Pieza",
            existencia = 19,
            existenciaMinima = 6,
            ubicacion = Ubicacion("C", "02", "A", 1, 7),
            medidas = "Rosca M18, alcance de 5 mm",
            medidaClave = "M18"
        ),

        Material(
            id = "M-024",
            nombre = "Elemento filtrante hidraulico 925752",
            descripcion = "Cartucho de filtracion 10 micras para linea de " +
                "retorno de sistemas hidraulicos.",
            numeroParte = "925752",
            fabricante = "Parker",
            categoria = Categoria.FILTRO,
            unidadMedida = "Pieza",
            existencia = 6,
            existenciaMinima = 3,
            ubicacion = Ubicacion("C", "03", "B", 1, 1),
            medidas = "Cartucho de 10 micras",
            medidaClave = "10 micras"
        ),

        Material(
            id = "M-025",
            nombre = "Grasa de uso general LGMT 2",
            descripcion = "Grasa de litio NLGI 2 para rodamientos, rango de " +
                "temperatura -30 a 120 grados. Cartucho de 420 ml.",
            numeroParte = "LGMT-2-0.4",
            fabricante = "SKF",
            categoria = Categoria.LUBRICANTE,
            unidadMedida = "Cartucho",
            existencia = 25,
            existenciaMinima = 8,
            ubicacion = Ubicacion("C", "04", "A", 1, 1),
            medidas = "Cartucho de 420 ml",
            medidaClave = "420 ml"
        ),

        Material(
            id = "M-026",
            nombre = "Juego de llaves Allen metricas",
            descripcion = "Nueve piezas de 1.5 a 10 mm, punta esferica, " +
                "acero cromo vanadio, con soporte.",
            numeroParte = "TRU-15532",
            fabricante = "Truper",
            categoria = Categoria.HERRAMIENTA,
            unidadMedida = "Juego",
            existencia = 4,
            existenciaMinima = 2,
            ubicacion = Ubicacion("C", "05", "A", 1, 3),
            medidas = "Nueve llaves de 1.5 a 10 mm",
            medidaClave = "1.5 a 10 mm"
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
        return UsuarioEntity(
            id = id,
            usuario = usuario,
            nombre = nombre,
            rol = rol,
            hashClave = Claves.hash(claveEnClaro, sal),
            sal = sal
        )
    }
}
