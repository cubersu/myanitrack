package com.myanitrack.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.myanitrack.core.designsystem.theme.ScoreColors

/** Kullanicinin verdigi puan rozeti. Puan yoksa hicbir sey cizilmez. */
@Composable
fun ScoreBadge(
    score: Int,
    modifier: Modifier = Modifier,
) {
    val color = ScoreColors.forScore(score) ?: return
    Text(
        text = score.toString(),
        style = MaterialTheme.typography.labelMedium,
        color = Color.White,
        modifier = modifier
            .background(color, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}
