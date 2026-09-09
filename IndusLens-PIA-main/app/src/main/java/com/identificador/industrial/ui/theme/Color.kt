package com.identificador.industrial.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Identidad oficial de IndusLens, tomada de la ficha tecnica del equipo.
 *
 * El azul profundo representa el entorno industrial, el azul claro identifica
 * la capa de inteligencia artificial y el ambar se reserva para hallazgos,
 * advertencias y elementos que requieren atencion.
 */
val AzulProfundo = Color(0xFF0E3B5C)
val AzulMedio = Color(0xFF17557F)
val AzulInteligencia = Color(0xFF2E86C1)
val AmbarHallazgo = Color(0xFFE8A33D)
val ColorAmbarOscuro = Color(0xFF6E4A12)

// Superficies
val FondoBase = Color(0xFF071D2B)
val Superficie = AzulProfundo
val SuperficieAlta = Color(0xFF124765)
val Borde = AzulMedio

// Texto
val TextoPrincipal = Color(0xFFF4F8FB)
val TextoSecundario = Color(0xFFB8CBD7)

// Estados
val VerdeExito = Color(0xFF3FB950)
val AmarilloAviso = AmbarHallazgo
val RojoError = Color(0xFFE5484D)
