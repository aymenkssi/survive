package com.survive.app.ui

import androidx.compose.runtime.Composable
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
import com.survive.app.ui.tools.CompassScreen
import com.survive.app.ui.tools.CprScreen
import com.survive.app.ui.tools.MorseScreen
import com.survive.app.ui.tools.TorchScreen

object Routes {
    const val HOME = "home"
    const val SEARCH = "search"
    const val SETTINGS = "settings"
    const val CATEGORY = "category/{id}"
    const val GUIDE = "guide/{id}"
    fun category(id: String) = "category/$id"
    fun guide(id: String) = "guide/$id"
    fun tool(id: String) = "tool/$id"
}

@Composable
fun SurviveNavHost(repository: ContentRepository, prefs: UserPrefs) {
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
            )
        }
        composable(Routes.SEARCH) {
            SearchScreen(repository, onGuide = { nav.navigate(Routes.guide(it)) }, onBack = back)
        }
        composable(Routes.SETTINGS) { SettingsScreen(prefs, onBack = back) }
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
    }
}
