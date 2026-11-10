// Pure Kotlin: no Android, no Compose. ArchitectureBoundaryTest checks this.
// ZXing core is a plain Java library: it reads and parses codes here and is shared with the feature.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

dependencies {
    api(libs.zxing.core)
    implementation("javax.inject:javax.inject:1")
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit)
}
