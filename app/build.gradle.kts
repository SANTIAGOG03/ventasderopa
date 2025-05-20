plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    // ¡NUEVO! Plugin del compilador de Compose, requerido para Kotlin 2.0+
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.example.ecotrend" // Asegúrate de que esto coincida con tu nombre de paquete
    compileSdk = 34 // Usa la última versión de SDK disponible

    defaultConfig {
        applicationId = "com.example.ecotrend" // Asegúrate de que esto coincida con tu nombre de paquete
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false // Reemplazar con true para minificar el código en producción
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        // Habilitar Compose
        compose = true
    }
    composeOptions {
        // Asegúrate de que esta versión coincida con la versión de tu compilador de Kotlin
        kotlinCompilerExtensionVersion = "1.5.1" // O la versión más reciente compatible
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Dependencias de Compose
    implementation("androidx.activity:activity-compose:1.8.2") // O la versión más reciente
    implementation(platform("androidx.compose:compose-bom:2023.08.00")) // O la versión más reciente
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3") // O la versión más reciente

    // Dependencias para WebView (AndroidView es parte de compose.ui.viewinterop)
    // No se necesita una dependencia específica para AndroidView, ya está incluida.

    // Dependencias de compatibilidad (si aún las necesitas para algo fuera de Compose)
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4") // Puede que no sea necesaria si todo es Compose

    // Dependencias para Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.08.00")) // O la versión más reciente
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
