import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "dev.qtremors.osyster"
    compileSdk = 37

    defaultConfig {
        applicationId = "dev.qtremors.osyster"
        minSdk = 24
        targetSdk = 37
        versionCode = 8
        versionName = "0.0.8"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    val keystoreProperties = Properties()
    var keystorePropertiesFile = rootProject.file("signing.properties")
    if (!keystorePropertiesFile.exists()) {
        keystorePropertiesFile = rootProject.file("local.properties")
    }
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use {
            keystoreProperties.load(it)
        }
    }

    val storeFileProp = keystoreProperties["signing.storeFile"]?.toString()
    val storePasswordProp = keystoreProperties["signing.storePassword"]?.toString()
    val keyAliasProp = keystoreProperties["signing.keyAlias"]?.toString()
    val keyPasswordProp = keystoreProperties["signing.keyPassword"]?.toString()

    val hasSigningConfig = storeFileProp != null && storePasswordProp != null && keyAliasProp != null && keyPasswordProp != null

    if (hasSigningConfig) {
        signingConfigs {
            create("release") {
                storeFile = file(storeFileProp!!)
                storePassword = storePasswordProp
                keyAlias = keyAliasProp
                keyPassword = keyPasswordProp
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            manifestPlaceholders["appLabel"] = "Osyster Debug"
            enableUnitTestCoverage = true
        }
        release {
            if (hasSigningConfig) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = true
            isShrinkResources = true
            manifestPlaceholders["appLabel"] = "Osyster"
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        jniLibs {
            keepDebugSymbols += setOf(
                "**/libandroidx.graphics.path.so"
            )
        }
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

androidComponents {
    onVariants { variant ->
        variant.outputs.forEach { output ->
            val version = output.versionName.get() ?: "0.0.0"
            output.outputFileName.set("Osyster-$version.apk")
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

composeCompiler {
    reportsDestination = layout.buildDirectory.dir("compose_compiler/reports")
    metricsDestination = layout.buildDirectory.dir("compose_compiler/metrics")
}

val verifyVersionCatalogFreshness = tasks.register("verifyVersionCatalogFreshness") {
    group = "verification"
    description = "Verifies that dependency freshness checks have an active version catalog to inspect."
    val versionCatalog = rootProject.layout.projectDirectory.file("gradle/libs.versions.toml")
    inputs.file(versionCatalog)
    doLast {
        val catalog = versionCatalog.asFile
        require(catalog.exists()) {
            "Missing gradle/libs.versions.toml; dependency freshness checks need a version catalog."
        }
        val text = catalog.readText()
        require("[versions]" in text && "[libraries]" in text && "[plugins]" in text) {
            "gradle/libs.versions.toml must keep [versions], [libraries], and [plugins] sections."
        }
    }
}

val verifyReleaseVersionMetadata = tasks.register("verifyReleaseVersionMetadata") {
    group = "verification"
    description = "Checks that the app declares explicit versionName and versionCode metadata."
    val appBuildFile = layout.projectDirectory.file("build.gradle.kts")
    inputs.file(appBuildFile)
    doLast {
        val buildFileText = appBuildFile.asFile.readText()
        require(Regex("""versionName\s*=\s*"[^"]+"""").containsMatchIn(buildFileText)) {
            "app/build.gradle.kts must declare versionName."
        }
        require(Regex("""versionCode\s*=\s*\d+""").containsMatchIn(buildFileText)) {
            "app/build.gradle.kts must declare versionCode."
        }
    }
}

tasks.register("verifyOsysterBuildConventions") {
    group = "verification"
    description = "Runs Osyster build convention checks used for release readiness."
    dependsOn(verifyVersionCatalogFreshness, verifyReleaseVersionMetadata)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.collections.immutable)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.adaptive)
    implementation(libs.androidx.compose.material3.adaptive.layout)
    implementation(libs.androidx.compose.material3.adaptive.navigation)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.material.kolor)
    implementation(libs.androidx.graphics.shapes)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}