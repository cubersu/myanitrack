package com.myanitrack.ui

import androidx.lifecycle.ViewModel
import com.myanitrack.core.common.deeplink.DeepLinkBus
import com.myanitrack.core.common.deeplink.DeepLinkTarget
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * Derin baglanti akisini gezinme kabuguna tasir.
 *
 * Ince bir sarmalayici; amaci [DeepLinkBus]-i Compose tarafina Hilt uzerinden
 * ulastirmak ve baglanti islendikten sonra tekrar oynatilmasini engellemek.
 */
@HiltViewModel
class DeepLinkViewModel @Inject constructor(
    private val deepLinkBus: DeepLinkBus,
) : ViewModel() {

    val deepLinks: Flow<DeepLinkTarget> = deepLinkBus.targets

    fun onDeepLinkHandled() = deepLinkBus.clear()
}
