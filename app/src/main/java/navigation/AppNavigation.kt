
package com.pocketbudget.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

import com.pocketbudget.app.screens.auth.ForgotPasswordScreen
import com.pocketbudget.app.screens.auth.LoginScreen
import com.pocketbudget.app.screens.auth.RegisterScreen
import com.pocketbudget.app.screens.auth.ResetPasswordScreen
import com.pocketbudget.app.screens.budget.AddBudgetScreen
import com.pocketbudget.app.screens.dashboard.DashboardScreen
import com.pocketbudget.app.screens.history.HistoryScreen
import com.pocketbudget.app.screens.payments.PaymentSandboxScreen
import com.pocketbudget.app.screens.profile.ProfileScreen
import com.pocketbudget.app.screens.savings.SavingsGoalsScreen
import com.pocketbudget.app.screens.splash.SplashScreen
import com.pocketbudget.app.screens.statistics.StatisticsScreen
import com.pocketbudget.app.screens.transactions.AddTransactionScreen

sealed class Screen(val route: String) {
    object PaymentSandbox : Screen("payment_sandbox")
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Register : Screen("register")
    object ForgotPassword : Screen("forgot_password")
    object ResetPassword : Screen("reset_password")
    object Dashboard : Screen("dashboard")
    object AddTransaction : Screen("add_transaction")
    object AddBudget : Screen("add_budget")
    object History : Screen("history")
    object Statistics : Screen("statistics")
    object SavingsGoals : Screen("savings_goals")
    object Profile : Screen("profile")
}

@Composable
fun AppNavigation(abrirRecuperacion: Boolean = false) {
    val navController = rememberNavController()

    LaunchedEffect(abrirRecuperacion) {
        if (abrirRecuperacion) {
            navController.navigate(Screen.ResetPassword.route) {
                popUpTo(Screen.Splash.route) {
                    inclusive = true
                }
                launchSingleTop = true
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(navController)
        }

        composable(Screen.Login.route) {
            LoginScreen(navController)
        }

        composable(Screen.Register.route) {
            RegisterScreen(navController)
        }

        composable(Screen.ForgotPassword.route) {
            ForgotPasswordScreen(navController)
        }

        composable(Screen.ResetPassword.route) {
            ResetPasswordScreen(navController)
        }

        composable(Screen.Dashboard.route) {
            DashboardScreen(navController)
        }

        composable(Screen.AddTransaction.route) {
            AddTransactionScreen(navController)
        }

        composable(Screen.AddBudget.route) {
            AddBudgetScreen(navController)
        }

        composable(Screen.PaymentSandbox.route) {
            PaymentSandboxScreen(navController = navController)
        }

        composable(Screen.History.route) {
            HistoryScreen(navController)
        }

        composable(Screen.Statistics.route) {
            StatisticsScreen(navController)
        }

        composable(Screen.SavingsGoals.route) {
            SavingsGoalsScreen(navController)
        }

        composable(Screen.Profile.route) {
            ProfileScreen(navController)
        }
    }
}
