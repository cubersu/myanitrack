package com.myanitrack.feature.messaging.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.myanitrack.feature.messaging.MessagingRoute
import kotlinx.serialization.Serializable

@Serializable
data object MessagingRouteKey

fun NavController.navigateToMessaging() = navigate(MessagingRouteKey)

fun NavGraphBuilder.messagingScreen(onBack: () -> Unit) {
    composable<MessagingRouteKey> { MessagingRoute(onBack = onBack) }
}
