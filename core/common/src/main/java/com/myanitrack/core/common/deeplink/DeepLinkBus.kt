package com.myanitrack.core.common.deeplink

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** Uygulama icinde acilacak hedef. */
sealed interface DeepLinkTarget {
    data class Media(val mediaTypeName: String, val malId: Int) : DeepLinkTarget
}

/**
 * `myanitrack://` derin baglantilarini Activity-den gezinme katmanina tasir.
 *
 * Bildirime dokunuldugunda ya da disaridan bir baglanti acildiginda MainActivity
 * hedefi buraya yayinlar; gezinme kabugu dinleyip ilgili ekrani acar.
 * Tampon 1 oldugu icin gezinme henuz hazir degilse baglanti kaybolmaz.
 */
@Singleton
class DeepLinkBus @Inject constructor() {

    private val _targets = MutableSharedFlow<DeepLinkTarget>(replay = 1, extraBufferCapacity = 1)

    val targets: SharedFlow<DeepLinkTarget> = _targets.asSharedFlow()

    fun publish(target: DeepLinkTarget) {
        _targets.tryEmit(target)
    }

    /** Hedefe gidildikten sonra tekrar islenmemesi icin temizlenir. */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun clear() {
        _targets.resetReplayCache()
    }
}
