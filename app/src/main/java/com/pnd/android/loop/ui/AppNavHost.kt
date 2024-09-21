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
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.util.Consumer
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pnd.android.loop.common.NavigatePage
import com.pnd.android.loop.ui.detail.DetailPage
import com.pnd.android.loop.ui.history.DailyAchievementPage
import com.pnd.android.loop.ui.home.Home
import com.pnd.android.loop.ui.home.loop.viewmodel.LoopViewModel
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
                    .build()
            )
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
            arguments = NavigatePage.Home.arguments
        ) { backStackEntry ->
            loopViewModel.setHighlightId(
                backStackEntry.arguments?.getInt(NavigatePage.ARGS_HIGHLIGHT_ID)
                    ?: NavigatePage.UNKNOWN_ID
            )

            Home(
                loopViewModel = loopViewModel,
                onNavigateToDetailPage = { loop ->
                    NavigatePage.DetailPage.navigate(
                        navController = navController,
                        loop = loop,
                    )
                },
                onNavigateToHistoryPage = {
                    navController.navigate(NavigatePage.DailyAchievementPage)
                },
                onNavigateToStatisticsPage = {
                    navController.navigate(NavigatePage.StatisticsPage)
                }
            )
        }

        composable(
            route = NavigatePage.DetailPage.route,
            arguments = NavigatePage.DetailPage.arguments,
            enterTransition = { scaleIntoContainer() },
            exitTransition = { scaleOutOfContainer(INWARDS) },
            popEnterTransition = { scaleIntoContainer(OUTWARDS) },
            popExitTransition = { scaleOutOfContainer() }
        ) {
            DetailPage(onNavigateUp = onNavigateUp)
        }

        composable(
            route = NavigatePage.DailyAchievementPage.route,
            enterTransition = { scaleIntoContainer() },
            exitTransition = { scaleOutOfContainer(INWARDS) },
            popEnterTransition = { scaleIntoContainer(OUTWARDS) },
            popExitTransition = { scaleOutOfContainer() }
        ) {
            DailyAchievementPage(onNavigateUp = onNavigateUp)
        }

        composable(
            route = NavigatePage.StatisticsPage.route,
            enterTransition = { scaleIntoContainer() },
            exitTransition = { scaleOutOfContainer(INWARDS) },
            popEnterTransition = { scaleIntoContainer(OUTWARDS) },
            popExitTransition = { scaleOutOfContainer() }
        ) {
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