package com.kaemis.healthdesk.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

private fun healthDeskLightColors(accentKey: String): ColorScheme {
    val primary = lightAccent(accentKey)
    val primaryContainer = blend(Color(0xFFFFF8EF), primary, 0.18f)
    val secondary = blend(primary, Color(0xFF24312F), 0.16f)
    val secondaryContainer = blend(Color(0xFFF6EFE3), primary, 0.14f)
    val tertiary = blend(primary, Color(0xFFC98261), 0.38f)
    val tertiaryContainer = blend(Color(0xFFFFF8EF), tertiary, 0.2f)
    return lightColorScheme(
        primary = primary,
        onPrimary = Color.White,
        primaryContainer = primaryContainer,
        onPrimaryContainer = readableOn(primaryContainer),
        secondary = secondary,
        onSecondary = readableOn(secondary),
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = readableOn(secondaryContainer),
        tertiary = tertiary,
        onTertiary = readableOn(tertiary),
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = readableOn(tertiaryContainer),
        background = Color(0xFFFFFFFF),
        onBackground = Color(0xFF24312F),
        surface = Color(0xFFFFFDF8),
        onSurface = Color(0xFF24312F),
        surfaceVariant = blend(Color(0xFFF6EFE3), primary, 0.08f),
        onSurfaceVariant = Color(0xFF5D6762),
        outline = Color(0xFFE7DDCE),
        outlineVariant = Color(0xFFE7DDCE),
        error = Color(0xFFD65B59),
    )
}

private fun healthDeskDarkColors(accentKey: String): ColorScheme {
    val primary = darkAccent(accentKey)
    val background = Color(0xFF121416)
    val surface = Color(0xFF1A1E22)
    val surfaceVariant = Color(0xFF242A2F)
    val primaryContainer = blend(surface, primary, 0.16f)
    val secondary = blend(primary, Color(0xFFF4EFE7), 0.18f)
    val secondaryContainer = blend(surface, secondary, 0.2f)
    val tertiary = blend(primary, Color(0xFFD6A15F), 0.32f)
    val tertiaryContainer = blend(surface, tertiary, 0.2f)
    return darkColorScheme(
        primary = primary,
        onPrimary = Color(0xFF1C1F1D),
        primaryContainer = primaryContainer,
        onPrimaryContainer = readableOn(primaryContainer),
        secondary = secondary,
        onSecondary = readableOn(secondary),
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = readableOn(secondaryContainer),
        tertiary = tertiary,
        onTertiary = readableOn(tertiary),
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = readableOn(tertiaryContainer),
        background = background,
        onBackground = Color(0xFFF4EFE7),
        surface = surface,
        onSurface = Color(0xFFF4EFE7),
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = Color(0xFFB8C5BF),
        outline = Color(0xFF3A4349),
        outlineVariant = Color(0xFF30383D),
        error = Color(0xFFF07F7B),
    )
}

@Composable
fun HealthDeskTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accentKey: String = "sage",
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) healthDeskDarkColors(accentKey) else healthDeskLightColors(accentKey),
        content = content,
    )
}

private fun lightAccent(accentKey: String): Color = parseAccent(accentKey) ?: when (accentKey) {
    "mint" -> Color(0xFF43A779)
    "amber" -> Color(0xFFE6A93E)
    "clay" -> Color(0xFFC98261)
    "sky" -> Color(0xFF5C8DB8)
    "lavender" -> Color(0xFF8A7CCF)
    else -> Color(0xFF6E8B74)
}

private fun darkAccent(accentKey: String): Color = parseAccent(accentKey)?.let { blend(it, Color.White, 0.22f) } ?: when (accentKey) {
    "mint" -> Color(0xFF7DD9AD)
    "amber" -> Color(0xFFD9A441)
    "clay" -> Color(0xFFE0A084)
    "sky" -> Color(0xFF93BFE3)
    "lavender" -> Color(0xFFB9ADEB)
    else -> Color(0xFF9AB7A2)
}

private fun parseAccent(accentKey: String): Color? = if (accentKey.startsWith("#")) {
    runCatching { Color(android.graphics.Color.parseColor(accentKey)) }.getOrNull()
} else {
    null
}

private fun blend(base: Color, overlay: Color, amount: Float): Color = Color(
    red = base.red + (overlay.red - base.red) * amount,
    green = base.green + (overlay.green - base.green) * amount,
    blue = base.blue + (overlay.blue - base.blue) * amount,
    alpha = 1f,
)

private fun readableOn(color: Color): Color {
    val dark = Color(0xFF1C1F1D)
    val light = Color.White
    return if (contrastRatio(color, dark) >= contrastRatio(color, light)) dark else light
}

private fun contrastRatio(first: Color, second: Color): Float {
    val firstLuminance = first.luminance()
    val secondLuminance = second.luminance()
    return (maxOf(firstLuminance, secondLuminance) + 0.05f) / (minOf(firstLuminance, secondLuminance) + 0.05f)
}
