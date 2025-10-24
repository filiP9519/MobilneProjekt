package com.dji.mobilneprojekt.pages

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.dji.mobilneprojekt.AuthViewModel
import com.google.firebase.Firebase
//import com.google.firebase.firestore.firestore
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation


@Composable
fun LoginScreen(modifier: Modifier = Modifier, navController: NavController, authViewModel: AuthViewModel) {

    var email by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column (
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ){ // why DP in this text doesnt work ? find later !!
        Text(text = "Login Page", fontSize = 32.sp)
        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = email,
            onValueChange = { newText -> email = newText},
            label = { Text(text ="Email") }
        )
        TextField(
            value = username,
            onValueChange = { newText -> username = newText},
            label = { Text(text ="Username") },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 100.dp, vertical = 20.dp)
        )
        TextField(
            value = password,
            onValueChange = { newText -> password = newText},
            label = { Text(text ="Password") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 100.dp).padding(bottom = 20.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = {

        }){
            Text(text = "Login")
        }
        //Button to navigate to the register screen
        TextButton(onClick = {navController.navigate("register") }) {
            Text(text = "Dont have an account? Sign Up")
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}