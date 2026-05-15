package com.app.awareness.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// ── Color Scheme ──────────────────────────────────────────────────────────────
private val BlinkDarkColorScheme = darkColorScheme(
    // Backgrounds
    background         = Background,
    surface            = Surface,
    surfaceVariant     = SurfaceVariant,

    // Primary = Accent (lime)
    primary            = Accent,
    onPrimary          = Background,        // dark text on bright lime button

    // Secondary = Positive (green stats)
    secondary          = Positive,
    onSecondary        = Background,

    // Tertiary = Warning (orange high-usage)
    tertiary           = Warning,
    onTertiary         = Background,

    // Text roles
    onBackground       = TextPrimary,
    onSurface          = TextPrimary,
    onSurfaceVariant   = TextSecondary,

    // Borders / dividers
    outline            = TextMuted,
    outlineVariant     = SurfaceVariant,
)

// ── Typography — mapped to Material3 type-scale slots ─────────────────────────
private val BlinkTypography = Typography(
    displayLarge  = DisplayStyle,   // 48sp / Light  — big screen-time number
    titleMedium   = TitleStyle,     // 22sp / SemiBold — section headings
    bodyMedium    = BodyStyle,      // 15sp / Normal  — body text
    bodySmall     = CaptionStyle,   // 12sp / Normal  — captions (TextSecondary baked in)
    labelSmall    = LabelStyle,     // 11sp / Medium  — uppercase tracking labels
)

// ── Theme Entry Point ─────────────────────────────────────────────────────────
@Composable
fun BlinkTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BlinkDarkColorScheme,
        typography  = BlinkTypography,
        content     = content,
    )
}
