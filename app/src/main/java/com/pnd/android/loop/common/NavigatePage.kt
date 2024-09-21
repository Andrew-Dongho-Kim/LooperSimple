package com.pnd.android.loop.common

import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.pnd.android.loop.data.LoopBase


sealed class NavigatePage(val route: String) {

    data object Home : NavigatePage("home?$ARGS_HIGHLIGHT_ID={$ARGS_HIGHLIGHT_ID}") {
        val arguments = listOf(
            navArgument(ARGS_HIGHLIGHT_ID) {
                type = NavType.IntType
                defaultValue = UNKNOWN_ID
            }
        )

        fun withHighlight(highlightId: Int) = "home?$ARGS_HIGHLIGHT_ID=$highlightId"
    }

    data object DetailPage : NavigatePage("detail/{$ARGS_ID}") {
        val arguments = listOf(
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
            id = loop.id
        )

        fun navigate(
            navController: NavHostController,
            id: Int,
        ) {
            navController.navigate("detail/${id}")
        }
    }

    data object DailyAchievementPage : NavigatePage("daily_achievement")

    data object StatisticsPage : NavigatePage("statistics")
    companion object {
        const val ARGS_HIGHLIGHT_ID = "highlightId"
        const val ARGS_ID = "id"
        const val UNKNOWN_ID = -1
    }
}
