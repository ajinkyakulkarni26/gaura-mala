plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val uploadStoreFile = providers.gradleProperty("GAURA_UPLOAD_STORE_FILE").orNull
val uploadStorePassword = providers.gradleProperty("GAURA_UPLOAD_STORE_PASSWORD").orNull
val uploadKeyAlias = providers.gradleProperty("GAURA_UPLOAD_KEY_ALIAS").orNull
val uploadKeyPassword = providers.gradleProperty("GAURA_UPLOAD_KEY_PASSWORD").orNull

android {
    namespace = "com.gauramala.wear"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.gauramala.wear"
        minSdk = 30
        targetSdk = 37
        versionCode = 10
        versionName = "1.0.0"
    }

    signingConfigs {
        create("release") {
            storeFile = uploadStoreFile?.let { file(it) }
            storePassword = uploadStorePassword
            keyAlias = uploadKeyAlias
            keyPassword = uploadKeyPassword
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn")
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")

    // AndroidX Core & Activity
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")

    // Wear OS & Compose
    implementation(platform("androidx.compose:compose-bom:2024.09.02"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-guava:1.8.1")

    // Wear Compose
    implementation("androidx.wear.compose:compose-foundation:1.7.0")
    implementation("androidx.wear.compose:compose-material3:1.7.0")

    // Wear Tiles & Complications
    implementation("androidx.wear.tiles:tiles:1.4.0")
    implementation("androidx.wear.protolayout:protolayout:1.2.0")
    implementation("androidx.wear.protolayout:protolayout-expression:1.2.0")
    implementation("androidx.wear.protolayout:protolayout-material:1.2.0")
    implementation("androidx.wear.watchface:watchface-complications-data-source:1.2.1")
    implementation("androidx.wear.watchface:watchface-complications-data-source-ktx:1.2.1")

    // Data Persistence
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Wearable Services
    implementation("com.google.android.gms:play-services-wearable:18.2.0")
    implementation("androidx.wear:wear-remote-interactions:1.2.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
