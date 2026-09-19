package dev.qtremors.osyster.ui.settings

data class OpenSourceComponent(
    val name: String,
    val purpose: String,
    val license: String,
    val sourceUrl: String,
    val licenseUrl: String
)

object OsysterOpenSourceComponents {
    val all = listOf(
        OpenSourceComponent(
            name = "AndroidX & Jetpack Compose",
            purpose = "Core Android application runtime, lifecycle handling, and modern declarative user interface.",
            license = "Apache License 2.0",
            sourceUrl = "https://android.googlesource.com/platform/frameworks/support/",
            licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0"
        ),
        OpenSourceComponent(
            name = "Material 3 & Expressive Components",
            purpose = "Material Design 3 tokens, expressive adaptive layouts, icons, and dynamic color system.",
            license = "Apache License 2.0",
            sourceUrl = "https://github.com/androidx/androidx",
            licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0"
        ),
        OpenSourceComponent(
            name = "Kotlin Coroutines",
            purpose = "Asynchronous scheduling, non-blocking telemetry collection, and reactive state flows.",
            license = "Apache License 2.0",
            sourceUrl = "https://github.com/Kotlin/kotlinx.coroutines",
            licenseUrl = "https://github.com/Kotlin/kotlinx.coroutines/blob/master/LICENSE.txt"
        ),
        OpenSourceComponent(
            name = "Kotlinx Serialization JSON",
            purpose = "Type-safe JSON serialization for preferences backup, restore, and structured exports.",
            license = "Apache License 2.0",
            sourceUrl = "https://github.com/Kotlin/kotlinx.serialization",
            licenseUrl = "https://github.com/Kotlin/kotlinx.serialization/blob/master/LICENSE.txt"
        ),
        OpenSourceComponent(
            name = "Kotlinx Immutable Collections",
            purpose = "Efficient immutable data structures for Compose state stability and performant recompositions.",
            license = "Apache License 2.0",
            sourceUrl = "https://github.com/Kotlin/kotlinx.collections.immutable",
            licenseUrl = "https://github.com/Kotlin/kotlinx.collections.immutable/blob/master/LICENSE.txt"
        ),
        OpenSourceComponent(
            name = "AndroidX Graphics Shapes",
            purpose = "Expressive polygonal shapes, morphable geometries, and rounded segmented controls.",
            license = "Apache License 2.0",
            sourceUrl = "https://developer.android.com/jetpack/androidx/releases/graphics",
            licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0"
        ),
        OpenSourceComponent(
            name = "AndroidX Navigation Compose",
            purpose = "Type-safe navigational backstack, transition animations, and deep-link dispatching.",
            license = "Apache License 2.0",
            sourceUrl = "https://developer.android.com/jetpack/androidx/releases/navigation",
            licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0"
        )
    )
}
