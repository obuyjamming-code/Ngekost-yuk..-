package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryTealLight,
    onPrimary = PrimaryContainerTealDark,
    primaryContainer = PrimaryContainerTealDark,
    onPrimaryContainer = PrimaryContainerMint,
    secondary = SecondaryCoralLight,
    onSecondary = SecondaryContainerCoralDark,
    secondaryContainer = SecondaryContainerCoralDark,
    onSecondaryContainer = SecondaryContainerPeach,
    tertiary = TertiaryAmberLight,
    onTertiary = OnTertiaryDark,
    background = BackgroundDarkSlate,
    onBackground = OnBackgroundDark,
    surface = SurfaceDarkSlate,
    onSurface = OnBackgroundDark,
    surfaceVariant = SurfaceVariantDarkSlate,
    onSurfaceVariant = OnSurfaceMutedDark,
    outline = OutlineDark,
    error = ErrorCrimson
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryTeal,
    onPrimary = OnPrimaryCream,
    primaryContainer = PrimaryContainerMint,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = SecondaryCoral,
    onSecondary = OnSecondaryWhite,
    secondaryContainer = SecondaryContainerPeach,
    onSecondaryContainer = OnSecondaryContainerDark,
    tertiary = TertiaryAmber,
    onTertiary = OnTertiaryDark,
    tertiaryContainer = TertiaryContainerGold,
    onTertiaryContainer = OnTertiaryContainerDark,
    background = BackgroundAlabaster,
    onBackground = OnBackgroundCharcoal,
    surface = SurfaceWhite,
    onSurface = OnBackgroundCharcoal,
    surfaceVariant = SurfaceVariantSand,
    onSurfaceVariant = OnSurfaceMuted,
    outline = OutlineSubtle,
    error = ErrorCrimson
)

val NgekostShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun NgekostYukTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = NgekostShapes,
        content = content
    )
}
