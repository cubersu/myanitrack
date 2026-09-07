package com.myanitrack.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navOptions
import com.myanitrack.R
import com.myanitrack.core.model.MediaType
import com.myanitrack.feature.browse.BrowseRoute
import com.myanitrack.feature.details.navigation.mediaDetailsScreen
import com.myanitrack.feature.details.navigation.navigateToMediaDetails
import com.myanitrack.feature.mylist.MyListRoute
import com.myanitrack.feature.settings.SettingsRoute
import kotlin.reflect.KClass
import kotlinx.serialization.Serializable

/**
 * Type-safe Navigation-Compose rotalari.
 *
 * Detay rotasi :feature:details icinde tanimli (bkz. `mediaDetailsScreen`);
 * boylece :app modulu ile feature modulleri arasinda dongusel bagimlilik olusmuyor.
 * Sonraki fazlarda eklenecek ekranlar (takvim, haberler, profil) ayni deseni izleyecek.
 */
@Serializable
data object MyListDestination

@Serializable
data object BrowseDestination

@Serializable
data object SettingsDestination

/** Alt gezinme cubugundaki sekmeler. */
enum class TopLevelDestination(
    val route: KClass<*>,
    val icon: ImageVector,
    val labelRes: Int,
) {
    MY_LIST(MyListDestination::class, Icons.Outlined.VideoLibrary, R.string.nav_my_list),
    BROWSE(BrowseDestination::class, Icons.Outlined.Explore, R.string.nav_browse),
    SETTINGS(SettingsDestination::class, Icons.Outlined.Settings, R.string.nav_settings),
}

fun NavDestination?.isTopLevel(destination: TopLevelDestination): Boolean =
    this?.hierarchy?.any { it.hasRoute(destination.route) } == true

private val NavDestination.hierarchy: Sequence<NavDestination>
    get() = generateSequence(this) { it.parent }

/**
 * Sekmeler arasi gecis: yigin buyumesin diye baslangica geri sarilir ve
 * her sekmenin kendi durumu korunur.
 */
fun NavHostController.navigateToTopLevel(destination: TopLevelDestination) {
    val options = navOptions {
        popUpTo(graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
    when (destination) {
        TopLevelDestination.MY_LIST -> navigate(MyListDestination, options)
        TopLevelDestination.BROWSE -> navigate(BrowseDestination, options)
        TopLevelDestination.SETTINGS -> navigate(SettingsDestination, options)
    }
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    val openMedia: (MediaType, Int) -> Unit = { mediaType, malId ->
        navController.navigateToMediaDetails(mediaType, malId)
    }

    NavHost(
        navController = navController,
        startDestination = MyListDestination,
        modifier = modifier,
    ) {
        mainGraph(onBack = navController::popBackStack, onOpenMedia = openMedia)
    }
}

private fun NavGraphBuilder.mainGraph(
    onBack: () -> Unit,
    onOpenMedia: (MediaType, Int) -> Unit,
) {
    composable<MyListDestination> { MyListRoute(onOpenDetails = onOpenMedia) }
    composable<BrowseDestination> { BrowseRoute(onOpenMedia = onOpenMedia) }
    composable<SettingsDestination> { SettingsRoute() }
    mediaDetailsScreen(onBack = onBack, onOpenMedia = onOpenMedia)
}
