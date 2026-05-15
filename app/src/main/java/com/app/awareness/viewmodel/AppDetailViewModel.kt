package com.app.awareness.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.app.awareness.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DailyUsageStat(
    val date: String,
    val minutes: Int,
)

class AppDetailViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {

    private val ctx = application.applicationContext
    private val db  = AppDatabase.getInstance(ctx)
    private val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private val prefs = ctx.getSharedPreferences("app_limits", Context.MODE_PRIVATE)

    // Package name injected via navigation argument
    private val _selectedApp = MutableStateFlow(
        savedStateHandle.get<String>("packageName") ?: ""
    )

    // ── StateFlows (FRONTEND.md ViewModel Structure) ───────────────────────────

    /** Currently selected app package name. */
    val selectedApp: StateFlow<String> = _selectedApp

    /** Daily usage for the last 30 days for the selected app. */
    val dailyUsage: StateFlow<List<DailyUsageStat>> = db.appUsageDao()
        .queryByDateRange(thirtyDaysAgo(), today())
        .map { entities ->
            entities
                .filter { it.packageName == _selectedApp.value }
                .map { DailyUsageStat(it.date, it.totalMinutes) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Current soft daily limit in minutes. 0 = no limit. */
    val currentLimit: StateFlow<Int> = MutableStateFlow(
        prefs.getInt("limit_${_selectedApp.value}", 0)
    )

    // ── Actions ───────────────────────────────────────────────────────────────

    /**
     * Persist the soft daily limit for the selected app.
     * 0 = no limit (overlay won't fire for this app).
     */
    fun setLimit(minutes: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            prefs.edit().putInt("limit_${_selectedApp.value}", minutes).apply()
            (currentLimit as MutableStateFlow).value = minutes
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun today() = fmt.format(Date())
    private fun thirtyDaysAgo() = fmt.format(
        Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -29) }.time
    )
}
