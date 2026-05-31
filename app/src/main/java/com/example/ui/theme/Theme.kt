package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = DarkVibrantBlue,
    onPrimary = DarkOnVibrantBlue,
    primaryContainer = DarkVibrantContainer,
    onPrimaryContainer = DarkOnVibrantContainer,
    background = DarkBg,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = CharcoalQuiet,
    onSurfaceVariant = DarkOnVibrantContainer,
    outline = CoolGrayBorder
  )

private val LightColorScheme =
  lightColorScheme(
    primary = VibrantBlue,
    onPrimary = Color.White,
    primaryContainer = LightBlueContainer,
    onPrimaryContainer = DarkNavy,
    secondary = CharcoalQuiet,
    onSecondary = Color.White,
    secondaryContainer = AuxiliaryBlue,
    onSecondaryContainer = DarkNavy,
    tertiary = SoftSilver,
    onTertiary = CharcoalQuiet,
    background = SoftBlueBg,
    onBackground = CharcoalDarkText,
    surface = Color.White,
    onSurface = CharcoalDarkText,
    surfaceVariant = AuxiliaryBlue,
    onSurfaceVariant = CharcoalQuiet,
    outline = CoolGrayBorder
  )

private val OledColorScheme =
  darkColorScheme(
    primary = DarkVibrantBlue,
    onPrimary = DarkOnVibrantBlue,
    primaryContainer = DarkVibrantContainer,
    onPrimaryContainer = DarkOnVibrantContainer,
    background = Color.Black,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = CharcoalQuiet,
    onSurfaceVariant = DarkOnVibrantContainer,
    outline = CoolGrayBorder
  )

fun generateColorSchemeFromSeed(seedColorInt: Int, isDark: Boolean): ColorScheme {
  val hsl = FloatArray(3)
  androidx.core.graphics.ColorUtils.colorToHSL(seedColorInt, hsl)
  val hue = hsl[0]
  val sat = hsl[1]

  if (isDark) {
    val primaryColor = Color(androidx.core.graphics.ColorUtils.HSLToColor(floatArrayOf(hue, sat.coerceIn(0.5f, 0.85f), 0.65f)))
    val onPrimaryColor = Color.Black
    
    val primaryContainerColor = Color(androidx.core.graphics.ColorUtils.HSLToColor(floatArrayOf(hue, sat.coerceIn(0.2f, 0.5f), 0.16f)))
    val onPrimaryContainerColor = Color(androidx.core.graphics.ColorUtils.HSLToColor(floatArrayOf(hue, sat.coerceIn(0.5f, 0.85f), 0.82f)))
    
    val secondaryColor = Color(androidx.core.graphics.ColorUtils.HSLToColor(floatArrayOf((hue + 30f) % 360f, sat.coerceIn(0.3f, 0.6f), 0.6f)))
    val onSecondaryColor = Color.Black
    val secondaryContainerColor = Color(androidx.core.graphics.ColorUtils.HSLToColor(floatArrayOf((hue + 30f) % 360f, sat.coerceIn(0.15f, 0.45f), 0.2f)))
    val onSecondaryContainerColor = Color(androidx.core.graphics.ColorUtils.HSLToColor(floatArrayOf((hue + 30f) % 360f, sat.coerceIn(0.3f, 0.6f), 0.82f)))
    
    val tertiaryColor = Color(androidx.core.graphics.ColorUtils.HSLToColor(floatArrayOf((hue + 120f) % 360f, sat.coerceIn(0.3f, 0.6f), 0.65f)))
    val onTertiaryColor = Color.Black
    
    val bgColor = Color(androidx.core.graphics.ColorUtils.HSLToColor(floatArrayOf(hue, sat.coerceIn(0f, 0.12f), 0.08f)))
    val surfaceColor = Color(androidx.core.graphics.ColorUtils.HSLToColor(floatArrayOf(hue, sat.coerceIn(0f, 0.1f), 0.12f)))
    val onSurfaceColor = Color(0xFFE2E2E6)
    
    val surfaceVariantColor = Color(androidx.core.graphics.ColorUtils.HSLToColor(floatArrayOf(hue, sat.coerceIn(0f, 0.12f), 0.16f)))
    val onSurfaceVariantColor = Color(0xFFC4C6D0)
    val outlineColor = Color(androidx.core.graphics.ColorUtils.HSLToColor(floatArrayOf(hue, sat.coerceIn(0f, 0.15f), 0.35f)))

    return darkColorScheme(
      primary = primaryColor,
      onPrimary = onPrimaryColor,
      primaryContainer = primaryContainerColor,
      onPrimaryContainer = onPrimaryContainerColor,
      secondary = secondaryColor,
      onSecondary = onSecondaryColor,
      secondaryContainer = secondaryContainerColor,
      onSecondaryContainer = onSecondaryContainerColor,
      tertiary = tertiaryColor,
      onTertiary = onTertiaryColor,
      background = bgColor,
      onBackground = onSurfaceColor,
      surface = surfaceColor,
      onSurface = onSurfaceColor,
      surfaceVariant = surfaceVariantColor,
      onSurfaceVariant = onSurfaceVariantColor,
      outline = outlineColor
    )
  } else {
    val primaryColor = Color(androidx.core.graphics.ColorUtils.HSLToColor(floatArrayOf(hue, sat.coerceIn(0.6f, 0.9f), 0.4f)))
    val onPrimaryColor = Color.White
    
    val primaryContainerColor = Color(androidx.core.graphics.ColorUtils.HSLToColor(floatArrayOf(hue, sat.coerceIn(0.2f, 0.5f), 0.92f)))
    val onPrimaryContainerColor = Color(androidx.core.graphics.ColorUtils.HSLToColor(floatArrayOf(hue, sat.coerceIn(0.6f, 0.9f), 0.15f)))
    
    val secondaryColor = Color(androidx.core.graphics.ColorUtils.HSLToColor(floatArrayOf((hue + 30f) % 360f, sat.coerceIn(0.3f, 0.7f), 0.35f)))
    val onSecondaryColor = Color.White
    val secondaryContainerColor = Color(androidx.core.graphics.ColorUtils.HSLToColor(floatArrayOf((hue + 30f) % 360f, sat.coerceIn(0.15f, 0.45f), 0.9f)))
    val onSecondaryContainerColor = Color(androidx.core.graphics.ColorUtils.HSLToColor(floatArrayOf((hue + 30f) % 360f, sat.coerceIn(0.3f, 0.7f), 0.15f)))
    
    val tertiaryColor = Color(androidx.core.graphics.ColorUtils.HSLToColor(floatArrayOf((hue + 120f) % 360f, sat.coerceIn(0.3f, 0.7f), 0.4f)))
    val onTertiaryColor = Color.White
    
    val bgColor = Color(androidx.core.graphics.ColorUtils.HSLToColor(floatArrayOf(hue, sat.coerceIn(0f, 0.08f), 0.97f)))
    val surfaceColor = Color.White
    val onSurfaceColor = Color(0xFF1A1C1E)
    
    val surfaceVariantColor = Color(androidx.core.graphics.ColorUtils.HSLToColor(floatArrayOf(hue, sat.coerceIn(0f, 0.12f), 0.88f)))
    val onSurfaceVariantColor = Color(0xFF43474E)
    val outlineColor = Color(androidx.core.graphics.ColorUtils.HSLToColor(floatArrayOf(hue, sat.coerceIn(0f, 0.15f), 0.5f)))

    return lightColorScheme(
      primary = primaryColor,
      onPrimary = onPrimaryColor,
      primaryContainer = primaryContainerColor,
      onPrimaryContainer = onPrimaryContainerColor,
      secondary = secondaryColor,
      onSecondary = onSecondaryColor,
      secondaryContainer = secondaryContainerColor,
      onSecondaryContainer = onSecondaryContainerColor,
      tertiary = tertiaryColor,
      onTertiary = onTertiaryColor,
      background = bgColor,
      onBackground = onSurfaceColor,
      surface = surfaceColor,
      onSurface = onSurfaceColor,
      surfaceVariant = surfaceVariantColor,
      onSurfaceVariant = onSurfaceVariantColor,
      outline = outlineColor
    )
  }
}

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  themeType: String = "DEFAULT",
  oledModeEnabled: Boolean = false,
  paletteIndex: Int = 0,
  wallpaperColors: List<Int> = emptyList(),
  content: @Composable () -> Unit,
) {
  var colorScheme =
    when {
      themeType == "DYNAMIC" -> {
        val seedColorInt = if (wallpaperColors.isNotEmpty() && paletteIndex in wallpaperColors.indices) {
          wallpaperColors[paletteIndex]
        } else {
          0xFF1D5AAB.toInt()
        }
        generateColorSchemeFromSeed(seedColorInt, darkTheme)
      }
      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  if (oledModeEnabled && darkTheme) {
    colorScheme = colorScheme.copy(
      background = Color.Black
    )
  }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
