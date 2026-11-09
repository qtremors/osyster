package dev.qtremors.osyster.core.navigation

/**
 * Extras that home-screen widgets and tiles put on the app's launch intent to open a specific
 * tool. The app shell reads them; widgets never reference the activity class directly.
 */
object DeepLinks {
    const val EXTRA_OPEN_TOOL = "dev.qtremors.osyster.OPEN_TOOL"
}
