package com.dji.mobilneprojekt

import androidx.compose.runtime.Composable
import  androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dji.mobilneprojekt.pages.HomeScreen
import com.dji.mobilneprojekt.pages.LoginScreen
import com.dji.mobilneprojekt.pages.RegisterScreen
import com.dji.mobilneprojekt.pages.CalendarScreen


@Composable
fun Navigation (modifier: Modifier = Modifier, authViewModel: AuthViewModel, calendarViewModel: CalendarViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController,
            startDestination = "login",

            builder = {
            composable("login") {
                LoginScreen(modifier, navController, authViewModel)
            }
            composable("register") {
                RegisterScreen(modifier, navController, authViewModel)
            }
            composable("home") {
                HomeScreen(modifier, navController, authViewModel)
                }

                composable (
                    route = "calendar?refreshTs={refreshTs}", // 1. Update the route definition
                    arguments = listOf(navArgument("refreshTs") { // 2. Define the argument
                        type = NavType.LongType
                        defaultValue = 0L // Default value if not provided
                    })
                ){
                CalendarScreen(
                    modifier, navController, calendarViewModel,
                    authViewModel
                )
            }
    })

}