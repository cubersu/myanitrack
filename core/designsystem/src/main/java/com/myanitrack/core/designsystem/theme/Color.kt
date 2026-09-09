package com.myanitrack.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// MAL-in kurumsal mavisi (#2E51A2) etrafinda uretilmis Material 3 paleti.
// Dinamik renk kapaliyken ya da Android 12 oncesinde bu palet kullanilir.

private val MalBlue = Color(0xFF2E51A2)
private val MalBlueLight = Color(0xFFAFC6FF)
private val MalBlueDark = Color(0xFF12326E)

internal val LightColors = lightColorScheme(
    primary = MalBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBE1FF),
    onPrimaryContainer = Color(0xFF001551),
    secondary = Color(0xFF5A5D72),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDFE1F9),
    onSecondaryContainer = Color(0xFF171B2C),
    tertiary = Color(0xFF76546D),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFD7F1),
    onTertiaryContainer = Color(0xFF2C1228),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF5F6F8),
    onBackground = Color(0xFF1B1B1F),
    surface = Color(0xFFF5F6F8),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFAFBFC),
    surfaceContainer = Color(0xFFF0F2F5),
    surfaceContainerHigh = Color(0xFFEBEDF1),
    surfaceContainerHighest = Color(0xFFE4E7EC),
    outlineVariant = Color(0xFFDCE0E6),
    onSurface = Color(0xFF1B1B1F),
    surfaceVariant = Color(0xFFE8EBF0),
    onSurfaceVariant = Color(0xFF45464F),
    outline = Color(0xFF767680),
)

internal val DarkColors = darkColorScheme(
    primary = MalBlueLight,
    onPrimary = Color(0xFF002780),
    primaryContainer = MalBlueDark,
    onPrimaryContainer = Color(0xFFDBE1FF),
    secondary = Color(0xFFC3C5DD),
    onSecondary = Color(0xFF2C2F42),
    secondaryContainer = Color(0xFF424659),
    onSecondaryContainer = Color(0xFFDFE1F9),
    tertiary = Color(0xFFE5BAD8),
    onTertiary = Color(0xFF44263E),
    tertiaryContainer = Color(0xFF5C3C55),
    onTertiaryContainer = Color(0xFFFFD7F1),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF111418),
    onBackground = Color(0xFFE4E1E6),
    surface = Color(0xFF111418),
    surfaceContainerLowest = Color(0xFF161A20),
    surfaceContainerLow = Color(0xFF191D23),
    surfaceContainer = Color(0xFF1D2229),
    surfaceContainerHigh = Color(0xFF252B33),
    surfaceContainerHighest = Color(0xFF2D343D),
    outlineVariant = Color(0xFF39414C),
    onSurface = Color(0xFFE4E1E6),
    surfaceVariant = Color(0xFF45464F),
    onSurfaceVariant = Color(0xFFC6C5D0),
    outline = Color(0xFF90909A),
)

/**
 * MAL puanlarini renklendirmek icin kullanilan skala.
 * Yuksek puan yesil, dusuk puan kirmizi; puansiz kayitlar notr kalir.
 */
object ScoreColors {
    val excellent = Color(0xFF2E7D32)
    val good = Color(0xFF689F38)
    val average = Color(0xFFF9A825)
    val poor = Color(0xFFE64A19)
    val bad = Color(0xFFC62828)

    fun forScore(score: Int): Color? = when {
        score >= 9 -> excellent
        score >= 7 -> good
        score >= 5 -> average
        score >= 3 -> poor
        score >= 1 -> bad
        else -> null
    }
}
