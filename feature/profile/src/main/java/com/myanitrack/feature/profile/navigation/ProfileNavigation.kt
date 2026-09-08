package com.myanitrack.feature.profile.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.myanitrack.core.model.MediaType
import com.myanitrack.feature.profile.ProfileRoute
import kotlinx.serialization.Serializable

/**
 * Baska bir kullanicinin profili.
 *
 * Kendi profilimiz :app modulundeki sekme hedefinden aciliyor (arguman yok);
 * bu rota yalnizca arkadas listesinden ya da akistan bir kullaniciya
 * gidildiginde kullanilir.
 */
@Serializable
data class UserProfileRoute(val userName: String)

fun NavController.navigateToUserProfile(userName: String) {
    navigate(UserProfileRoute(userName))
}

fun NavGraphBuilder.userProfileScreen(
    onBack: () -> Unit,
    onOpenMedia: (MediaType, Int) -> Unit,
    onOpenUser: (String) -> Unit,
) {
    composable<UserProfileRoute> {
        ProfileRoute(
            onBack = onBack,
            onOpenMedia = onOpenMedia,
            onOpenUser = onOpenUser,
        )
    }
}
