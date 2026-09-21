import com.android.build.api.artifact.SingleArtifact
import java.util.Properties

/** Copies the release bundle to `Osyster-<version>.aab`. */
abstract class NameBundleTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val inputBundle: RegularFileProperty

    @get:OutputFile
    abstract val outputBundle: RegularFileProperty

    @TaskAction
    fun nameBundle() {
        inputBundle.get().asFile.copyTo(outputBundle.get().asFile, overwrite = true)
    }
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "dev.qtremors.osyster"
    compileSdk = 37

    defaultConfig {
        applicationId = "dev.qtremors.osyster"
        minSdk = 26
        targetSdk = 37
        // versionCode is versionName without dots.
        versionCode = 22
        versionName = "0.2.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Release signing from an untracked signing.properties, falling back to local.properties.
    // Without all four keys the release build is simply unsigned.
    val keystoreProperties = Properties()
    var keystorePropertiesFile = rootProject.file("signing.properties")
    if (!keystorePropertiesFile.exists()) {
        keystorePropertiesFile = rootProject.file("local.properties")
    }
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { keystoreProperties.load(it) }
    }
    val storeFileProp = keystoreProperties["signing.storeFile"]?.toString()
    val storePasswordProp = keystoreProperties["signing.storePassword"]?.toString()
    val keyAliasProp = keystoreProperties["signing.keyAlias"]?.toString()
    val keyPasswordProp = keystoreProperties["signing.keyPassword"]?.toString()
    val hasSigningConfig = storeFileProp != null && storePasswordProp != null &&
        keyAliasProp != null && keyPasswordProp != null

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
            // Installs beside the release build, with its own name on the launcher.
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
    testOptions {
        unitTests.isIncludeAndroidResources = true
        // ArchitectureBoundaryTest imports every production module's classes into one graph.
        unitTests.all { it.maxHeapSize = "2g" }
    }
}

// Artifacts are named after the app and version: Osyster-X.Y.Z.apk, Osyster-X.Y.Z-debug.apk,
// Osyster-X.Y.Z.aab.
androidComponents {
    onVariants { variant ->
        variant.outputs.forEach { output ->
            val version = output.versionName.get() ?: "0.0.0"
            output.outputFileName.set("Osyster-$version.apk")
        }
        val version = variant.outputs.single().versionName.get() ?: "0.0.0"
        val nameBundle = tasks.register<NameBundleTask>(
            "name${variant.name.replaceFirstChar { it.uppercaseChar() }}Bundle"
        )
        variant.artifacts.use(nameBundle)
            .wiredWithFiles(NameBundleTask::inputBundle, NameBundleTask::outputBundle)
            .withName("Osyster-$version.aab")
            .toTransform(SingleArtifact.BUNDLE)
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        optIn.add("androidx.compose.material3.ExperimentalMaterial3Api")
        optIn.add("androidx.compose.material3.ExperimentalMaterial3ExpressiveApi")
    }
}

dependencies {
    implementation(project(":core:runtime"))
    implementation(project(":core:navigation:api"))
    implementation(project(":core:device:domain"))
    // Concrete data modules are wired here only, so Hilt sees their bindings.
    implementation(project(":core:device:data"))
    implementation(project(":core:ui"))
    implementation(project(":feature:home"))
    implementation(project(":feature:device"))
    implementation(project(":feature:tools"))
    implementation(project(":feature:settings"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.archunit.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
