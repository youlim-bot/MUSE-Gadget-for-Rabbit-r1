package dev.cameronpak.muser1

import org.junit.Assert.assertEquals
import org.junit.Test

class PowerMenuRequestTest {
    @Test fun missingServiceShowsGuidanceWithoutConfirmation() {
        assertEquals(PowerMenuRequest.Action.SERVICE_REQUIRED,
            PowerMenuRequest.decide(recording = false, sending = false, serviceAvailable = false, confirmed = false))
    }

    @Test fun recordingIgnoresRequestBeforeAndAfterConfirmationEvenWithoutService() {
        for (available in listOf(false, true)) for (confirmed in listOf(false, true)) {
            assertEquals(PowerMenuRequest.Action.IGNORE,
                PowerMenuRequest.decide(recording = true, sending = false, serviceAvailable = available, confirmed = confirmed))
        }
    }

    @Test fun sendingIgnoresRequestBeforeAndAfterConfirmationEvenWithoutService() {
        for (recording in listOf(false, true)) for (available in listOf(false, true)) for (confirmed in listOf(false, true)) {
            assertEquals(PowerMenuRequest.Action.IGNORE,
                PowerMenuRequest.decide(recording = recording, sending = true, serviceAvailable = available, confirmed = confirmed))
        }
    }

    @Test fun availableServiceRequiresConfirmation() {
        assertEquals(PowerMenuRequest.Action.CONFIRM,
            PowerMenuRequest.decide(recording = false, sending = false, serviceAvailable = true, confirmed = false))
    }

    @Test fun confirmedIdleRequestOpensSystemPowerMenu() {
        assertEquals(PowerMenuRequest.Action.OPEN_POWER_MENU,
            PowerMenuRequest.decide(recording = false, sending = false, serviceAvailable = true, confirmed = true))
    }

    @Test fun serviceLostWhileConfirmingShowsGuidanceInsteadOfRequestingGlobalAction() {
        assertEquals(PowerMenuRequest.Action.SERVICE_REQUIRED,
            PowerMenuRequest.decide(recording = false, sending = false, serviceAvailable = false, confirmed = true))
    }
}
