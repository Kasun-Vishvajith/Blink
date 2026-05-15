package com.app.awareness.ui

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.WindowInsets
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.app.awareness.ui.theme.Accent
import com.app.awareness.ui.theme.Background
import com.app.awareness.ui.theme.BodyStyle
import com.app.awareness.ui.theme.CaptionStyle
import com.app.awareness.ui.theme.DisplayStyle
import com.app.awareness.ui.theme.LabelStyle
import com.app.awareness.ui.theme.Positive
import com.app.awareness.ui.theme.SurfaceVariant
import com.app.awareness.ui.theme.TextMuted
import com.app.awareness.ui.theme.TextPrimary
import com.app.awareness.ui.theme.TextSecondary
import com.app.awareness.ui.theme.TitleStyle
import com.app.awareness.ui.theme.Warning
import kotlinx.coroutines.launch

private const val ONBOARDING_PREFS  = "awareness_settings"
private const val KEY_ONBOARDING_OK = "onboarding_complete"

/**
 * Screen 1 — Onboarding — FRONTEND.md
 *
 * 3-page [HorizontalPager]:
 *   Page 1 — "What is Blink?"
 *   Page 2 — "What you'll see"
 *   Page 3 — "Let's set you up" (permission request flow)
 *
 * On all permissions granted + "Get started" tapped:
 *   • Saves [KEY_ONBOARDING_OK]=true to SharedPreferences
 *   • Calls [onComplete] → Navigation pops to HomeScreen
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    val context     = LocalContext.current
    val scope       = rememberCoroutineScope()
    val pagerState  = rememberPagerState(pageCount = { 3 })

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        // ── Pages ─────────────────────────────────────────────────────────────
        HorizontalPager(
            state    = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            when (page) {
                0 -> Page1()
                1 -> Page2()
                2 -> Page3(onComplete = {
                    context.getSharedPreferences(ONBOARDING_PREFS, Context.MODE_PRIVATE)
                        .edit().putBoolean(KEY_ONBOARDING_OK, true).apply()
                    onComplete()
                })
            }
        }

        // ── Bottom chrome: dots + Next / Get started ──────────────────────────
        Column(
            modifier          = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Dot indicator
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(3) { index ->
                    val isActive = index == pagerState.currentPage
                    val width by animateDpAsState(
                        targetValue = if (isActive) 24.dp else 8.dp,
                        animationSpec = tween(300),
                        label = "dot_$index",
                    )
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(width)
                            .clip(RoundedCornerShape(100.dp))
                            .background(if (isActive) Accent else TextMuted),
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Page 1 & 2 → "Next"; Page 3 handled inside Page3 composable
            if (pagerState.currentPage < 2) {
                Button(
                    onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape    = RoundedCornerShape(16.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = Accent,
                        contentColor   = Background,
                    ),
                ) {
                    Text("Next", style = BodyStyle.copy(fontWeight = FontWeight.SemiBold))
                }
            }
        }
    }
}

// ── Page 1 — What is Blink? ───────────────────────────────────────────────────

@Composable
private fun Page1() {
    Column(
        modifier            = Modifier.fillMaxSize().padding(horizontal = 32.dp).padding(top = 80.dp, bottom = 160.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Hero icon with glow rings
        Box(contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.size(160.dp).clip(CircleShape).background(Accent.copy(alpha = 0.06f)))
            Box(modifier = Modifier.size(120.dp).clip(CircleShape).background(Accent.copy(alpha = 0.12f)))
            Box(
                modifier         = Modifier.size(80.dp).clip(CircleShape).background(Accent.copy(alpha = 0.20f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(imageVector = Icons.Outlined.Visibility, contentDescription = null, tint = Accent, modifier = Modifier.size(36.dp))
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text      = "Your phone knows\nmore than you think",
            style     = DisplayStyle.copy(fontSize = 32.sp, fontWeight = FontWeight.W300),
            color     = TextPrimary,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text      = "Blink watches how you use your phone — quietly, privately, and honestly.",
            style     = BodyStyle,
            color     = TextSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

// ── Page 2 — What you'll see ─────────────────────────────────────────────────

private data class Feature(val icon: ImageVector, val title: String, val body: String)

@Composable
private fun Page2() {
    val features = listOf(
        Feature(Icons.Outlined.Timer,       "Screen time by app",     "See exactly where your time goes, down to the minute."),
        Feature(Icons.Outlined.PauseCircle, "Mindful micro-pauses",   "A gentle pause before opening your most-used apps."),
        Feature(Icons.Outlined.BarChart,    "Weekly wrapped",         "A beautiful story-card summary of your full week."),
    )

    Column(
        modifier            = Modifier.fillMaxSize().padding(horizontal = 32.dp).padding(top = 80.dp, bottom = 160.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text      = "What you'll see",
            style     = DisplayStyle.copy(fontSize = 32.sp, fontWeight = FontWeight.W300),
            color     = TextPrimary,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(40.dp))

        features.forEach { feature ->
            FeatureRow(feature)
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun FeatureRow(feature: Feature) {
    Row(
        modifier          = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier         = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(Accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = feature.icon, contentDescription = null, tint = Accent, modifier = Modifier.size(22.dp))
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = feature.title, style = BodyStyle.copy(fontWeight = FontWeight.SemiBold), color = TextPrimary)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = feature.body, style = CaptionStyle, color = TextSecondary)
        }
    }
}

// ── Page 3 — Let's set you up ─────────────────────────────────────────────────

@Composable
private fun Page3(onComplete: () -> Unit) {
    val context       = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Re-check permissions whenever the activity resumes (user returns from Settings)
    var usageGranted   by remember { mutableStateOf(checkUsageStatsPermission(context)) }
    var overlayGranted by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var notifGranted   by remember { mutableStateOf(checkNotifListenerPermission(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                usageGranted   = checkUsageStatsPermission(context)
                overlayGranted = Settings.canDrawOverlays(context)
                notifGranted   = checkNotifListenerPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val allGranted = usageGranted && overlayGranted && notifGranted

    Column(
        modifier            = Modifier.fillMaxSize().padding(horizontal = 32.dp).padding(top = 80.dp, bottom = 160.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text      = "Let's set you up",
            style     = DisplayStyle.copy(fontSize = 32.sp, fontWeight = FontWeight.W300),
            color     = TextPrimary,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text      = "Blink needs three permissions to do its job. All data stays on your device.",
            style     = BodyStyle,
            color     = TextSecondary,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(36.dp))

        // Permission rows
        OnboardingPermissionRow(
            label     = "Usage Access",
            subtitle  = "Tracks which apps you use and for how long",
            isGranted = usageGranted,
            onGrant   = {
                context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            },
        )

        Spacer(modifier = Modifier.height(16.dp))

        OnboardingPermissionRow(
            label     = "Display Over Apps",
            subtitle  = "Shows the pause overlay and dynamic pill",
            isGranted = overlayGranted,
            onGrant   = {
                context.startActivity(
                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                )
            },
        )

        Spacer(modifier = Modifier.height(16.dp))

        OnboardingPermissionRow(
            label     = "Notification Access",
            subtitle  = "Counts daily notifications per app",
            isGranted = notifGranted,
            onGrant   = {
                context.startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
            },
        )

        Spacer(modifier = Modifier.height(40.dp))

        // "Get started" — only active when all 3 permissions are granted
        Button(
            onClick  = { if (allGranted) onComplete() },
            enabled  = allGranted,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape    = RoundedCornerShape(16.dp),
            colors   = ButtonDefaults.buttonColors(
                containerColor         = Accent,
                contentColor           = Background,
                disabledContainerColor = SurfaceVariant,
                disabledContentColor   = TextMuted,
            ),
        ) {
            if (allGranted) {
                Icon(imageVector = Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text("Get started", style = BodyStyle.copy(fontWeight = FontWeight.SemiBold))
        }

        if (!allGranted) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text  = "Grant all three permissions to continue",
                style = CaptionStyle,
                color = TextMuted,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun OnboardingPermissionRow(
    label: String,
    subtitle: String,
    isGranted: Boolean,
    onGrant: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceVariant)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Status dot
        Box(
            modifier = Modifier.size(10.dp).clip(CircleShape)
                .background(if (isGranted) Positive else Warning),
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = label,    style = BodyStyle.copy(fontWeight = FontWeight.Medium), color = TextPrimary)
            Text(text = subtitle, style = CaptionStyle, color = TextMuted)
        }

        if (!isGranted) {
            Spacer(modifier = Modifier.width(8.dp))
            TextButton(onClick = onGrant) {
                Text("Grant", style = LabelStyle, color = Accent)
            }
        }
    }
}

// ── Permission helpers ────────────────────────────────────────────────────────

private fun checkUsageStatsPermission(context: Context): Boolean = try {
    val ops  = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val uid  = context.applicationInfo.uid
    val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, uid, context.packageName)
    else
        @Suppress("DEPRECATION")
        ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, uid, context.packageName)
    mode == AppOpsManager.MODE_ALLOWED
} catch (_: Exception) { false }

private fun checkNotifListenerPermission(context: Context): Boolean {
    val flat = Settings.Secure.getString(
        context.contentResolver, "enabled_notification_listeners"
    ) ?: return false
    return flat.contains(context.packageName)
}
