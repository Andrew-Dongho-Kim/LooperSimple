package com.pnd.android.loop.common

import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.pnd.android.loop.data.LoopBase
import kotlin.random.Random


sealed class NavigatePage(val route: String) {

    open val arguments: List<NamedNavArgument> = emptyList()

    data object Home : NavigatePage(
        route = "home?$ARGS_HIGHLIGHT_ID={$ARGS_HIGHLIGHT_ID}&$ARGS_RANDOM_KEY={$ARGS_RANDOM_KEY}"
    ) {

        private const val HOST = "home"

        override val arguments = listOf(
            navArgument(ARGS_HIGHLIGHT_ID) {
                type = NavType.IntType
                defaultValue = UNKNOWN_ID
            },
            navArgument(ARGS_RANDOM_KEY) {
                type = NavType.IntType
                defaultValue = UNKNOWN_ID
            }
        )

        fun deepLinkPattern() = uriPattern(route)
        fun deepLink(highlightId: Int = UNKNOWN_ID) =
            "${uriPattern(HOST)}?$ARGS_HIGHLIGHT_ID=$highlightId&$ARGS_RANDOM_KEY=${Random.nextInt()}"

        fun to(highlightId: Int = UNKNOWN_ID) =
            "$HOST?$ARGS_HIGHLIGHT_ID=$highlightId&$ARGS_RANDOM_KEY=${Random.nextInt()}"
    }

    data object DetailPage : NavigatePage("detail/{$ARGS_ID}") {
        override val arguments = listOf(
            navArgument(ARGS_ID) {
                type = NavType.IntType
            }
        )

        fun id(backStackEntry: NavBackStackEntry) =
            backStackEntry.arguments?.getInt(ARGS_ID) ?: UNKNOWN_ID

        fun navigate(
            navController: NavHostController,
            loop: LoopBase
        ) = navigate(
            navController = navController,
            id = loop.loopId
        )

        fun navigate(
            navController: NavHostController,
            id: Int,
        ) {
            navController.navigate("detail/${id}")
        }
    }

    data object GroupPage : NavigatePage("group") {
        fun navigate(navController: NavHostController) {
            navController.navigate(route)
        }
    }

    data object DailyAchievementPage : NavigatePage("daily_achievement") {
        fun navigate(navController: NavHostController) {
            navController.navigate(route)
        }
    }

    data object StatisticsPage : NavigatePage("statistics") {
        fun navigate(navController: NavHostController) {
            navController.navigate(route)
        }
    }

    companion object {
        private const val SCHEME = "lsimp"

        const val ARGS_HIGHLIGHT_ID = "highlightId"
        const val ARGS_RANDOM_KEY = "randomKey"
        const val ARGS_ID = "id"
        const val UNKNOWN_ID = -1

        fun uriPattern(host: String) = "$SCHEME://$host"
    }
}
