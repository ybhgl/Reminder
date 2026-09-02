package com.ybhgl.reminder.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.ybhgl.reminder.data.AppThemeOption
import com.ybhgl.reminder.data.AppColorPalette

private val PurpleLightColorScheme = lightColorScheme(
    primary = Color(0xFF6750A4),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF21005D),
    secondary = Color(0xFF625B71),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE8DDFF),
    onSecondaryContainer = Color(0xFF1D192B),
    tertiary = Color(0xFF7D5260),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD8E4),
    onTertiaryContainer = Color(0xFF31111D),
    background = Color(0xFFFEF7FF),
    onBackground = Color(0xFF1D1B20),
    surface = Color(0xFFFDF7FF),
    onSurface = Color(0xFF1D1B20),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),
    surfaceContainerLow = Color(0xFFF5EEFA),
    surfaceContainerHigh = Color(0xFFE7DFEF)
)

private val PurpleDarkColorScheme = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFFCCC2DC),
    onSecondary = Color(0xFF332D41),
    secondaryContainer = Color(0xFF4A4458),
    onSecondaryContainer = Color(0xFFE8DDFF),
    tertiary = Color(0xFFEFB8C8),
    onTertiary = Color(0xFF492532),
    tertiaryContainer = Color(0xFF633B48),
    onTertiaryContainer = Color(0xFFFFD8E4),
    background = Color(0xFF141218),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF141218),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    surfaceContainerLow = Color(0xFF1C1A22),
    surfaceContainerHigh = Color(0xFF25222B)
)

private val BlueLightColorScheme = lightColorScheme(
    primary = Color(0xFF465D91),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD7DAEB),
    onPrimaryContainer = Color(0xFF00153D),
    secondary = Color(0xFF535F70),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD7E3F7),
    onSecondaryContainer = Color(0xFF101C2B),
    tertiary = Color(0xFF6B5778),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF2DAFF),
    onTertiaryContainer = Color(0xFF251431),
    background = Color(0xFFFBF9FF),
    onBackground = Color(0xFF1A1B20),
    surface = Color(0xFFFBF9FF),
    onSurface = Color(0xFF1A1B20),
    surfaceVariant = Color(0xFFDFE2EB),
    onSurfaceVariant = Color(0xFF43474E),
    surfaceContainerLow = Color(0xFFD7DAEB),
    surfaceContainerHigh = Color(0xFFC7CBDC)
)

private val BlueDarkColorScheme = darkColorScheme(
    primary = Color(0xFFAEC6FF),
    onPrimary = Color(0xFF142E5F),
    primaryContainer = Color(0xFF2D4578),
    onPrimaryContainer = Color(0xFFD7DAEB),
    secondary = Color(0xFFBBC7DB),
    onSecondary = Color(0xFF253140),
    secondaryContainer = Color(0xFF3B4858),
    onSecondaryContainer = Color(0xFFD7E3F7),
    tertiary = Color(0xFFD7BDE4),
    onTertiary = Color(0xFF3B2948),
    tertiaryContainer = Color(0xFF523F5F),
    onTertiaryContainer = Color(0xFFF2DAFF),
    background = Color(0xFF111318),
    onBackground = Color(0xFFE2E2E9),
    surface = Color(0xFF111318),
    onSurface = Color(0xFFE2E2E9),
    surfaceVariant = Color(0xFF43474E),
    onSurfaceVariant = Color(0xFFC3C7CF),
    surfaceContainerLow = Color(0xFF1A1C22),
    surfaceContainerHigh = Color(0xFF24262E)
)

private val GreenLightColorScheme = lightColorScheme(
    primary = Color(0xFF34693C),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD1DFCD),
    onPrimaryContainer = Color(0xFF002206),
    secondary = Color(0xFF4D6356),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD0E9D9),
    onSecondaryContainer = Color(0xFF0B1F15),
    tertiary = Color(0xFF3B6571),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFBEEAF8),
    onTertiaryContainer = Color(0xFF001F26),
    background = Color(0xFFF8FCF4),
    onBackground = Color(0xFF171D18),
    surface = Color(0xFFF8FCF4),
    onSurface = Color(0xFF171D18),
    surfaceVariant = Color(0xFFDDE5DB),
    onSurfaceVariant = Color(0xFF414942),
    surfaceContainerLow = Color(0xFFD1DFCD),
    surfaceContainerHigh = Color(0xFFC1CFBD)
)

private val GreenDarkColorScheme = darkColorScheme(
    primary = Color(0xFF98D7A4),
    onPrimary = Color(0xFF003916),
    primaryContainer = Color(0xFF1B5126),
    onPrimaryContainer = Color(0xFFD1DFCD),
    secondary = Color(0xFFB4CCBC),
    onSecondary = Color(0xFF20352A),
    secondaryContainer = Color(0xFF364B3F),
    onSecondaryContainer = Color(0xFFD0E9D9),
    tertiary = Color(0xFFA2CEDC),
    onTertiary = Color(0xFF033641),
    tertiaryContainer = Color(0xFF214D59),
    onTertiaryContainer = Color(0xFFBEEAF8),
    background = Color(0xFF0E110F),
    onBackground = Color(0xFFE0E3DE),
    surface = Color(0xFF0E110F),
    onSurface = Color(0xFFE0E3DE),
    surfaceVariant = Color(0xFF414942),
    onSurfaceVariant = Color(0xFFC1C9C0),
    surfaceContainerLow = Color(0xFF161C18),
    surfaceContainerHigh = Color(0xFF212923)
)

private val YellowLightColorScheme = lightColorScheme(
    primary = Color(0xFF7E5719),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE6D7C5),
    onPrimaryContainer = Color(0xFF2B1700),
    secondary = Color(0xFF6B5C3F),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF4E0CC),
    onSecondaryContainer = Color(0xFF241A04),
    tertiary = Color(0xFF3F6652),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFC1ECD3),
    onTertiaryContainer = Color(0xFF002112),
    background = Color(0xFFFEF8F4),
    onBackground = Color(0xFF1E1B16),
    surface = Color(0xFFFEF8F4),
    onSurface = Color(0xFF1E1B16),
    surfaceVariant = Color(0xFFE6E3D1),
    onSurfaceVariant = Color(0xFF48473A),
    surfaceContainerLow = Color(0xFFE6D7C5),
    surfaceContainerHigh = Color(0xFFD6C7B5)
)

private val YellowDarkColorScheme = darkColorScheme(
    primary = Color(0xFFF3BF88),
    onPrimary = Color(0xFF452B00),
    primaryContainer = Color(0xFF614000),
    onPrimaryContainer = Color(0xFFE6D7C5),
    secondary = Color(0xFFD7C4A7),
    onSecondary = Color(0xFF3B2E16),
    secondaryContainer = Color(0xFF53452B),
    onSecondaryContainer = Color(0xFFF4E0CC),
    tertiary = Color(0xFFA6D0B0),
    onTertiary = Color(0xFF0E3725),
    tertiaryContainer = Color(0xFF284E3B),
    onTertiaryContainer = Color(0xFFC1ECD3),
    background = Color(0xFF13110F),
    onBackground = Color(0xFFECE0DA),
    surface = Color(0xFF13130F),
    onSurface = Color(0xFFECE0DA),
    surfaceVariant = Color(0xFF48473A),
    onSurfaceVariant = Color(0xFFC9C7B9),
    surfaceContainerLow = Color(0xFF1D1A17),
    surfaceContainerHigh = Color(0xFF272320)
)

private val OrangeLightColorScheme = lightColorScheme(
    primary = Color(0xFF8F4B36),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEAD7CF),
    onPrimaryContainer = Color(0xFF3A0B00),
    secondary = Color(0xFF745B53),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFADBD2),
    onSecondaryContainer = Color(0xFF2A1510),
    tertiary = Color(0xFF596239),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFDDE7B3),
    onTertiaryContainer = Color(0xFF171E00),
    background = Color(0xFFFFF9F6),
    onBackground = Color(0xFF221A15),
    surface = Color(0xFFFFF9F6),
    onSurface = Color(0xFF221A15),
    surfaceVariant = Color(0xFFF1E0D4),
    onSurfaceVariant = Color(0xFF504539),
    surfaceContainerLow = Color(0xFFEAD7CF),
    surfaceContainerHigh = Color(0xFFDAC7BF)
)

private val OrangeDarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFB59E),
    onPrimary = Color(0xFF561E0E),
    primaryContainer = Color(0xFF723422),
    onPrimaryContainer = Color(0xFFEAD7CF),
    secondary = Color(0xFFE3C0B6),
    onSecondary = Color(0xFF422C25),
    secondaryContainer = Color(0xFF5B423A),
    onSecondaryContainer = Color(0xFFFADBD2),
    tertiary = Color(0xFFC1CA99),
    onTertiary = Color(0xFF2C3410),
    tertiaryContainer = Color(0xFF424A24),
    onTertiaryContainer = Color(0xFFC1CA99),
    background = Color(0xFF15110E),
    onBackground = Color(0xFFEDE0DB),
    surface = Color(0xFF15110E),
    onSurface = Color(0xFFEDE0DB),
    surfaceVariant = Color(0xFF504539),
    onSurfaceVariant = Color(0xFFD4C4B5),
    surfaceContainerLow = Color(0xFF1E1A17),
    surfaceContainerHigh = Color(0xFF2A2320)
)

private val PinkLightColorScheme = lightColorScheme(
    primary = Color(0xFF8E4957),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE8D7D7),
    onPrimaryContainer = Color(0xFF3B0715),
    secondary = Color(0xFF73575C),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFAD8DC),
    onSecondaryContainer = Color(0xFF2B151A),
    tertiary = Color(0xFF8D4F38),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDAD0),
    onTertiaryContainer = Color(0xFF370F03),
    background = Color(0xFFFFF9F6),
    onBackground = Color(0xFF221A1B),
    surface = Color(0xFFFFF9F6),
    onSurface = Color(0xFF221A1B),
    surfaceVariant = Color(0xFFF2DDE3),
    onSurfaceVariant = Color(0xFF514347),
    surfaceContainerLow = Color(0xFFE8D7D7),
    surfaceContainerHigh = Color(0xFFD8C7C7)
)

private val PinkDarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFB2BF),
    onPrimary = Color(0xFF561C2A),
    primaryContainer = Color(0xFF723240),
    onPrimaryContainer = Color(0xFFE8D7D7),
    secondary = Color(0xFFDFBFC3),
    onSecondary = Color(0xFF412A2F),
    secondaryContainer = Color(0xFF594045),
    onSecondaryContainer = Color(0xFFFAD8DC),
    tertiary = Color(0xFFFFB59C),
    onTertiary = Color(0xFF54210F),
    tertiaryContainer = Color(0xFF703723),
    onTertiaryContainer = Color(0xFFFFB59C),
    background = Color(0xFF161111),
    onBackground = Color(0xFFECE0E1),
    surface = Color(0xFF161111),
    onSurface = Color(0xFFECE0E1),
    surfaceVariant = Color(0xFF514347),
    onSurfaceVariant = Color(0xFFD5C2C6),
    surfaceContainerLow = Color(0xFF201A1B),
    surfaceContainerHigh = Color(0xFF2B2325)
)

private val CyanLightColorScheme = lightColorScheme(
    primary = Color(0xFF136776),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFC5DDE1),
    onPrimaryContainer = Color(0xFF001F25),
    secondary = Color(0xFF4C6266),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCCE7EC),
    onSecondaryContainer = Color(0xFF051F22),
    tertiary = Color(0xFF4B607C),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD3E4FF),
    onTertiaryContainer = Color(0xFF041C35),
    background = Color(0xFFF6FBFE),
    onBackground = Color(0xFF181C1D),
    surface = Color(0xFFF6FBFE),
    onSurface = Color(0xFF181C1D),
    surfaceVariant = Color(0xFFDAE4E4),
    onSurfaceVariant = Color(0xFF3F4949),
    surfaceContainerLow = Color(0xFFC5DDE1),
    surfaceContainerHigh = Color(0xFFB5CDD1)
)

private val CyanDarkColorScheme = darkColorScheme(
    primary = Color(0xFF8CD2E2),
    onPrimary = Color(0xFF003640),
    primaryContainer = Color(0xFF004E5A),
    onPrimaryContainer = Color(0xFFC5DDE1),
    secondary = Color(0xFFB1CBD0),
    onSecondary = Color(0xFF1E3438),
    secondaryContainer = Color(0xFF344A4F),
    onSecondaryContainer = Color(0xFFCCE7EC),
    tertiary = Color(0xFFB3C8E8),
    onTertiary = Color(0xFF1C314B),
    tertiaryContainer = Color(0xFF334863),
    onTertiaryContainer = Color(0xFFB3C8E8),
    background = Color(0xFF0E1315),
    onBackground = Color(0xFFDFE3E5),
    surface = Color(0xFF0E1315),
    onSurface = Color(0xFFDFE3E5),
    surfaceVariant = Color(0xFF3F4949),
    onSurfaceVariant = Color(0xFFBEC8C8),
    surfaceContainerLow = Color(0xFF171D1F),
    surfaceContainerHigh = Color(0xFF21282A)
)

private val MonochromeLightColorScheme = lightColorScheme(
    primary = Color(0xFF2C2C2C),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE2E2E2),
    onPrimaryContainer = Color(0xFF111111),
    secondary = Color(0xFF5A5F63),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE5E7EC),
    onSecondaryContainer = Color(0xFF181C1E),
    tertiary = Color(0xFF7C7C7C),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFEEEEEE),
    onTertiaryContainer = Color(0xFF222222),
    background = Color(0xFFFAFBFD),
    onBackground = Color(0xFF191C1E),
    surface = Color(0xFFFAFBFD),
    onSurface = Color(0xFF191C1E),
    surfaceVariant = Color(0xFFE2E2E2),
    onSurfaceVariant = Color(0xFF474747),
    surfaceContainerLow = Color(0xFFF1F3F5),
    surfaceContainerHigh = Color(0xFFE5E7EB)
)

private val MonochromeDarkColorScheme = darkColorScheme(
    primary = Color(0xFFE2E2E6),
    onPrimary = Color(0xFF1A1C1E),
    primaryContainer = Color(0xFF2D2E30),
    onPrimaryContainer = Color(0xFFE2E2E6),
    secondary = Color(0xFFC4C6CB),
    onSecondary = Color(0xFF2E3033),
    secondaryContainer = Color(0xFF44474B),
    onSecondaryContainer = Color(0xFFE2E4E9),
    tertiary = Color(0xFF888888),
    onTertiary = Color(0xFF121212),
    tertiaryContainer = Color(0xFF333333),
    onTertiaryContainer = Color(0xFFDDDDDD),
    background = Color(0xFF111315),
    surface = Color(0xFF111315),
    onBackground = Color(0xFFE2E2E6),
    onSurface = Color(0xFFE2E2E6),
    surfaceVariant = Color(0xFF303030),
    onSurfaceVariant = Color(0xFFC7C7C7),
    surfaceContainerLow = Color(0xFF1A1C1E),
    surfaceContainerHigh = Color(0xFF242628)
)

val LocalAppDarkTheme = staticCompositionLocalOf { false }
val LocalCardColoringEnabled = staticCompositionLocalOf { true }

@Composable
fun ReminderTheme(
    themeOption: AppThemeOption = AppThemeOption.SYSTEM,
    usePureBlack: Boolean = false,
    cardColoringEnabled: Boolean = true,
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    colorPalette: AppColorPalette = AppColorPalette.PURPLE,
    customColorSeed: Color = Color(0xFF6750A4),
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeOption) {
        AppThemeOption.SYSTEM -> isSystemInDarkTheme()
        AppThemeOption.LIGHT -> false
        AppThemeOption.DARK -> true
    }

    val context = LocalContext.current
    val baseColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        else -> {
            if (darkTheme) {
                when (colorPalette) {
                    AppColorPalette.BLUE -> BlueDarkColorScheme
                    AppColorPalette.GREEN -> GreenDarkColorScheme
                    AppColorPalette.YELLOW -> YellowDarkColorScheme
                    AppColorPalette.ORANGE -> OrangeDarkColorScheme
                    AppColorPalette.PURPLE -> PurpleDarkColorScheme
                    AppColorPalette.PINK -> PinkDarkColorScheme
                    AppColorPalette.CYAN -> CyanDarkColorScheme
                    AppColorPalette.MONOCHROME -> MonochromeDarkColorScheme
                    AppColorPalette.CUSTOM -> generateCustomColorScheme(customColorSeed, isDark = true)
                }
            } else {
                when (colorPalette) {
                    AppColorPalette.BLUE -> BlueLightColorScheme
                    AppColorPalette.GREEN -> GreenLightColorScheme
                    AppColorPalette.YELLOW -> YellowLightColorScheme
                    AppColorPalette.ORANGE -> OrangeLightColorScheme
                    AppColorPalette.PURPLE -> PurpleLightColorScheme
                    AppColorPalette.PINK -> PinkLightColorScheme
                    AppColorPalette.CYAN -> CyanLightColorScheme
                    AppColorPalette.MONOCHROME -> MonochromeLightColorScheme
                    AppColorPalette.CUSTOM -> generateCustomColorScheme(customColorSeed, isDark = false)
                }
            }
        }
    }

    val colorScheme = if (usePureBlack && darkTheme) {
        baseColorScheme.copy(
            background = Color.Black,
            surface = Color(0xFF050505),
            surfaceVariant = Color(0xFF121212),
            surfaceTint = Color.Black
        )
    } else {
        baseColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as android.app.Activity).window
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(
        LocalAppDarkTheme provides darkTheme,
        LocalCardColoringEnabled provides cardColoringEnabled
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

/**
 * 核心自定义色彩计算：基于 HSV 空间模型对 Seed Color 进行亮度/饱和度调配，
 * 动态输出轻量级、高度和谐、符合高对比度规范的 Material 3 ColorScheme。
 */
private fun generateCustomColorScheme(seedColor: Color, isDark: Boolean): androidx.compose.material3.ColorScheme {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(
        android.graphics.Color.argb(
            (seedColor.alpha * 255).toInt(),
            (seedColor.red * 255).toInt(),
            (seedColor.green * 255).toInt(),
            (seedColor.blue * 255).toInt()
        ),
        hsv
    )
    val hue = hsv[0]
    val sat = hsv[1]
    val value = hsv[2]

    // 辅助色彩变换函数
    fun fromHsv(h: Float, s: Float, v: Float): Color {
        val colorInt = android.graphics.Color.HSVToColor(floatArrayOf(h, s.coerceIn(0f, 1f), v.coerceIn(0f, 1f)))
        return Color(colorInt)
    }

    return if (!isDark) {
        // --- 浅色模式自定义 Scheme ---
        val primary = seedColor
        val onPrimary = Color.White
        val primaryContainer = fromHsv(hue, sat * 0.25f, 0.96f)
        val onPrimaryContainer = fromHsv(hue, sat.coerceAtLeast(0.7f), 0.3f)
        val secondary = fromHsv((hue + 15) % 360f, sat * 0.4f, value * 0.7f)
        val onSecondary = Color.White
        val secondaryContainer = fromHsv((hue + 15) % 360f, sat * 0.15f, 0.96f)
        val onSecondaryContainer = fromHsv((hue + 15) % 360f, sat.coerceAtLeast(0.7f), 0.35f)
        val tertiary = fromHsv((hue + 120) % 360f, sat * 0.5f, value * 0.6f)
        val onTertiary = Color.White
        val tertiaryContainer = fromHsv((hue + 120) % 360f, sat * 0.15f, 0.97f)
        val onTertiaryContainer = fromHsv((hue + 120) % 360f, sat.coerceAtLeast(0.7f), 0.3f)
        
        // 柔和、洁净、并融入微弱原色调的背景与表面
        val background = fromHsv(hue, sat * 0.03f, 0.99f)
        val onBackground = Color(0xFF1D1B20)
        val surface = fromHsv(hue, sat * 0.03f, 0.99f)
        val onSurface = Color(0xFF1D1B20)
        val surfaceVariant = fromHsv(hue, sat * 0.08f, 0.93f)
        val onSurfaceVariant = Color(0xFF49454F)

        val surfaceContainerLow = fromHsv(hue, sat * 0.04f, 0.97f)
        val surfaceContainerHigh = fromHsv(hue, sat * 0.06f, 0.93f)

        lightColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            secondary = secondary,
            onSecondary = onSecondary,
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = onSecondaryContainer,
            tertiary = tertiary,
            onTertiary = onTertiary,
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = onTertiaryContainer,
            background = background,
            onBackground = onBackground,
            surface = surface,
            onSurface = onSurface,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = onSurfaceVariant,
            surfaceContainerLow = surfaceContainerLow,
            surfaceContainerHigh = surfaceContainerHigh
        )
    } else {
        // --- 深色模式自定义 Scheme ---
        val primary = fromHsv(hue, sat * 0.5f, 0.9f)
        val onPrimary = fromHsv(hue, sat.coerceAtLeast(0.7f), 0.3f)
        val primaryContainer = fromHsv(hue, sat.coerceAtLeast(0.6f), 0.45f)
        val onPrimaryContainer = fromHsv(hue, sat * 0.2f, 0.97f)
        val secondary = fromHsv((hue + 15) % 360f, sat * 0.35f, 0.8f)
        val onSecondary = fromHsv((hue + 15) % 360f, sat.coerceAtLeast(0.7f), 0.25f)
        val secondaryContainer = fromHsv((hue + 15) % 360f, sat * 0.5f, 0.4f)
        val onSecondaryContainer = fromHsv((hue + 15) % 360f, sat * 0.15f, 0.97f)
        val tertiary = fromHsv((hue + 120) % 360f, sat * 0.4f, 0.85f)
        val onTertiary = fromHsv((hue + 120) % 360f, sat.coerceAtLeast(0.7f), 0.3f)
        val tertiaryContainer = fromHsv((hue + 120) % 360f, sat * 0.5f, 0.45f)
        val onTertiaryContainer = fromHsv((hue + 120) % 360f, sat * 0.15f, 0.98f)

        // 舒适、低反差、充满沉浸感的深暗色背景与表面
        val background = Color(0xFF121118)
        val onBackground = Color(0xFFE6E1E5)
        val surface = Color(0xFF121118)
        val onSurface = Color(0xFFE6E1E5)
        val surfaceVariant = fromHsv(hue, sat * 0.12f, 0.32f)
        val onSurfaceVariant = Color(0xFFCAC4D0)

        val surfaceContainerLow = Color(0xFF1B1922)
        val surfaceContainerHigh = Color(0xFF26242F)

        darkColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            secondary = secondary,
            onSecondary = onSecondary,
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = onSecondaryContainer,
            tertiary = tertiary,
            onTertiary = onTertiary,
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = onTertiaryContainer,
            background = background,
            onBackground = onBackground,
            surface = surface,
            onSurface = onSurface,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = onSurfaceVariant,
            surfaceContainerLow = surfaceContainerLow,
            surfaceContainerHigh = surfaceContainerHigh
        )
    }
}
