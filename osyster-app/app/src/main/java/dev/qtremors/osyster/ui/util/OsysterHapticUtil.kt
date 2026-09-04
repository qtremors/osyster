package dev.qtremors.osyster.ui.util

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View

object OsysterHapticUtil {

    fun performTick(view: View, enabled: Boolean = true) {
        if (!enabled) return
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }

    fun performVirtualKey(view: View, enabled: Boolean = true) {
        if (!enabled) return
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    }

    fun performSegmentTick(view: View, enabled: Boolean = true) {
        if (!enabled) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            view.performHapticFeedback(HapticFeedbackConstants.SEGMENT_FREQUENT_TICK)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }
    }
}
