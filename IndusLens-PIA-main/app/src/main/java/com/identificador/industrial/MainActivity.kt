package com.identificador.industrial

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.identificador.industrial.navegacion.NavegacionApp
import com.identificador.industrial.ui.theme.IndusLensTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            IndusLensTheme {
                NavegacionApp()
            }
        }
    }
}
