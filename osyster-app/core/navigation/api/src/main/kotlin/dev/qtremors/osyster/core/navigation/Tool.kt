package dev.qtremors.osyster.core.navigation

/**
 * Every tool listed on the Tools tab. [id] is persisted (favorites, order) and used in
 * [ToolRoute], so it never changes once released.
 */
enum class Tool(val id: String, val group: ToolGroup, val access: ToolAccess = ToolAccess.NONE) {
    DATA_USAGE("data_usage", ToolGroup.USAGE),
    SCREEN_USAGE("screen_usage", ToolGroup.USAGE),
    APP_STOPPER("app_stopper", ToolGroup.USAGE),

    STAY_AWAKE("stay_awake", ToolGroup.UTILITIES),
    BLUETOOTH_BATTERIES("bluetooth_batteries", ToolGroup.UTILITIES),
    FLASHLIGHT("flashlight", ToolGroup.UTILITIES),
    ACTIVITY_LAUNCHER("activity_launcher", ToolGroup.UTILITIES),
    LINK_ACTIONS("link_actions", ToolGroup.UTILITIES),
    WIDGET_STACK("widget_stack", ToolGroup.UTILITIES),
    WIDGETS("widgets", ToolGroup.UTILITIES),
    QUICK_TILES("quick_tiles", ToolGroup.UTILITIES),

    POWER_SETUP("power_setup", ToolGroup.POWER),
    POWER_SAVING("power_saving", ToolGroup.POWER, ToolAccess.SECURE_SETTINGS),
    DISPLAY_SCALE("display_scale", ToolGroup.POWER, ToolAccess.SECURE_SETTINGS),
    STATUS_BAR_ICONS("status_bar_icons", ToolGroup.POWER, ToolAccess.SECURE_SETTINGS),
    NIGHT_LIGHT("night_light", ToolGroup.POWER, ToolAccess.SECURE_SETTINGS),
    SYSTEM_TWEAKS("system_tweaks", ToolGroup.POWER, ToolAccess.SECURE_SETTINGS),
    SHUT_UP("shut_up", ToolGroup.POWER, ToolAccess.SECURE_SETTINGS),
    STANDBY_APPS("standby_apps", ToolGroup.POWER, ToolAccess.SHIZUKU),
    APP_VOLUME("app_volume", ToolGroup.POWER, ToolAccess.SHIZUKU),
    FREEZE("freeze", ToolGroup.POWER, ToolAccess.SHIZUKU);

    companion object {
        private val byId = entries.associateBy(Tool::id)
        fun fromId(id: String): Tool? = byId[id]
    }
}

enum class ToolGroup { USAGE, UTILITIES, POWER }

/**
 * What a tool needs beyond normal permissions. SECURE_SETTINGS works after a one-time ADB grant,
 * or automatically when Shizuku or root grants it; SHIZUKU needs Shizuku or root while in use.
 */
enum class ToolAccess { NONE, SECURE_SETTINGS, SHIZUKU }
