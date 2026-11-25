package com.survive.app.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.survive.app.data.ContentRepository
import com.survive.app.ui.category.CategoryScreen
import com.survive.app.ui.guide.GuideScreen
import com.survive.app.ui.home.HomeScreen
import com.survive.app.ui.search.SearchScreen

object Routes {
    const val HOME = "home"
    const val SEARCH = "search"
    const val CATEGORY = "category/{id}"
    const val GUIDE = "guide/{id}"
    fun category(id: String) = "category/$id"
    fun guide(id: String) = "guide/$id"
}

@Composable
fun SurviveNavHost(repository: ContentRepository) {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                repository = repository,
                onCategory = { nav.navigate(Routes.category(it)) },
                onGuide = { nav.navigate(Routes.guide(it)) },
                onSearch = { nav.navigate(Routes.SEARCH) },
            )
        }
        composable(Routes.SEARCH) {
            SearchScreen(
                repository = repository,
                onGuide = { nav.navigate(Routes.guide(it)) },
                onBack = { nav.popBackStack() },
            )
        }
        composable(Routes.CATEGORY) { entry ->
            CategoryScreen(
                repository = repository,
                categoryId = entry.arguments?.getString("id").orEmpty(),
                onGuide = { nav.navigate(Routes.guide(it)) },
                onBack = { nav.popBackStack() },
            )
        }
        composable(Routes.GUIDE) { entry ->
            GuideScreen(
                repository = repository,
                guideId = entry.arguments?.getString("id").orEmpty(),
                onBack = { nav.popBackStack() },
            )
        }
    }
}
