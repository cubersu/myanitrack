package com.myanitrack.feature.mylist.component

import com.myanitrack.core.domain.schedule.NextEpisodeCalculator
import com.myanitrack.core.model.AiringStatus
import com.myanitrack.core.model.BroadcastInfo
import com.myanitrack.core.model.MediaNode
import java.time.Duration
import java.time.Instant
import java.util.Locale

internal fun airingCountdown(node: MediaNode, broadcast: BroadcastInfo?, now: Instant): String? {
    if (!node.mediaType.isAnime || node.airingStatus != AiringStatus.AIRING || broadcast == null) return null
    val next = NextEpisodeCalculator.nextAiring(broadcast, now) ?: return null
    val remaining = Duration.between(now, next)
    if (remaining.isZero || remaining.isNegative || remaining >= Duration.ofHours(24)) return null
    // Round up so the badge never shows zero before the scheduled broadcast.
    val seconds = remaining.seconds + if (remaining.nano > 0) 1 else 0
    return String.format(Locale.ROOT, "%02d:%02d:%02d", seconds / 3600, seconds / 60 % 60, seconds % 60)
}
