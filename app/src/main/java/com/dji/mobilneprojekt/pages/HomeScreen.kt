package com.dji.mobilneprojekt.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.dji.mobilneprojekt.AuthState
import com.dji.mobilneprojekt.AuthViewModel

@Composable
fun HomeScreen(modifier: Modifier = Modifier, navController: NavController, authViewModel: AuthViewModel) {

    val authState = authViewModel.authState.collectAsState()

    LaunchedEffect(authState) {
        when (authState.value) {
            is AuthState.Unauthenticated -> {
                navController.navigate("login") {
                    popUpTo("home") { inclusive = true }
                }
            }
            else -> Unit
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Home Page", fontSize = 32.sp)

        Spacer(modifier = Modifier.height(16.dp))

        TextButton( onClick =
            {
                val refreshTrigger = System.currentTimeMillis()
                navController.navigate("calendar?refreshTs=$refreshTrigger")
            } ) {
        Text("Calendar")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            authViewModel.signOut()
            navController.navigate("login") {
                popUpTo(navController.graph.startDestinationId) {
                    inclusive = true
                }
            }
        } ) {
            Text("SignOut")
        }
    }
}