package com.pnd.android.loop.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import com.pnd.android.loop.ui.active.ActivePage
import com.pnd.android.loop.ui.home.Home
import com.pnd.android.loop.ui.home.loop.LoopViewModel

sealed class Screen(val route: String) {
    fun NavHostController.navigateTo(screen: Screen) = this.navigate(screen.route)

    data object Home : Screen("home")
    data object ActivePage : Screen("activePage")
}


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
        composable(Screen.Home.route) {
            Home(
                loopViewModel = loopViewModel,
            )
        }
        composable(Screen.ActivePage.route) {
            ActivePage(loopViewModel = loopViewModel)
        }
    }

}