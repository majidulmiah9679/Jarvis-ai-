package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// New Clean Modern "Ocean Cyber & Deep Slate" Default Palette
val JarvisObsidian = Color(0xFF060D18)
val JarvisDeepNavy = Color(0xFF0B172A)
val JarvisSurfaceCard = Color(0xFF101F36)
val JarvisSurfaceElevated = Color(0xFF162A47)

// Crisp Modern Background Gradient Tokens (Replacing the old peach/purple background)
val AuroraPeachTop = Color(0xFF0A2239)
val AuroraRoseMid = Color(0xFF081B31)
val AuroraVioletDeep = Color(0xFF071526)
val AuroraDarkPlum = Color(0xFF06101E)
val AuroraObsidianBottom = Color(0xFF040912)

// Modern Cyan-Emerald & Sky Accent Tokens
val StudioGoldLight = Color(0xFF38BDF8)
val StudioGoldWarm = Color(0xFF0EA5E9)
val StudioVioletAccent = Color(0xFF10B981)
val StudioPurpleVibrant = Color(0xFF0284C7)
val StudioMagentaGlow = Color(0xFF06B6D4)

// Clean Frosted Glass & Card Tokens
val StudioGlassFill = Color(0x22FFFFFF)
val StudioGlassBorder = Color(0x33FFFFFF)
val StudioDarkCard = Color(0xFF0F1D32)
val StudioDarkCardBorder = Color(0xFF1E3557)
val StudioSubtleText = Color(0xFF94A3B8)

val JarvisCyan = Color(0xFF00E5FF)
val JarvisCyanDim = Color(0xFF0284C7)
val JarvisElectricBlue = Color(0xFF3B82F6)
val JarvisIceWhite = Color(0xFFF8FAFC)
val JarvisMutedCyan = Color(0xFF94A3B8)

val JarvisEmerald = Color(0xFF10B981)
val JarvisAmber = Color(0xFFF59E0B)
val JarvisCrimson = Color(0xFFF43F5E)
val JarvisViolet = Color(0xFF6366F1)

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
        titleBn = "সায়ান ব্লু",
        titleEn = "Ocean Cyber",
        topBg = Color(0xFF0B2545),
        midBg = Color(0xFF08182F),
        bottomBg = Color(0xFF040B16),
        cardBg = Color(0xFF0F213A),
        cardBorder = Color(0xFF1E3A63),
        primaryAccent = Color(0xFF00E5FF),
        secondaryAccent = Color(0xFF10B981)
    ),
    EMERALD_MATRIX(
        id = "EMERALD_MATRIX",
        titleBn = "এমেরাল্ড গ্রিন",
        titleEn = "Emerald Neo",
        topBg = Color(0xFF062C21),
        midBg = Color(0xFF051C15),
        bottomBg = Color(0xFF020E0A),
        cardBg = Color(0xFF0A261D),
        cardBorder = Color(0xFF164B3A),
        primaryAccent = Color(0xFF10B981),
        secondaryAccent = Color(0xFF34D399)
    ),
    ROYAL_INDIGO(
        id = "ROYAL_INDIGO",
        titleBn = "রয়্যাল নীল",
        titleEn = "Royal Indigo",
        topBg = Color(0xFF17153B),
        midBg = Color(0xFF0E0D26),
        bottomBg = Color(0xFF070614),
        cardBg = Color(0xFF151433),
        cardBorder = Color(0xFF2B2963),
        primaryAccent = Color(0xFF818CF8),
        secondaryAccent = Color(0xFF38BDF8)
    ),
    SUNSET_GOLD(
        id = "SUNSET_GOLD",
        titleBn = "গোল্ডেন ডার্ক",
        titleEn = "Gold Onyx",
        topBg = Color(0xFF2A1810),
        midBg = Color(0xFF180E0A),
        bottomBg = Color(0xFF0C0705),
        cardBg = Color(0xFF23150E),
        cardBorder = Color(0xFF4A2C1D),
        primaryAccent = Color(0xFFFBBF24),
        secondaryAccent = Color(0xFFF97316)
    )
}
