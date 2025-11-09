package com.dji.mobilneprojekt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.dji.mobilneprojekt.ui.theme.MobilneProjektTheme
import com.google.firebase.FirebaseApp

//Import NavController and NavHost

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //FirebaseApp.initializeApp(this)
        enableEdgeToEdge()
        val authViewModel : AuthViewModel by viewModels()
        val calendarViewModel : CalendarViewModel by viewModels()
        setContent {
            MobilneProjektTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Navigation(
                        modifier = Modifier.padding(innerPadding),
                        authViewModel = authViewModel,
                        calendarViewModel = calendarViewModel
                    )
                }
            }
        }
    }
}