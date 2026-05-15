package com.app.awareness.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.app.awareness.R

// ── Google Fonts Provider ─────────────────────────────────────────────────────
val fontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage   = "com.google.android.gms",
    certificates      = R.array.com_google_android_gms_fonts_certs
)

// ── DM Sans Font Family ───────────────────────────────────────────────────────
private val dmSansFont = GoogleFont("DM Sans")

val DmSansFontFamily = FontFamily(
    Font(googleFont = dmSansFont, fontProvider = fontProvider, weight = FontWeight.Light),
    Font(googleFont = dmSansFont, fontProvider = fontProvider, weight = FontWeight.Normal),
    Font(googleFont = dmSansFont, fontProvider = fontProvider, weight = FontWeight.Medium),
    Font(googleFont = dmSansFont, fontProvider = fontProvider, weight = FontWeight.SemiBold),
)

// ── Type Scale ────────────────────────────────────────────────────────────────

/** 48sp / Light — big numbers (screen time display, weekly totals) */
val DisplayStyle = TextStyle(
    fontFamily = DmSansFontFamily,
    fontWeight = FontWeight.Light,
    fontSize   = 48.sp,
)

/** 22sp / SemiBold — section titles and headings */
val TitleStyle = TextStyle(
    fontFamily = DmSansFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize   = 22.sp,
)

/** 15sp / Normal — body text and descriptions */
val BodyStyle = TextStyle(
    fontFamily = DmSansFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize   = 15.sp,
)

/** 12sp / Normal — secondary metadata, muted text */
val CaptionStyle = TextStyle(
    fontFamily = DmSansFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize   = 12.sp,
    color      = TextSecondary,
)

/** 11sp / Medium — uppercase labels with wide tracking */
val LabelStyle = TextStyle(
    fontFamily    = DmSansFontFamily,
    fontWeight    = FontWeight.Medium,
    fontSize      = 11.sp,
    letterSpacing = 0.1.em,
)
