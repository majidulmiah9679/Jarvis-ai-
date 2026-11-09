package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Clean, Simple Native Android System (Material 3 Dark) Palette
val JarvisObsidian = Color(0xFF111318)
val JarvisDeepNavy = Color(0xFF16181E)
val JarvisSurfaceCard = Color(0xFF1D2026)
val JarvisSurfaceElevated = Color(0xFF252932)

// Clean Flat Android System Background Tokens
val AuroraPeachTop = Color(0xFF111318)
val AuroraRoseMid = Color(0xFF111318)
val AuroraVioletDeep = Color(0xFF111318)
val AuroraDarkPlum = Color(0xFF111318)
val AuroraObsidianBottom = Color(0xFF111318)

// Clean Android System Accent Tokens (Google Pixel / Material 3 Blue & Green)
val StudioGoldLight = Color(0xFF8AB4F8)
val StudioGoldWarm = Color(0xFF669DF6)
val StudioVioletAccent = Color(0xFF81C995)
val StudioPurpleVibrant = Color(0xFF4285F4)
val StudioMagentaGlow = Color(0xFF8AB4F8)

// Clean Material 3 Card & Divider Tokens
val StudioGlassFill = Color(0xFF252932)
val StudioGlassBorder = Color(0xFF333842)
val StudioDarkCard = Color(0xFF1C1F26)
val StudioDarkCardBorder = Color(0xFF2D323C)
val StudioSubtleText = Color(0xFF9AA0A6)

val JarvisCyan = Color(0xFF8AB4F8)
val JarvisCyanDim = Color(0xFF5F6368)
val JarvisElectricBlue = Color(0xFF4285F4)
val JarvisIceWhite = Color(0xFFE8EAED)
val JarvisMutedCyan = Color(0xFF9AA0A6)

val JarvisEmerald = Color(0xFF81C995)
val JarvisAmber = Color(0xFFFDD663)
val JarvisCrimson = Color(0xFFF28B82)
val JarvisViolet = Color(0xFFC58AF9)

enum class JarvisVisualTheme(
    val id: String,
    val titleBn: String,
    val titleEn: String,
    val topBg: Color,
    val midBg: Color,
    val bottomBg: Color,
    val cardBg: Color,
    val cardBorder: Color,
    val primaryAccent: Color,
    val secondaryAccent: Color
) {
    OCEAN_CYBER(
        id = "OCEAN_CYBER",
        titleBn = "অ্যান্ড্রয়েড ব্লু",
        titleEn = "Android Blue",
        topBg = Color(0xFF111318),
        midBg = Color(0xFF111318),
        bottomBg = Color(0xFF111318),
        cardBg = Color(0xFF1C1F26),
        cardBorder = Color(0xFF2D323C),
        primaryAccent = Color(0xFF8AB4F8),
        secondaryAccent = Color(0xFF81C995)
    ),
    EMERALD_MATRIX(
        id = "EMERALD_MATRIX",
        titleBn = "সিস্টেম গ্রিন",
        titleEn = "System Green",
        topBg = Color(0xFF101412),
        midBg = Color(0xFF101412),
        bottomBg = Color(0xFF101412),
        cardBg = Color(0xFF1A211E),
        cardBorder = Color(0xFF2B3731),
        primaryAccent = Color(0xFF81C995),
        secondaryAccent = Color(0xFFA8DAB5)
    ),
    ROYAL_INDIGO(
        id = "ROYAL_INDIGO",
        titleBn = "ম্যাটেরিয়াল পার্পল",
        titleEn = "Material Purple",
        topBg = Color(0xFF131218),
        midBg = Color(0xFF131218),
        bottomBg = Color(0xFF131218),
        cardBg = Color(0xFF1E1C26),
        cardBorder = Color(0xFF322F3E),
        primaryAccent = Color(0xFFD0BCFF),
        secondaryAccent = Color(0xFFCCC2DC)
    ),
    SUNSET_GOLD(
        id = "SUNSET_GOLD",
        titleBn = "সিম্পল ডার্ক",
        titleEn = "Simple Dark",
        topBg = Color(0xFF0E0E0E),
        midBg = Color(0xFF0E0E0E),
        bottomBg = Color(0xFF0E0E0E),
        cardBg = Color(0xFF1A1A1A),
        cardBorder = Color(0xFF2E2E2E),
        primaryAccent = Color(0xFFE8EAED),
        secondaryAccent = Color(0xFF8AB4F8)
    )
}
