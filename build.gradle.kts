plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.ksp) apply false // This will now look for 2.0.0-1.0.21
    alias(libs.plugins.lumo) apply false
}