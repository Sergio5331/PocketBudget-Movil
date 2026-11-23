plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}


android {

    namespace = "com.pocketbudget.app"


    compileSdk = 37


    defaultConfig {

        applicationId = "com.pocketbudget.app"

        minSdk = 24

        targetSdk = 37

        versionCode = 1

        versionName = "1.0"


        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"

    }


    buildTypes {

        release {

            isMinifyEnabled = false

            proguardFiles(
                getDefaultProguardFile(
                    "proguard-android-optimize.txt"
                ),
                "proguard-rules.pro"
            )

        }

    }


    compileOptions {

        sourceCompatibility =
            JavaVersion.VERSION_11

        targetCompatibility =
            JavaVersion.VERSION_11

    }


    buildFeatures {

        compose = true

    }

}



dependencies {


    // Compose BOM

    implementation(
        platform(
            libs.androidx.compose.bom
        )
    )


    // Activity Compose

    implementation(
        libs.androidx.activity.compose
    )


    // Material 3

    implementation(
        libs.androidx.compose.material3
    )


    // UI Compose

    implementation(
        libs.androidx.compose.ui
    )


    implementation(
        libs.androidx.compose.ui.graphics
    )


    implementation(
        libs.androidx.compose.ui.tooling.preview
    )


    // Core

    implementation(
        libs.androidx.core.ktx
    )


    implementation(
        libs.androidx.lifecycle.runtime.ktx
    )



    // ==========================
    // POCKETBUDGET DEPENDENCIES
    // ==========================


    // Navegación entre pantallas

    implementation(
        "androidx.navigation:navigation-compose:2.8.5"
    )


    // Iconos completos Material

    implementation(
        "androidx.compose.material:material-icons-extended"
    )


    // Animaciones Compose

    implementation(
        "androidx.compose.animation:animation"
    )


    // ViewModel para arquitectura MVVM

    implementation(
        "androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7"
    )


    // Persistencia local futura

    implementation(
        "androidx.datastore:datastore-preferences:1.1.1"
    )
// ==========================
// SUPABASE
// ==========================

// Cliente Supabase
    implementation("io.github.jan-tennert.supabase:postgrest-kt:3.0.0")

    implementation("io.github.jan-tennert.supabase:auth-kt:3.0.0")

    implementation("io.github.jan-tennert.supabase:storage-kt:3.0.0")


// Cliente HTTP para Android
    implementation("io.ktor:ktor-client-android:3.0.0")


// Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")


    // Testing

    testImplementation(
        libs.junit
    )


    androidTestImplementation(
        platform(
            libs.androidx.compose.bom
        )
    )


    androidTestImplementation(
        libs.androidx.compose.ui.test.junit4
    )


    androidTestImplementation(
        libs.androidx.espresso.core
    )


    androidTestImplementation(
        libs.androidx.junit
    )


    debugImplementation(
        libs.androidx.compose.ui.test.manifest
    )


    debugImplementation(
        libs.androidx.compose.ui.tooling
    )


}