package com.myanitrack.core.domain.schedule

import com.myanitrack.core.model.BroadcastInfo
import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters

/**
 * Yayin bilgisinden bir sonraki bolumun yayin anini hesaplar.
 *
 * MAL yayin saatini her zaman yayinci saat diliminde (genelde Asia/Tokyo) verir.
 * Kullanicinin cihazi baska bir dilimde oldugu icin hesap once yayinci diliminde
 * yapilip sonra [Instant]-a cevriliyor; boylece yaz saati gecisleri ve tarih
 * siniri sorunlari java.time tarafindan dogru ele aliniyor.
 *
 * Saf fonksiyon - "simdi" disaridan verildigi icin dogrudan birim testi yazilabiliyor.
 */
object NextEpisodeCalculator {

    /**
     * [broadcast] gunu ve saatinin, [now]-dan sonraki ilk gerceklesme ani.
     *
     * Yayin bilgisi eksikse null doner. Yayin ani tam olarak [now]-a esitse
     * bolum "henuz yayinlandi" kabul edilip bir sonraki haftaya gecilir; boylece
     * geri sayim hicbir zaman sifirda takili kalmaz.
     */
    fun nextAiring(broadcast: BroadcastInfo, now: Instant): Instant? {
        val day = broadcast.day ?: return null
        val time = broadcast.time ?: return null
        val zone = broadcast.zone ?: return null

        val localNow = ZonedDateTime.ofInstant(now, zone)
        val candidate = localNow
            .with(TemporalAdjusters.nextOrSame(day))
            .with(time)

        val next = if (candidate.toInstant() > now) candidate else candidate.plusWeeks(1)
        return next.toInstant()
    }

    /**
     * Bildirim icin: yayina [within] suresinden daha az kalmis mi.
     * Gecmis yayinlar false doner.
     */
    fun airsWithin(nextAiringAt: Instant?, now: Instant, within: Duration): Boolean {
        val airing = nextAiringAt ?: return false
        val remaining = Duration.between(now, airing)
        return !remaining.isNegative && remaining <= within
    }
}
