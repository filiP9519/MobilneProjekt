package com.dji.mobilneprojekt.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.dji.mobilneprojekt.AuthState
import com.dji.mobilneprojekt.AuthViewModel

@Composable
fun HomeScreen(modifier: Modifier = Modifier, navController: NavController, authViewModel: AuthViewModel) {

    val authState = authViewModel.authState.observeAsState()

    LaunchedEffect(authState.value) {
        when (authState.value) {
            is AuthState.Unauthenticated -> navController.navigate("login")
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

        TextButton(onClick = {
            authViewModel.signout()
        }) {
            Text(text = "Sign Out")
        }
    }
}