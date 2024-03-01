package com.pnd.android.loop.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.ui.detail.DetailPage
import com.pnd.android.loop.ui.history.HistoryPage
import com.pnd.android.loop.ui.home.Home
import com.pnd.android.loop.ui.home.loop.viewmodel.LoopViewModel

sealed class Screen(val route: String) {

    data object Home : Screen("home")
    data object ActivePage : Screen("active")
    data object DetailPage : Screen("detail/{$ARGS_ID}") {
        val arguments = listOf(navArgument(ARGS_ID) { type = NavType.IntType })

        fun id(backStackEntry: NavBackStackEntry) = backStackEntry.arguments?.getInt(ARGS_ID) ?: -1

        fun navigate(
            navController: NavHostController,
            loop: LoopBase
        ) {
            navController.navigate("detail/${loop.id}")
        }
    }

    data object HistoryPage : Screen("history")

    companion object {
        const val ARGS_ID = "id"
    }
}

fun NavHostController.navigate(screen: Screen) = this.navigate(screen.route)

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    loopViewModel: LoopViewModel,
) {
    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(
            route = Screen.Home.route
        ) {
            Home(
                loopViewModel = loopViewModel,
                onNavigateToDetailPage = { loop ->
                    Screen.DetailPage.navigate(
                        navController = navController,
                        loop = loop,
                    )
                },
                onNavigateToHistoryPage = {
                    navController.navigate(Screen.HistoryPage)
                }
            )
        }

        composable(
            route = Screen.DetailPage.route,
            arguments = Screen.DetailPage.arguments,
            enterTransition = { scaleIntoContainer() },
            exitTransition = { scaleOutOfContainer(INWARDS) },
            popEnterTransition = { scaleIntoContainer(OUTWARDS) },
            popExitTransition = { scaleOutOfContainer() }
        ) {
            DetailPage(
                onNavigateUp = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.HistoryPage.route,
            enterTransition = { scaleIntoContainer() },
            exitTransition = { scaleOutOfContainer(INWARDS) },
            popEnterTransition = { scaleIntoContainer(OUTWARDS) },
            popExitTransition = { scaleOutOfContainer() }
        ) {
            HistoryPage()
        }
    }
}

private const val INWARDS = 0
private const val OUTWARDS = 1
private fun scaleIntoContainer(
    direction: Int = INWARDS,
    initialScale: Float = if (direction == OUTWARDS) 0.9f else 1.1f
): EnterTransition {
    return scaleIn(
        animationSpec = tween(700),
        initialScale = initialScale
    ) + fadeIn(animationSpec = tween(700))
}

private fun scaleOutOfContainer(
    direction: Int = OUTWARDS,
    targetScale: Float = if (direction == INWARDS) 0.9f else 1.1f
): ExitTransition {
    return scaleOut(
        animationSpec = tween(
            durationMillis = 700,
        ), targetScale = targetScale
    ) + fadeOut(tween(700))
}