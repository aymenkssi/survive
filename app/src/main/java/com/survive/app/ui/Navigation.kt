package com.survive.app.ui

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.survive.app.data.maps.MapStore
import com.survive.app.ui.maps.CatalogScreen
import com.survive.app.ui.maps.MapViewerScreen
import com.survive.app.ui.maps.MapsScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.survive.app.data.ContentRepository
import com.survive.app.data.Tools
import com.survive.app.data.UserPrefs
import com.survive.app.ui.category.CategoryScreen
import com.survive.app.ui.guide.GuideScreen
import com.survive.app.ui.home.HomeScreen
import com.survive.app.ui.search.SearchScreen
import com.survive.app.ui.settings.SettingsScreen
import com.survive.app.ui.support.SupportScreen
import com.survive.app.ui.tools.CompassScreen
import com.survive.app.ui.tools.CprScreen
import com.survive.app.ui.tools.MorseScreen
import com.survive.app.ui.tools.TorchScreen

object Routes {
    const val HOME = "home"
    const val SEARCH = "search"
    const val SETTINGS = "settings"
    const val SUPPORT = "support"
    const val CATEGORY = "category/{id}"
    const val GUIDE = "guide/{id}"
    fun category(id: String) = "category/$id"
    fun guide(id: String) = "guide/$id"
    fun tool(id: String) = "tool/$id"
    const val MAP_CATALOG = "maps/catalog?folder={folder}"
    const val MAP_VIEW = "maps/view/{file}"
    fun mapCatalog(folder: String) = "maps/catalog?folder=" + Uri.encode(folder)
    fun mapView(file: String) = "maps/view/" + Uri.encode(file)
}

@Composable
fun SurviveNavHost(repository: ContentRepository, prefs: UserPrefs, mapStore: MapStore) {
    val nav = rememberNavController()
    val back: () -> Unit = { nav.popBackStack() }
    NavHost(navController = nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                repository = repository,
                prefs = prefs,
                onCategory = { nav.navigate(Routes.category(it)) },
                onGuide = { nav.navigate(Routes.guide(it)) },
                onTool = { nav.navigate(Routes.tool(it)) },
                onSearch = { nav.navigate(Routes.SEARCH) },
                onSettings = { nav.navigate(Routes.SETTINGS) },
                onSupport = { nav.navigate(Routes.SUPPORT) },
            )
        }
        composable(Routes.SEARCH) {
            SearchScreen(repository, onGuide = { nav.navigate(Routes.guide(it)) }, onBack = back)
        }
        composable(Routes.SETTINGS) { SettingsScreen(prefs, onSupport = { nav.navigate(Routes.SUPPORT) }, onBack = back) }
        composable(Routes.SUPPORT) { SupportScreen(onBack = back) }
        composable(Routes.CATEGORY) { entry ->
            CategoryScreen(
                repository = repository,
                categoryId = entry.arguments?.getString("id").orEmpty(),
                onGuide = { nav.navigate(Routes.guide(it)) },
                onBack = back,
            )
        }
        composable(Routes.GUIDE) { entry ->
            GuideScreen(
                repository = repository,
                prefs = prefs,
                guideId = entry.arguments?.getString("id").orEmpty(),
                onTool = { nav.navigate(Routes.tool(it)) },
                onBack = back,
            )
        }
        composable(Routes.tool(Tools.TORCH)) { TorchScreen(back) }
        composable(Routes.tool(Tools.MORSE)) { MorseScreen(back) }
        composable(Routes.tool(Tools.COMPASS)) { CompassScreen(back) }
        composable(Routes.tool(Tools.CPR)) { CprScreen(back) }
        composable(Routes.tool(Tools.MAPS)) {
            MapsScreen(
                mapStore,
                onCatalog = { nav.navigate(Routes.mapCatalog("")) },
                onOpen = { nav.navigate(Routes.mapView(it)) },
                onBack = back,
            )
        }
        composable(
            Routes.MAP_CATALOG,
            arguments = listOf(navArgument("folder") { type = NavType.StringType; defaultValue = "" }),
        ) { entry ->
            CatalogScreen(
                mapStore,
                folder = entry.arguments?.getString("folder").orEmpty(),
                onFolder = { nav.navigate(Routes.mapCatalog(it)) },
                onBack = back,
            )
        }
        composable(Routes.MAP_VIEW) { entry ->
            MapViewerScreen(mapStore, entry.arguments?.getString("file").orEmpty(), onBack = back)
        }
    }
}
