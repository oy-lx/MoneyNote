package com.moneynote.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.moneynote.ui.budget.BudgetScreen
import com.moneynote.ui.category.CategoryScreen
import com.moneynote.ui.edit.EditTxnScreen
import com.moneynote.ui.feedback.FeedbackScreen
import com.moneynote.ui.home.HomeScreen
import com.moneynote.ui.logs.LogScreen
import com.moneynote.ui.navigation.Routes
import com.moneynote.ui.navigation.TopLevelDestination
import com.moneynote.ui.settings.SettingsScreen
import com.moneynote.ui.stats.StatsScreen

@Composable
fun LedgerApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in TopLevelDestination.routeSet

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    TopLevelDestination.entries.forEach { destination ->
                        NavigationBarItem(
                            selected = currentRoute == destination.route,
                            onClick = { navController.switchTab(destination.route) },
                            icon = {
                                Icon(
                                    painter = painterResource(destination.icon),
                                    contentDescription = destination.label,
                                )
                            },
                            label = { Text(destination.label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = TopLevelDestination.HOME.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(TopLevelDestination.HOME.route) {
                HomeScreen(
                    onAddClick = { navController.navigate(Routes.edit()) },
                    onTxnClick = { id -> navController.navigate(Routes.edit(id)) },
                )
            }
            composable(TopLevelDestination.STATS.route) {
                StatsScreen()
            }
            composable(TopLevelDestination.BUDGET.route) {
                BudgetScreen()
            }
            composable(TopLevelDestination.SETTINGS.route) {
                SettingsScreen(
                    onOpenCategories = { navController.navigate(Routes.CATEGORIES) },
                    onOpenFeedback = { navController.navigate(Routes.FEEDBACK) },
                    onOpenLogs = { navController.navigate(Routes.LOGS) },
                )
            }
            composable(Routes.CATEGORIES) {
                CategoryScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.FEEDBACK) {
                FeedbackScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.LOGS) {
                LogScreen(onBack = { navController.popBackStack() })
            }
            composable(
                route = Routes.EDIT,
                arguments = listOf(
                    navArgument(Routes.ARG_TXN_ID) {
                        type = NavType.LongType
                        defaultValue = -1L
                    },
                ),
            ) { entry ->
                EditTxnScreen(
                    txnId = entry.arguments?.getLong(Routes.ARG_TXN_ID) ?: -1L,
                    onDone = { navController.popBackStack() },
                )
            }
        }
    }
}

/** 一级页面之间切换：保留各 Tab 状态，避免重复入栈。 */
private fun NavHostController.switchTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
