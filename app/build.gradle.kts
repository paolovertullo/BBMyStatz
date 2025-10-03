plugins {
    id("com.google.dagger.hilt.android")
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("com.google.devtools.ksp")
    //id("org.jetbrains.kotlin.kapt")
    id("org.jetbrains.kotlin.plugin.compose")

}

android {
    namespace = "com.mapovich.bbmystatz"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mapovich.bbmystatz"
        minSdk = 24
        targetSdk = 35
        versionCode = 19
        versionName = "0.$versionCode Beta"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    //kapt {
    //    correctErrorTypes = true
    //}


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
    kotlinOptions {
        jvmTarget = "17"
    }

    // Forza il toolchain per TUTTO Kotlin/Java
    kotlin {
        jvmToolchain(17)
    }

    tasks.withType<JavaCompile>().configureEach {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
    buildFeatures {
        viewBinding = true
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"  // usa la versione aggiornata
    }


    buildTypes {
        release {
            // ... your release build config
            setProperty("archivesBaseName", "BBMyStatz-v${defaultConfig.versionName}")
        }

        debug {
            // ... your debug build config
            setProperty("archivesBaseName", "BBMyStatz-v${defaultConfig.versionName}")
        }
    }

}

dependencies {
    implementation("com.google.dagger:hilt-android:2.57.2")
    implementation("androidx.hilt:hilt-lifecycle-viewmodel-compose:1.3.0")
// Usa KSP per Hilt:
    ksp("com.google.dagger:hilt-compiler:2.57.2")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    val room_version = "2.6.1"

    implementation("androidx.room:room-runtime:$room_version")
    annotationProcessor("androidx.room:room-compiler:$room_version")

    //implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:$room_version")
    //kapt("androidx.room:room-compiler:$room_version")
    ksp("androidx.room:room-compiler:$room_version")

    // PER I GRAFICI
    implementation (libs.mpandroidchart)
    implementation ("com.github.furkanaskin:ClickablePieChart:1.0.9")
    implementation ("com.github.PhilJay:MPAndroidChart:v3.1.0")

    implementation ("org.jsoup:jsoup:1.15.4")

    //PER ROOM
    //implementation(libs.androidx.room.runtime)
    //implementation(libs.androidx.room.ktx)
    //implementation(libs.androidx.room.compiler)
    //annotationProcessor(libs.androidx.room.compiler)


    //COMPOSE 08-09-2025

    // --- COMPOSE (Kotlin 2.x) ---

// BOM: gestisce le versioni di tutte le lib Compose
    implementation(platform("androidx.compose:compose-bom:2024.09.01"))
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.09.01"))

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

// Icone Material (per Icons.Rounded.*)
    implementation("androidx.compose.material:material-icons-extended")

// Activity + Compose
    implementation("androidx.activity:activity-compose:1.9.3")

// Lifecycle + ViewModel integration
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")



}