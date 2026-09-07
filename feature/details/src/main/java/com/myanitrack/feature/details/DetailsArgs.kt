package com.myanitrack.feature.details

import androidx.lifecycle.SavedStateHandle
import com.myanitrack.core.model.MediaType
import com.myanitrack.feature.details.navigation.MediaDetailsRoute

/**
 * [MediaDetailsRoute] argumanlarini ViewModel tarafinda okur.
 *
 * Navigation-Compose type-safe rotalari, rota sinifinin ozellik adlarini anahtar
 * olarak [SavedStateHandle]-a yaziyor; bu yuzden dogrudan okumak `toRoute()` ile
 * ayni sonucu veriyor. `toRoute()` yerine bunu tercih ettik cunku o cagri
 * `android.os.Bundle` kullaniyor ve JVM birim testlerinde calismiyor - ViewModel-i
 * test edebilmek Roboelectric eklemekten daha degerli.
 *
 * Bilinmeyen bir tur adi gelirse (orn. bozuk deep link) anime varsayilir; ekran
 * bos bir hata yerine yine de bir seyler gosterir.
 */
internal class DetailsArgs(savedStateHandle: SavedStateHandle) {

    val mediaType: MediaType = savedStateHandle
        .get<String>(KEY_MEDIA_TYPE)
        ?.let { name -> MediaType.entries.firstOrNull { it.name == name } }
        ?: MediaType.ANIME

    val malId: Int = savedStateHandle.get<Int>(KEY_MAL_ID) ?: 0

    private companion object {
        const val KEY_MEDIA_TYPE = "mediaTypeName"
        const val KEY_MAL_ID = "malId"
    }
}
