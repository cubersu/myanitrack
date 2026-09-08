package com.myanitrack.feature.forum.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.myanitrack.feature.forum.ForumRoute
import kotlinx.serialization.Serializable

@Serializable
data object ForumRouteKey

fun NavController.navigateToForum() = navigate(ForumRouteKey)

fun NavGraphBuilder.forumScreen(onBack: () -> Unit) {
    composable<ForumRouteKey> { ForumRoute(onBack = onBack) }
}
