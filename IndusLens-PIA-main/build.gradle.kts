// Configuracion de nivel raiz. Los plugins se declaran aqui con `apply false`
// y se activan en cada modulo (por ahora solo :app).
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.google.services) apply false
}
