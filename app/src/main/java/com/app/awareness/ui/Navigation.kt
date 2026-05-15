package com.app.awareness.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.app.awareness.ui.theme.TextSecondary
import com.app.awareness.viewmodel.AppDetailViewModel
import com.app.awareness.viewmodel.HomeViewModel
import com.app.awareness.viewmodel.WeeklyViewModel
import com.app.awareness.ui.OnboardingScreen

// ── Route constants ───────────────────────────────────────────────────────────

object Routes {
    const val HOME       = "home"
    const val WEEKLY     = "weekly"
    const val SETTINGS   = "settings"
    const val ONBOARDING = "onboarding"
    const val APP_DETAIL = "detail/{packageName}"

    fun appDetail(packageName: String) = "detail/$packageName"
}

// ── Nav graph ─────────────────────────────────────────────────────────────────

/**
 * Root NavHost for the Blink app.
 *
 * Navigation structure (FRONTEND.md):
 *   HomeScreen  (default destination)
 *   WeeklyScreen
 *   SettingsScreen
 *   AppDetailSheet  (bottom sheet over any screen)
 *
 * No bottom navigation bar.
 * Home → Settings via gear icon callback.
 * Home → Weekly via swipe-up callback (gesture wiring TBD in HomeScreen).
 * Any screen → back via system gesture / navController.popBackStack().
 */
@Composable
fun BlinkNavGraph(
    startDestination: String = Routes.HOME,
) {
    val navController = rememberNavController()

    NavHost(
        navController    = navController,
        startDestination = startDestination,
    ) {

        // ── Onboarding ────────────────────────────────────────────────────────
        composable(Routes.ONBOARDING) {
            OnboardingScreen(onComplete = {
                navController.navigate(Routes.HOME) {
                    popUpTo(Routes.ONBOARDING) { inclusive = true }
                }
            })
        }

        // ── Home ──────────────────────────────────────────────────────────────
        composable(Routes.HOME) {
            val vm: HomeViewModel = viewModel()

            val todayScreenTime    by vm.todayScreenTime.collectAsStateWithLifecycle()
            val topApps            by vm.topApps.collectAsStateWithLifecycle()
            val insights           by vm.insights.collectAsStateWithLifecycle()
            val benchmarkStats     by vm.benchmarkComparisons.collectAsStateWithLifecycle()
            val yesterdayDelta     by vm.yesterdayDelta.collectAsStateWithLifecycle()

            HomeScreen(
                todayScreenTime    = todayScreenTime,
                topApps            = topApps,
                insights           = insights,
                benchmarkStats     = benchmarkStats,
                yesterdayDelta     = yesterdayDelta,
                onAppClick         = { pkg -> navController.navigate(Routes.appDetail(pkg)) },
                onSettingsClick    = { navController.navigate(Routes.SETTINGS) },
                onWeeklySwipe      = { navController.navigate(Routes.WEEKLY) },
            )
        }

        // ── Weekly ────────────────────────────────────────────────────────────
        composable(Routes.WEEKLY) {
            val vm: WeeklyViewModel = viewModel()

            val weekTotalMinutes by vm.weekTotalMinutes.collectAsStateWithLifecycle()
            val bestDay          by vm.bestDay.collectAsStateWithLifecycle()
            val topApp           by vm.topApp.collectAsStateWithLifecycle()
            val weeklyCards      by vm.weeklyCards.collectAsStateWithLifecycle()

            WeeklyScreen(
                weekTotalMinutes = weekTotalMinutes,
                bestDay          = bestDay,
                topApp           = topApp,
                weeklyCards      = weeklyCards,
            )
        }

        // ── Settings ──────────────────────────────────────────────────────────
        composable(Routes.SETTINGS) {
            SettingsScreen()
        }

        // ── App Detail (bottom sheet) ─────────────────────────────────────────
        composable(
            route     = Routes.APP_DETAIL,
            arguments = listOf(navArgument("packageName") { type = NavType.StringType }),
        ) { backStackEntry ->
            val packageName = backStackEntry.arguments?.getString("packageName") ?: ""
            val vm: AppDetailViewModel = viewModel()

            val dailyUsage   by vm.dailyUsage.collectAsStateWithLifecycle()
            val currentLimit by vm.currentLimit.collectAsStateWithLifecycle()

            AppDetailSheet(
                appName      = packageName,
                dailyUsage   = dailyUsage,
                currentLimit = currentLimit,
                onLimitChange = { vm.setLimit(it) },
                onDismiss    = { navController.popBackStack() },
            )
        }
    }
}


