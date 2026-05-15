package com.app.awareness.ui

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.awareness.data.AppDatabase
import com.app.awareness.ui.theme.Accent
import com.app.awareness.ui.theme.Background
import com.app.awareness.ui.theme.BodyStyle
import com.app.awareness.ui.theme.CaptionStyle
import com.app.awareness.ui.theme.LabelStyle
import com.app.awareness.ui.theme.Positive
import com.app.awareness.ui.theme.Surface
import com.app.awareness.ui.theme.SurfaceVariant
import com.app.awareness.ui.theme.TextMuted
import com.app.awareness.ui.theme.TextPrimary
import com.app.awareness.ui.theme.TextSecondary
import com.app.awareness.ui.theme.TitleStyle
import com.app.awareness.ui.theme.Warning
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.foundation.WindowInsets
import androidx.compose.foundation.layout.statusBars

private const val PREFS_NAME = "awareness_settings"

/**
 * Screen 5 — Settings — FRONTEND.md
 *
 * All values persisted to SharedPreferences under [PREFS_NAME].
 * Keys:
 *   overlay_delay      — "1" | "2" | "off"
 *   show_dynamic_pill  — Boolean
 *   weekly_report_day  — "Monday" … "Sunday"
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val prefs   = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    val scope   = rememberCoroutineScope()

    // ── State (read initial values from SharedPreferences) ────────────────────

    var overlayIndex by remember {
        mutableIntStateOf(when (prefs.getString("overlay_delay", "1")) {
            "1"  -> 0; "2" -> 1; else -> 2
        })
    }

    var showPill by remember {
        mutableStateOf(prefs.getBoolean("show_dynamic_pill", true))
    }

    var reportDay by remember {
        mutableStateOf(prefs.getString("weekly_report_day", "Monday") ?: "Monday")
    }

    var dayDropdownExpanded by remember { mutableStateOf(false) }
    var showResetDialog     by remember { mutableStateOf(false) }

    // ── Permission status (live check at composition) ─────────────────────────

    val hasUsageStats   = remember { checkUsageStatsPermission(context) }
    val hasNotifListener = remember { checkNotificationListenerPermission(context) }
    val hasOverlay      = remember { Settings.canDrawOverlays(context) }

    // ── UI ────────────────────────────────────────────────────────────────────

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(top = 24.dp, bottom = 48.dp),
    ) {
        Text(text = "Settings", style = TitleStyle, color = TextPrimary)

        Spacer(modifier = Modifier.height(28.dp))

        // ── Behaviour ─────────────────────────────────────────────────────────
        SectionHeader("BEHAVIOUR")
        Spacer(modifier = Modifier.height(12.dp))

        // Overlay delay
        SettingsCard {
            Text(text = "Pause overlay delay", style = BodyStyle, color = TextPrimary)
            Spacer(modifier = Modifier.height(10.dp))
            val overlayOptions = listOf("1 sec", "2 sec", "Off")
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                overlayOptions.forEachIndexed { index, label ->
                    SegmentedButton(
                        shape    = SegmentedButtonDefaults.itemShape(index, overlayOptions.size),
                        selected = index == overlayIndex,
                        onClick  = {
                            overlayIndex = index
                            val value = when (index) { 0 -> "1"; 1 -> "2"; else -> "off" }
                            prefs.edit().putString("overlay_delay", value).apply()
                        },
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor   = Accent.copy(alpha = 0.15f),
                            activeContentColor     = Accent,
                            activeBorderColor      = Accent,
                            inactiveContainerColor = SurfaceVariant,
                            inactiveContentColor   = TextSecondary,
                            inactiveBorderColor    = TextMuted,
                        ),
                    ) { Text(label, style = LabelStyle) }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Dynamic pill toggle
        SettingsCard {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Show dynamic pill", style = BodyStyle, color = TextPrimary)
                    Text(
                        text  = "Mini notification shown on app open and session end",
                        style = CaptionStyle,
                        color = TextMuted,
                    )
                }
                Switch(
                    checked         = showPill,
                    onCheckedChange = {
                        showPill = it
                        prefs.edit().putBoolean("show_dynamic_pill", it).apply()
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor  = Background,
                        checkedTrackColor  = Accent,
                        uncheckedThumbColor = TextSecondary,
                        uncheckedTrackColor = SurfaceVariant,
                    ),
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Schedule ──────────────────────────────────────────────────────────
        SectionHeader("SCHEDULE")
        Spacer(modifier = Modifier.height(12.dp))

        SettingsCard {
            Text(text = "Weekly report day", style = BodyStyle, color = TextPrimary)
            Spacer(modifier = Modifier.height(10.dp))

            val days = listOf(
                "Monday", "Tuesday", "Wednesday", "Thursday",
                "Friday", "Saturday", "Sunday",
            )

            ExposedDropdownMenuBox(
                expanded        = dayDropdownExpanded,
                onExpandedChange = { dayDropdownExpanded = it },
            ) {
                OutlinedTextField(
                    value            = reportDay,
                    onValueChange    = {},
                    readOnly         = true,
                    singleLine       = true,
                    trailingIcon     = { ExposedDropdownMenuDefaults.TrailingIcon(dayDropdownExpanded) },
                    modifier         = Modifier.fillMaxWidth().menuAnchor(),
                    colors           = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = Accent,
                        unfocusedBorderColor = TextMuted,
                        focusedTextColor     = TextPrimary,
                        unfocusedTextColor   = TextPrimary,
                        focusedContainerColor  = SurfaceVariant,
                        unfocusedContainerColor = SurfaceVariant,
                    ),
                    textStyle = BodyStyle,
                )
                ExposedDropdownMenu(
                    expanded        = dayDropdownExpanded,
                    onDismissRequest = { dayDropdownExpanded = false },
                    containerColor  = Surface,
                ) {
                    days.forEach { day ->
                        DropdownMenuItem(
                            text    = { Text(day, style = BodyStyle, color = TextPrimary) },
                            onClick = {
                                reportDay = day
                                dayDropdownExpanded = false
                                prefs.edit().putString("weekly_report_day", day).apply()
                            },
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Permissions ───────────────────────────────────────────────────────
        SectionHeader("PERMISSIONS")
        Spacer(modifier = Modifier.height(12.dp))

        SettingsCard {
            PermissionRow(
                label     = "Usage Access",
                subtitle  = "Required for screen time tracking",
                isGranted = hasUsageStats,
                onTap     = {
                    context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                },
            )
            Spacer(modifier = Modifier.height(14.dp))
            PermissionRow(
                label     = "Notification Access",
                subtitle  = "Required for notification counting",
                isGranted = hasNotifListener,
                onTap     = {
                    context.startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
                },
            )
            Spacer(modifier = Modifier.height(14.dp))
            PermissionRow(
                label     = "Display Over Apps",
                subtitle  = "Required for pause overlay and dynamic pill",
                isGranted = hasOverlay,
                onTap     = {
                    context.startActivity(
                        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                               Uri.parse("package:${context.packageName}"))
                    )
                },
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Data ──────────────────────────────────────────────────────────────
        SectionHeader("DATA")
        Spacer(modifier = Modifier.height(12.dp))

        SettingsCard {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(text = "Reset all data", style = BodyStyle, color = Warning)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text  = "Permanently delete all usage history, insights, and session data",
                    style = CaptionStyle,
                    color = TextMuted,
                )
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(
                    onClick = { showResetDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Warning.copy(alpha = 0.08f))
                        .border(1.dp, Warning.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                ) {
                    Text(text = "Reset all data", color = Warning, style = BodyStyle.copy(fontWeight = FontWeight.SemiBold))
                }
            }
        }
    }

    // ── Reset confirmation dialog ─────────────────────────────────────────────
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            containerColor   = Surface,
            titleContentColor = TextPrimary,
            textContentColor  = TextSecondary,
            title = { Text("Delete all data?", style = TitleStyle) },
            text  = {
                Text(
                    "This will permanently erase all usage history, call sessions, " +
                    "media sessions, notifications, and daily summaries. " +
                    "This action cannot be undone.",
                    style = BodyStyle,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showResetDialog = false
                    scope.launch(Dispatchers.IO) {
                        AppDatabase.getInstance(context).clearAllTables()
                    }
                }) {
                    Text("Delete", color = Warning, style = BodyStyle.copy(fontWeight = FontWeight.SemiBold))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
        )
    }
}

// ── Sub-composables ───────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Text(text = title, style = LabelStyle, color = TextMuted)
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceVariant)
            .padding(16.dp),
    ) { content() }
}

@Composable
private fun PermissionRow(
    label: String,
    subtitle: String,
    isGranted: Boolean,
    onTap: () -> Unit,
) {
    Row(
        modifier          = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Status dot
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(if (isGranted) Positive else Warning),
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = label,    style = BodyStyle,   color = TextPrimary)
            Text(text = subtitle, style = CaptionStyle, color = TextMuted)
        }

        if (!isGranted) {
            TextButton(onClick = onTap) {
                Text(
                    text  = "Enable",
                    style = LabelStyle.copy(fontSize = 11.sp),
                    color = Accent,
                )
            }
        }
    }
}

// ── Permission helpers ────────────────────────────────────────────────────────

private fun checkUsageStatsPermission(context: Context): Boolean {
    return try {
        val ops = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val uid = context.applicationInfo.uid
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, uid, context.packageName)
        } else {
            @Suppress("DEPRECATION")
            ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, uid, context.packageName)
        }
        mode == AppOpsManager.MODE_ALLOWED
    } catch (_: Exception) { false }
}

private fun checkNotificationListenerPermission(context: Context): Boolean {
    val flat = Settings.Secure.getString(
        context.contentResolver,
        "enabled_notification_listeners",
    ) ?: return false
    return flat.contains(context.packageName)
}
