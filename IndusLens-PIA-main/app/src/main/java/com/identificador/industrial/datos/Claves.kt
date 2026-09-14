package com.identificador.industrial.datos

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/**
 * Guardado y verificacion de contrasenas.
 *
 * Cada usuario tiene su propia sal aleatoria, de modo que dos personas con la
 * misma contrasena produzcan hashes distintos y no se pueda deducir una a
 * partir de la otra ni usar tablas precalculadas.
 *
 * Limitacion asumida y documentada: SHA-256 es rapido por diseno, y eso juega
 * a favor de quien intenta romper contrasenas por fuerza bruta. Lo correcto en
 * produccion es una funcion lenta y parametrizable (bcrypt, scrypt, Argon2).
 * Aqui se usa SHA-256 porque no exige dependencias externas y el alcance es
 * academico. Es deuda tecnica conocida, no un descuido.
 */
object Claves {

    private const val ITERACIONES = 10_000

    fun generarSal(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return Base64.getEncoder().encodeToString(bytes)
    }

    /**
     * Se aplica el hash repetidas veces para encarecer algo el ataque por
     * fuerza bruta. No sustituye a un KDF real, pero es mejor que una pasada.
     */
    fun hash(clave: String, sal: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        var actual = (sal + clave).toByteArray(Charsets.UTF_8)
        repeat(ITERACIONES) {
            actual = digest.digest(actual)
        }
        return Base64.getEncoder().encodeToString(actual)
    }

    /**
     * Compara en tiempo constante. Una comparacion normal de cadenas se
     * detiene en el primer caracter distinto, y ese detalle de tiempo puede
     * filtrar informacion sobre el hash correcto.
     */
    fun verificar(clave: String, sal: String, hashEsperado: String): Boolean {
        val calculado = hash(clave, sal)
        return MessageDigest.isEqual(
            calculado.toByteArray(Charsets.UTF_8),
            hashEsperado.toByteArray(Charsets.UTF_8)
        )
    }
}
