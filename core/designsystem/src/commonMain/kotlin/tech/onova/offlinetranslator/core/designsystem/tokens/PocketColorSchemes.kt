package tech.onova.offlinetranslator.core.designsystem.tokens

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/** The approved Teal palette (mockups, light and dark), used when dynamic color is off or unsupported. */
public object PocketColorSchemes {
    /** Something only in part, which can still be completed (spec 080's ⚠): amber, readable on light and on dark. */
    public val warningLight: Color = Color(0xFF8B5000)
    public val warningDark: Color = Color(0xFFFFB870)

    public val light: ColorScheme =
        lightColorScheme(
            primary = Color(0xFF006A6A),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFF9CF1F0),
            onPrimaryContainer = Color(0xFF002020),
            inversePrimary = Color(0xFF80D5D4),
            secondary = Color(0xFF4A6363),
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFFCCE8E7),
            onSecondaryContainer = Color(0xFF051F1F),
            tertiary = Color(0xFF4B607C),
            onTertiary = Color(0xFFFFFFFF),
            tertiaryContainer = Color(0xFFD3E4FF),
            onTertiaryContainer = Color(0xFF041C35),
            background = Color(0xFFF8FAFA),
            onBackground = Color(0xFF191C1C),
            surface = Color(0xFFF8FAFA),
            onSurface = Color(0xFF191C1C),
            surfaceVariant = Color(0xFFDAE5E4),
            onSurfaceVariant = Color(0xFF404848),
            surfaceTint = Color(0xFF006A6A),
            inverseSurface = Color(0xFF2D3131),
            inverseOnSurface = Color(0xFFEFF1F1),
            error = Color(0xFFBA1A1A),
            onError = Color(0xFFFFFFFF),
            errorContainer = Color(0xFFFFDAD6),
            onErrorContainer = Color(0xFF410002),
            outline = Color(0xFF707979),
            outlineVariant = Color(0xFFC0C8C8),
            scrim = Color(0xFF000000),
            surfaceBright = Color(0xFFF8FAFA),
            surfaceDim = Color(0xFFD8DADA),
            surfaceContainerLowest = Color(0xFFFFFFFF),
            surfaceContainerLow = Color(0xFFF1F4F4),
            surfaceContainer = Color(0xFFEBEFEF),
            surfaceContainerHigh = Color(0xFFE5E9E9),
            surfaceContainerHighest = Color(0xFFDFE3E3),
        )

    public val dark: ColorScheme =
        darkColorScheme(
            primary = Color(0xFF80D5D4),
            onPrimary = Color(0xFF003737),
            primaryContainer = Color(0xFF004F4F),
            onPrimaryContainer = Color(0xFF9CF1F0),
            inversePrimary = Color(0xFF006A6A),
            secondary = Color(0xFFB0CCCB),
            onSecondary = Color(0xFF1B3534),
            secondaryContainer = Color(0xFF324B4B),
            onSecondaryContainer = Color(0xFFCCE8E7),
            tertiary = Color(0xFFB3C8E8),
            onTertiary = Color(0xFF1C314B),
            tertiaryContainer = Color(0xFF344863),
            onTertiaryContainer = Color(0xFFD3E4FF),
            background = Color(0xFF101414),
            onBackground = Color(0xFFE0E3E3),
            surface = Color(0xFF101414),
            onSurface = Color(0xFFE0E3E3),
            surfaceVariant = Color(0xFF404848),
            onSurfaceVariant = Color(0xFFC0C8C8),
            surfaceTint = Color(0xFF80D5D4),
            inverseSurface = Color(0xFFE0E3E3),
            inverseOnSurface = Color(0xFF2D3131),
            error = Color(0xFFFFB4AB),
            onError = Color(0xFF690005),
            errorContainer = Color(0xFF93000A),
            onErrorContainer = Color(0xFFFFDAD6),
            outline = Color(0xFF8A9292),
            outlineVariant = Color(0xFF404848),
            scrim = Color(0xFF000000),
            surfaceBright = Color(0xFF363A3A),
            surfaceDim = Color(0xFF101414),
            surfaceContainerLowest = Color(0xFF0B0F0F),
            surfaceContainerLow = Color(0xFF191C1C),
            surfaceContainer = Color(0xFF1D2121),
            surfaceContainerHigh = Color(0xFF272B2B),
            surfaceContainerHighest = Color(0xFF323636),
        )
}
