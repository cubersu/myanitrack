package com.myanitrack.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.outlined.Person
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
import com.myanitrack.feature.calendar.CalendarRoute
import com.myanitrack.feature.details.navigation.mediaDetailsScreen
import com.myanitrack.feature.details.navigation.navigateToMediaDetails
import com.myanitrack.feature.forum.navigation.forumScreen
import com.myanitrack.feature.forum.navigation.navigateToForum
import com.myanitrack.feature.messaging.navigation.messagingScreen
import com.myanitrack.feature.messaging.navigation.navigateToMessaging
import com.myanitrack.feature.mylist.MyListRoute
import com.myanitrack.feature.news.NewsRoute
import com.myanitrack.feature.profile.ProfileRoute
import com.myanitrack.feature.profile.navigation.navigateToProfileComments
import com.myanitrack.feature.profile.navigation.navigateToUserProfile
import com.myanitrack.feature.profile.navigation.profileCommentsScreen
import com.myanitrack.feature.profile.navigation.userProfileScreen
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
data object CalendarDestination

@Serializable
data object NewsDestination

@Serializable
data object ProfileDestination

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
    CALENDAR(CalendarDestination::class, Icons.Outlined.CalendarMonth, R.string.nav_calendar),
    NEWS(NewsDestination::class, Icons.Outlined.Newspaper, R.string.nav_news),
    PROFILE(ProfileDestination::class, Icons.Outlined.Person, R.string.nav_profile),
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
        TopLevelDestination.CALENDAR -> navigate(CalendarDestination, options)
        TopLevelDestination.NEWS -> navigate(NewsDestination, options)
        TopLevelDestination.PROFILE -> navigate(ProfileDestination, options)
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
    val openUser: (String) -> Unit = navController::navigateToUserProfile
    val openSettings: () -> Unit = { navController.navigate(SettingsDestination) }
    val openForum: () -> Unit = navController::navigateToForum
    val openMessages: () -> Unit = navController::navigateToMessaging
    val openComments: (Int) -> Unit = navController::navigateToProfileComments

    NavHost(
        navController = navController,
        startDestination = MyListDestination,
        modifier = modifier,
    ) {
        mainGraph(
            onBack = { navController.popBackStack() },
            onOpenMedia = openMedia,
            onOpenUser = openUser,
            onOpenSettings = openSettings,
            onOpenForum = openForum,
            onOpenMessages = openMessages,
            onOpenComments = openComments,
        )
    }
}

private fun NavGraphBuilder.mainGraph(
    onBack: () -> Unit,
    onOpenMedia: (MediaType, Int) -> Unit,
    onOpenUser: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenForum: () -> Unit,
    onOpenMessages: () -> Unit,
    onOpenComments: (Int) -> Unit,
) {
    composable<MyListDestination> { MyListRoute(onOpenDetails = onOpenMedia) }
    composable<BrowseDestination> { BrowseRoute(onOpenMedia = onOpenMedia) }
    composable<CalendarDestination> { CalendarRoute(onOpenMedia = onOpenMedia) }
    composable<NewsDestination> { NewsRoute() }
    composable<ProfileDestination> {
        // Sekmeden acilan kendi profilimiz: geri dugmesi yok, ayarlar buradan aciliyor.
        ProfileRoute(
            onOpenMedia = onOpenMedia,
            onOpenUser = onOpenUser,
            onOpenSettings = onOpenSettings,
            onOpenForum = onOpenForum,
            onOpenMessages = onOpenMessages,
            onOpenComments = onOpenComments,
        )
    }
    composable<SettingsDestination> { SettingsRoute() }
    mediaDetailsScreen(onBack = onBack, onOpenMedia = onOpenMedia)
    userProfileScreen(onBack = onBack, onOpenMedia = onOpenMedia, onOpenUser = onOpenUser)

    // Kirilgan moduller: MAL API-si olmayan bolumler WebView ile aciliyor.
    forumScreen(onBack = onBack)
    messagingScreen(onBack = onBack)
    profileCommentsScreen(onBack = onBack)
}
