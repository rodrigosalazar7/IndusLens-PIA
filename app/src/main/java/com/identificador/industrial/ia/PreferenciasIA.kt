package com.identificador.industrial.ia

import android.content.Context

/**
 * Consentimiento local para el analisis visual en linea.
 *
 * La decision vive solo en el telefono. Nunca se envia una fotografia a la
 * nube hasta que la persona acepta expresamente el aviso de la camara.
 */
object PreferenciasIA {

    private const val ARCHIVO = "preferencias_ia"
    private const val CLAVE_DECIDIDA = "decision_tomada"
    private const val CLAVE_EN_LINEA = "ia_en_linea"

    fun decisionTomada(contexto: Context): Boolean =
        preferencias(contexto).getBoolean(CLAVE_DECIDIDA, false)

    fun usarEnLinea(contexto: Context): Boolean =
        preferencias(contexto).getBoolean(CLAVE_EN_LINEA, false)

    fun guardar(contexto: Context, usarEnLinea: Boolean) {
        preferencias(contexto).edit()
            .putBoolean(CLAVE_DECIDIDA, true)
            .putBoolean(CLAVE_EN_LINEA, usarEnLinea)
            .apply()
    }

    private fun preferencias(contexto: Context) =
        contexto.applicationContext.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)
}
