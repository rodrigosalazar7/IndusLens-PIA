plugins {
    alias(libs.plugins.android.application)
    // Desde AGP 9 el soporte de Kotlin viene integrado en el plugin de Android:
    // aplicar ademas 'org.jetbrains.kotlin.android' rompe la compilacion.
    // El plugin de Compose si se sigue aplicando aparte.
    alias(libs.plugins.kotlin.compose)
    // Procesador de anotaciones: genera el codigo de Room en tiempo de compilacion.
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.identificador.industrial"
    // Las librerias de Compose del BOM 2026.08 exigen compilar contra API 37.
    compileSdk = 37

    defaultConfig {
        applicationId = "com.identificador.industrial"
        minSdk = 26
        // targetSdk se deja en 36 a proposito: compilar contra 37 permite usar
        // las APIs nuevas, pero subir targetSdk activa cambios de comportamiento
        // en tiempo de ejecucion que no se han probado en esta app.
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // Con el Kotlin integrado de AGP 9, sus opciones van dentro de `android`.
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        compose = true
    }

    /**
     * Un APK por arquitectura de CPU.
     *
     * ML Kit, MediaPipe y TensorFlow Lite traen codigo nativo compilado para
     * cuatro arquitecturas. En un APK unico son unos 125 MB de librerias de
     * las que cada telefono usa exactamente una: el resto viaja como lastre.
     *
     * Separandolo, cada APK ronda los 60 MB. Para entregar:
     *   - arm64-v8a    telefonos modernos (practicamente todos desde 2017)
     *   - armeabi-v7a  telefonos antiguos de 32 bits
     *   - x86_64       emulador de Android Studio
     *
     * Se conserva ademas el APK universal, que funciona en cualquier sitio a
     * costa del tamano, por si hace falta entregar un unico archivo.
     */
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86_64")
            isUniversalApk = true
        }
    }

    androidResources {
        // El modelo se carga mapeandolo en memoria directamente desde el APK.
        // Si se empaqueta comprimido, ese mapeo falla en tiempo de ejecucion.
        noCompress += "tflite"
    }
}

// Room exporta el esquema de la base de datos a app/schemas. Sirve para revisar
// la estructura y es obligatorio para poder escribir migraciones mas adelante.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    implementation(libs.mlkit.text.recognition)
    implementation(libs.mediapipe.tasks.vision)

    debugImplementation(libs.androidx.ui.tooling)
}
