package dev.qtremors.osyster.widget.music

internal enum class SessionActivity {
    PLAYING,
    TRANSITIONAL,
    PAUSED,
    INACTIVE
}

internal data class SessionCandidate<T>(
    val key: T,
    val activity: SessionActivity,
    val lastUpdateTime: Long
)

internal data class SessionAction(
    val id: String,
    val label: String
)

internal enum class MusicModeControl {
    REPEAT,
    SHUFFLE
}

/**
 * Chooses one media session and keeps it selected until a different session is
 * actually playing. This prevents paused or newly-posted player notifications
 * from stealing the widget from the player the user is controlling.
 */
internal fun <T> selectMediaSession(
    candidates: List<SessionCandidate<T>>,
    currentKey: T?
): T? {
    if (candidates.isEmpty()) return null

    val current = currentKey?.let { key -> candidates.firstOrNull { it.key == key } }
    if (current?.activity == SessionActivity.PLAYING) return current.key

    candidates
        .filter { it.activity == SessionActivity.PLAYING }
        .maxByOrNull { it.lastUpdateTime }
        ?.let { return it.key }

    if (
        current != null &&
        (current.activity == SessionActivity.TRANSITIONAL || current.activity == SessionActivity.PAUSED)
    ) {
        return current.key
    }

    return candidates
        .filter { it.activity != SessionActivity.INACTIVE }
        .maxWithOrNull(
            compareBy<SessionCandidate<T>> { it.activity.priority }
                .thenBy { it.lastUpdateTime }
        )
        ?.key
        ?: candidates.maxByOrNull { it.lastUpdateTime }?.key
}

internal fun findModeAction(
    actions: List<SessionAction>,
    control: MusicModeControl
): String? {
    val terms = when (control) {
        MusicModeControl.REPEAT -> listOf("repeat", "loop")
        MusicModeControl.SHUFFLE -> listOf("shuffle")
    }
    return actions.firstOrNull { action ->
        val searchable = "${action.id} ${action.label}".lowercase()
        terms.any(searchable::contains)
    }?.id
}

private val SessionActivity.priority: Int
    get() = when (this) {
        SessionActivity.PLAYING -> 3
        SessionActivity.TRANSITIONAL -> 2
        SessionActivity.PAUSED -> 1
        SessionActivity.INACTIVE -> 0
    }
