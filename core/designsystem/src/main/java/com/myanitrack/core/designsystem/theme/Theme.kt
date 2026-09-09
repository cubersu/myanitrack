package com.myanitrack.core.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Uygulamanin tek tema girisi.
 *
 * [useDynamicColor] Android 12+ cihazlarda duvar kagidindan turetilen Material You
 * paletini acar; daha eski surumlerde ya da kapatildiginda MAL mavisi kullanilir.
 */
@Composable
fun MyAniTrackTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    useDynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val supportsDynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val context = LocalContext.current

    val accentScheme = when {
        useDynamicColor && supportsDynamicColor && darkTheme -> dynamicDarkColorScheme(context)
        useDynamicColor && supportsDynamicColor -> dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }
    // Neutral surfaces keep covers readable even with vivid wallpaper-derived accents.
    val neutral = if (darkTheme) DarkColors else LightColors
    val colorScheme = accentScheme.copy(
        background = neutral.background,
        onBackground = neutral.onBackground,
        surface = neutral.surface,
        onSurface = neutral.onSurface,
        surfaceVariant = neutral.surfaceVariant,
        onSurfaceVariant = neutral.onSurfaceVariant,
        surfaceContainer = neutral.surfaceContainer,
        surfaceContainerLow = neutral.surfaceContainerLow,
        surfaceContainerLowest = neutral.surfaceContainerLowest,
        surfaceContainerHigh = neutral.surfaceContainerHigh,
        surfaceContainerHighest = neutral.surfaceContainerHighest,
        outlineVariant = neutral.outlineVariant,
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MyAniTrackTypography,
        content = content,
    )
}
