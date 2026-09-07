package com.myanitrack.core.common.auth

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Tarayicidan donen OAuth yonlendirmesini Activity-den giris ekranina tasir.
 *
 * `myanitrack://auth?code=...` adresi MainActivity-nin intent-filter-ina duser;
 * oradaki URI buraya yayinlanir ve giris ekranindaki ViewModel dinler.
 * Tampon 1 oldugu icin ekran henuz dinlemiyorsa bile yonlendirme kaybolmaz.
 */
@Singleton
class AuthRedirectBus @Inject constructor() {

    private val _redirects = MutableSharedFlow<String>(replay = 1, extraBufferCapacity = 1)

    val redirects: SharedFlow<String> = _redirects.asSharedFlow()

    fun publish(uri: String) {
        _redirects.tryEmit(uri)
    }

    /** Giris tamamlandiktan sonra tekrar islenmemesi icin temizlenir. */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun clear() {
        _redirects.resetReplayCache()
    }
}
