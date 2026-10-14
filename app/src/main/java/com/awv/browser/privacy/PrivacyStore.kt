package com.awv.browser.privacy

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.awv.browser.ui.home.PrivacyStats
import java.util.Calendar

/**
 * Shield on/off + the "Protected this week" numbers, saved across launches and reset every Monday.
 * Blocked count is exact. Data / time saved are estimates (a blocked request is never downloaded,
 * so its real size is unknown): ~28 KB and ~0.25 s per blocked request.
 */
class PrivacyStore(context: Context) {
    private val prefs = context.getSharedPreferences("privacy", Context.MODE_PRIVATE)

    var shieldOn by mutableStateOf(prefs.getBoolean("shield", true))
        private set

    var stats by mutableStateOf(PrivacyStats())
        private set

    /** Thread-safe copy of [shieldOn] for WebView IO threads (shouldInterceptRequest). */
    @Volatile var shieldFlag: Boolean = shieldOn
        private set

    private var week = prefs.getInt("week", currentWeek())
    private var blocked = prefs.getInt("blocked", 0)

    init {
        rollWeek()
        publish()
    }

    fun toggleShield() {
        shieldOn = !shieldOn
        shieldFlag = shieldOn
        prefs.edit().putBoolean("shield", shieldOn).apply()
    }

    /** Main thread only. */
    fun recordBlocked() {
        rollWeek()
        blocked++
        prefs.edit().putInt("blocked", blocked).apply()
        publish()
    }

    private fun rollWeek() {
        val now = currentWeek()
        if (now != week) {
            week = now
            blocked = 0
            prefs.edit().putInt("week", week).putInt("blocked", 0).apply()
        }
    }

    private fun publish() {
        stats = PrivacyStats(
            trackersBlocked = blocked,
            dataSavedMb = (blocked * 28L / 1024).toInt(),
            timeSavedMin = (blocked * 0.25 / 60).toInt()
        )
    }

    private fun currentWeek(): Int {
        val c = Calendar.getInstance().apply { firstDayOfWeek = Calendar.MONDAY; minimalDaysInFirstWeek = 4 }
        return c.getWeekYear() * 100 + c.get(Calendar.WEEK_OF_YEAR)
    }
}
