package com.dji.mobilneprojekt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.collection.emptyLongIntMap
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.dji.mobilneprojekt.ui.theme.MobilneProjektTheme
//Import NavController and NavHost
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MobilneProjektTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // 1. Create a NavController instance
                    val navController = rememberNavController()
                    // 2. Set up the Navhost
                    NavHost(
                        navController = navController,
                        startDestination = "login",// The first screen to show
                        modifier = Modifier.padding(innerPadding) // Apply padding
                    ) {
                        // 3. Define the composable for the "login" route
                        composable("login"){
                            LoginScreen(navController = navController)
                        }
                        // 4. Define the composable for the "register" route
                        composable("register"){
                            RegisterScreen(navController = navController)
                        }

                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MobilneProjektTheme {
        Greeting("Android")
    }
}