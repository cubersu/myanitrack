package com.myanitrack.feature.details.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.myanitrack.core.model.MediaType
import com.myanitrack.feature.details.DetailsRoute
import kotlinx.serialization.Serializable

/**
 * Detay rotasi feature modulunun icinde tanimli.
 *
 * Boylece :app modulu rota sinifini bilmek zorunda kalmiyor ve
 * feature -> app yonunde bir bagimlilik dongusu olusmuyor.
 */
@Serializable
data class MediaDetailsRoute(
    val mediaTypeName: String,
    val malId: Int,
)

fun NavController.navigateToMediaDetails(mediaType: MediaType, malId: Int) {
    navigate(MediaDetailsRoute(mediaTypeName = mediaType.name, malId = malId))
}

fun NavGraphBuilder.mediaDetailsScreen(
    onBack: () -> Unit,
    onOpenMedia: (MediaType, Int) -> Unit,
    onOpenPerson: (Int, Boolean) -> Unit,
    isGuest: Boolean,
    onLogin: () -> Unit,
) {
    composable<MediaDetailsRoute> {
        DetailsRoute(onBack = onBack, onOpenMedia = onOpenMedia, onOpenPerson = onOpenPerson, isGuest = isGuest, onLogin = onLogin)
    }
}

@Serializable
data class PersonDestination(val id: Int, val isCharacter: Boolean)

fun NavGraphBuilder.personDetailsScreen(onBack: () -> Unit) {
    composable<PersonDestination> { com.myanitrack.feature.details.PersonScreen(onBack = onBack) }
}
