package dev.cameronpak.muser1

/** Re-evaluated both when selecting the menu item and when confirming it. */
internal object PowerMenuRequest {
    enum class Action { IGNORE, SERVICE_REQUIRED, CONFIRM, OPEN_POWER_MENU }

    fun decide(recording: Boolean, sending: Boolean, serviceAvailable: Boolean, confirmed: Boolean): Action = when {
        recording || sending -> Action.IGNORE
        !serviceAvailable -> Action.SERVICE_REQUIRED
        !confirmed -> Action.CONFIRM
        else -> Action.OPEN_POWER_MENU
    }
}
