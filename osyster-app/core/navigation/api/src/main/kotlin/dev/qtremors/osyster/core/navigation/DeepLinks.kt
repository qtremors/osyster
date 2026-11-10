package dev.qtremors.osyster.core.navigation

/**
 * Extras that home-screen widgets and tiles put on the app's launch intent to open a specific
 * tool. The app shell reads them; widgets never reference the activity class directly.
 */
object DeepLinks {
    const val EXTRA_OPEN_TOOL = "dev.qtremors.osyster.OPEN_TOOL"

    /** A page inside the tool named by [EXTRA_OPEN_TOOL], for launcher shortcuts. */
    const val EXTRA_OPEN_PAGE = "dev.qtremors.osyster.OPEN_PAGE"

    /** The scanner, with the photo picker open. */
    const val PAGE_SCAN_PICTURE = "scan_picture"

    /** The code creator. */
    const val PAGE_CREATE_CODE = "create_code"
}
