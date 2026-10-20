package com.safespend.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Tiny SharedPreferences-backed store for the danger threshold (default $7.00). */
class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val _threshold = MutableStateFlow(prefs.getLong(KEY_THRESHOLD, 700L))
    val thresholdCents: StateFlow<Long> = _threshold

    fun setThreshold(cents: Long) {
        prefs.edit().putLong(KEY_THRESHOLD, cents).apply()
        _threshold.value = cents
    }

    private companion object { const val KEY_THRESHOLD = "threshold_cents" }
}
