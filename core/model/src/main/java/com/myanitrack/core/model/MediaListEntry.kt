package com.myanitrack.core.model

/** Listedeki tek satir: yapim bilgisi + kullanicinin kaydi. */
data class MediaListEntry(
    val node: MediaNode,
    val listStatus: MyListStatus,
) {
    val id: Int get() = node.id
    val mediaType: MediaType get() = node.mediaType

    val progress: Int get() = listStatus.progress(mediaType)
    val total: Int? get() = node.totalUnits

    /** 0f..1f arasi ilerleme; toplam bilinmiyorsa null. */
    val progressFraction: Float?
        get() {
            val t = total ?: return null
            if (t <= 0) return null
            return (progress.toFloat() / t).coerceIn(0f, 1f)
        }

    /** Sonraki bolume gecilebilir mi (toplam asilmasin). */
    val canIncrement: Boolean
        get() = total?.let { progress < it } ?: true
}
