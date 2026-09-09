package com.myanitrack.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.myanitrack.core.model.AiringStatus
import com.myanitrack.core.model.SeasonName

@Composable
fun SeasonName.localizedLabel(): String = stringResource(when (this) {
    SeasonName.WINTER -> R.string.season_winter
    SeasonName.SPRING -> R.string.season_spring
    SeasonName.SUMMER -> R.string.season_summer
    SeasonName.FALL -> R.string.season_fall
})

@Composable
fun AiringStatus.localizedLabel(): String = stringResource(when (this) {
    AiringStatus.FINISHED, AiringStatus.FINISHED_PUBLISHING -> R.string.airing_finished
    AiringStatus.AIRING -> R.string.airing_ongoing
    AiringStatus.PUBLISHING -> R.string.airing_publishing
    AiringStatus.NOT_YET_AIRED, AiringStatus.NOT_YET_PUBLISHED -> R.string.airing_upcoming
    AiringStatus.UNKNOWN -> R.string.airing_unknown
})
