plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.kapt")
}

android {
    namespace = "com.mapovich.bbmystatz"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.mapovich.bbmystatz"
        minSdk = 33
        targetSdk = 34
        versionCode = 19
        versionName = "0.$versionCode Beta"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
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
}