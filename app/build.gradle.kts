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
        versionCode = 13
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
            ndk {
                debugSymbolLevel = "SYMBOL_TABLE"
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
            enableUnitTestCoverage = true
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
    constraints {
        implementation("androidx.fragment:fragment:1.9.1") {
            because("Avoid resolving the obsolete Fragment 1.1.0 transitive dependency from wearable and Play Services libraries.")
        }
    }

    testImplementation("junit:junit:4.13.2")

    androidTestImplementation(platform("androidx.compose:compose-bom:2026.09.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.wear.tiles:tiles-renderer:1.6.2")

    // AndroidX Core & Activity
    implementation("androidx.core:core-ktx:1.19.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")

    // Wear OS & Compose
    implementation(platform("androidx.compose:compose-bom:2026.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-guava:1.11.0")

    // Wear Compose
    implementation("androidx.wear.compose:compose-foundation:1.7.0")
    implementation("androidx.wear.compose:compose-material3:1.7.0")

    // Wear Tiles & Complications
    implementation("androidx.wear.tiles:tiles:1.6.2")
    implementation("androidx.wear.protolayout:protolayout:1.4.2")
    implementation("androidx.wear.protolayout:protolayout-expression:1.4.2")
    implementation("androidx.wear.protolayout:protolayout-material:1.4.2")
    implementation("androidx.wear.watchface:watchface-complications-data-source:1.3.0")
    implementation("androidx.wear.watchface:watchface-complications-data-source-ktx:1.3.0")

    // Data Persistence
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Wearable Services
    implementation("com.google.android.gms:play-services-wearable:20.0.1")
    implementation("androidx.wear:wear-remote-interactions:1.2.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

tasks.register("verifyCoreLogicCoverage") {
    group = "verification"
    description = "Fails unless unit-test line coverage for core counter logic is 100%."
    dependsOn("createDebugUnitTestCoverageReport")

    doLast {
        val reportFile = layout.buildDirectory
            .file("reports/coverage/test/debug/report.xml")
            .get()
            .asFile
        check(reportFile.isFile) {
            "JaCoCo report not found at ${reportFile.path}"
        }

        val requiredSourceFiles = setOf(
            "com/gauramala/wear/presentation/MantraCounterStateMachine.kt",
            "com/gauramala/wear/presentation/CounterHapticPolicy.kt",
            "com/gauramala/wear/presentation/RotaryBeadInput.kt",
            "com/gauramala/wear/presentation/MantraUiState.kt",
            "com/gauramala/wear/tile/GauraMalaTileContent.kt"
        )
        val documentFactory = javax.xml.parsers.DocumentBuilderFactory.newInstance().apply {
            setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
            setFeature("http://xml.org/sax/features/external-general-entities", false)
            setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            isXIncludeAware = false
            isExpandEntityReferences = false
        }
        val report = documentFactory.newDocumentBuilder().parse(reportFile)
        val sourceFiles = report.getElementsByTagName("package")
            .let { packages ->
                (0 until packages.length)
                    .map { packages.item(it) as org.w3c.dom.Element }
                    .flatMap { packageNode ->
                        val files = packageNode.getElementsByTagName("sourcefile")
                        (0 until files.length).map {
                            val sourceFile = files.item(it) as org.w3c.dom.Element
                            "${packageNode.getAttribute("name")}/${sourceFile.getAttribute("name")}" to sourceFile
                        }
                    }
            }
            .filter { (path, _) -> path in requiredSourceFiles }
            .toMap()

        val missingFiles = requiredSourceFiles - sourceFiles.keys
        check(missingFiles.isEmpty()) {
            "Coverage report is missing core source files: ${missingFiles.joinToString()}"
        }

        var coveredLines = 0
        var missedLines = 0
        sourceFiles.forEach { (path, sourceFile) ->
            val lineCounter = sourceFile.getElementsByTagName("counter")
                .let { counters ->
                    (0 until counters.length)
                        .map { counters.item(it) as org.w3c.dom.Element }
                        .first { it.getAttribute("type") == "LINE" && it.parentNode === sourceFile }
                }
            val covered = lineCounter.getAttribute("covered").toInt()
            val missed = lineCounter.getAttribute("missed").toInt()
            coveredLines += covered
            missedLines += missed
            logger.lifecycle("Core unit coverage: $path ${covered * 100 / (covered + missed)}% ($covered/${covered + missed} lines)")
        }

        val totalLines = coveredLines + missedLines
        val coverage = if (totalLines == 0) 1.0 else coveredLines.toDouble() / totalLines
        logger.lifecycle("Core unit-test line coverage: ${"%.1f".format(coverage * 100)}% ($coveredLines/$totalLines lines)")
        check(coverage >= 1.0) {
            "Core unit-test line coverage is below 100%. Add tests for every uncovered counter behavior."
        }
    }
}
