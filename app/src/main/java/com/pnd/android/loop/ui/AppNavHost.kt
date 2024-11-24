package com.pnd.android.loop.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.util.Consumer
import androidx.fragment.app.FragmentManager.BackStackEntry
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import com.pnd.android.loop.common.NavigatePage
import com.pnd.android.loop.ui.detail.DetailPage
import com.pnd.android.loop.ui.history.DailyAchievementPage
import com.pnd.android.loop.ui.home.Home
import com.pnd.android.loop.ui.home.group.GroupPage
import com.pnd.android.loop.ui.home.group.GroupPicker
import com.pnd.android.loop.ui.home.viewmodel.LoopViewModel
import com.pnd.android.loop.ui.statisctics.StatisticsPage

fun Context.findActivity(): ComponentActivity? {
    var context = this
    while (context is ContextWrapper && context !is Activity)
        context = context.baseContext
    return context as? ComponentActivity
}


const val ARGS_NAVIGATE_ACTION = "args_navigate_action"

@Composable
fun IntentConsumer(
    navController: NavHostController
) {
    val activity = LocalContext.current.findActivity() ?: return

    DisposableEffect(key1 = activity, navController) {
        val onNewIntentConsumer = Consumer<Intent> { intent ->
            val navAction = intent.getStringExtra(ARGS_NAVIGATE_ACTION)
            Log.d("IntentConsumer", "onNewIntent[$navAction]: $intent")

            navAction ?: return@Consumer
            navController.navigate(
                navAction,
                navOptions = NavOptions.Builder()
                    .setLaunchSingleTop(true)
                    .setRestoreState(false)
                    .build()
            )
            intent.extras?.remove(ARGS_NAVIGATE_ACTION)
        }

        activity.addOnNewIntentListener(onNewIntentConsumer)
        onDispose { activity.removeOnNewIntentListener(onNewIntentConsumer) }
    }
}

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    loopViewModel: LoopViewModel,
) {
    val onNavigateUp: () -> Unit = remember { { navController.popBackStack() } }

    IntentConsumer(navController = navController)

    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = NavigatePage.Home.route
    ) {
        composable(
            route = NavigatePage.Home.route,
            arguments = NavigatePage.Home.arguments,
            deepLinks = listOf(
                navDeepLink {
                    uriPattern = NavigatePage.Home.deepLinkPattern()
                }
            )
        ) { backStackEntry ->

            backStackEntry.arguments?.let { args ->
                loopViewModel.setHighlightId(
                    args.getInt(NavigatePage.ARGS_HIGHLIGHT_ID),
                    args.getInt(NavigatePage.ARGS_RANDOM_KEY)
                )
            }

            Home(
                loopViewModel = loopViewModel,
                onNavigateToGroupPicker = { loop ->
                    NavigatePage.GroupPicker.navigate(
                        navController = navController,
                        loop = loop,
                    )
                },
                onNavigateToGroupPage = {
                    NavigatePage.GroupPage.navigate(navController)
                },
                onNavigateToDetailPage = { loop ->
                    NavigatePage.DetailPage.navigate(
                        navController = navController,
                        loop = loop,
                    )
                },
                onNavigateToHistoryPage = {
                    NavigatePage.DailyAchievementPage.navigate(navController)
                },
                onNavigateToStatisticsPage = {
                    NavigatePage.StatisticsPage.navigate(navController)
                }
            )
        }

        page(page = NavigatePage.GroupPicker) { backStackEntry ->
            GroupPicker(
                loopId = backStackEntry.arguments?.getInt(NavigatePage.ARGS_ID) ?: -1,
                onNavigateUp = onNavigateUp
            )
        }

        page(page = NavigatePage.GroupPage) {
            GroupPage(
                loopViewModel = loopViewModel,
                onNavigateUp = onNavigateUp
            )
        }

        page(page = NavigatePage.DetailPage) {
            DetailPage(onNavigateUp = onNavigateUp)
        }

        page(page = NavigatePage.DailyAchievementPage) {
            DailyAchievementPage(onNavigateUp = onNavigateUp)
        }

        page(page = NavigatePage.StatisticsPage) {
            StatisticsPage(
                onNavigateToDetailPage = { id ->
                    NavigatePage.DetailPage.navigate(
                        navController = navController,
                        id = id,
                    )
                },
                onNavigateUp = onNavigateUp
            )
        }
    }
}


private fun NavGraphBuilder.page(
    page: NavigatePage,
    content: @Composable (NavBackStackEntry) -> Unit,
) {
    composable(
        route = page.route,
        arguments = page.arguments,
        enterTransition = { enterTransition() },
        exitTransition = { exitTransition() },
        popEnterTransition = { enterTransition() },
        popExitTransition = { exitTransition() }
    ) {
        content(it)
    }
}


private fun enterTransition(): EnterTransition {
//    return
//    scaleIn(
//        animationSpec = tween(700),
//        initialScale = initialScale
//    ) +
    return fadeIn(tween(1000)) + slideInVertically(tween(1000))
}

private fun exitTransition(): ExitTransition {
//    return scaleOut(
//        animationSpec = tween(
//            durationMillis = 700,
//        ), targetScale = targetScale
//    ) +
    return fadeOut(tween(1000)) + slideOutVertically(tween(1000))
}