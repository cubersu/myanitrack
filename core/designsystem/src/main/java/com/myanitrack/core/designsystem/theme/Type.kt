package com.myanitrack.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Material 3 varsayilan tipografisi uzerine, liste satirlarinda okunurlugu
 * artiran birkac ince ayar.
 */
internal val MyAniTrackTypography = Typography().let { base ->
    base.copy(
        titleMedium = base.titleMedium.copy(
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Default,
        ),
        labelSmall = base.labelSmall.copy(
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.4.sp,
        ),
        bodySmall = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            lineHeight = 18.sp,
        ),
    )
}
