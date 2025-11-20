package com.dji.mobilneprojekt.pages

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
// We don't need DatePickerDialog anymore
import androidx.compose.material3.Text
// We don't need TextButton anymore
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextField
// removed rememberTooltipState as it was unused
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import java.time.LocalDate
import com.dji.mobilneprojekt.CalendarViewModel
import java.time.Instant
import com.dji.mobilneprojekt.AuthViewModel
import com.dji.mobilneprojekt.MyEvent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.api.services.calendar.CalendarScopes
import com.google.android.gms.common.api.Scope

fun LocalDate.formatAsString() : String {
    val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    return this.format(formatter)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    modifier: Modifier = Modifier,
    navController: NavController,
    viewModel: CalendarViewModel, // Removed default viewModel()
    authViewModel: AuthViewModel
){
// In your Composable (e.g., HomeScreen.kt)
    val context = LocalContext.current

// 1. Create a launcher to handle the Google Sign-In result
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    // SUCCESS! You now have the account.
                    // You can now use this account to fetch events.
                    val account = task.getResult(ApiException::class.java)
                    // You would pass this account to your ViewModel to start fetching
                    // viewModel.loadGoogleCalendarEvents(account)
                    Toast.makeText(context, "Google Calendar connected!", Toast.LENGTH_SHORT).show()
                } catch (e: ApiException) {
                    // Failed
                    Toast.makeText(context, "Failed to connect: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    )
    //======Google sign-in======
    val googleSignInClient = remember {
        // We request the "read-only" calendar scope
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(CalendarScopes.CALENDAR_READONLY)) // This is the key line
            .build()
        GoogleSignIn.getClient(context, gso)
    }
    //======Google sign-in======

    //observe the state from viewModel
    val uiState by viewModel.uiState.collectAsState()

    // --- FIX 1: Declared eventTitle state ---
    var eventTitle by remember { mutableStateOf("") }

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = Instant.now().toEpochMilli())

    LaunchedEffect(datePickerState.selectedDateMillis) {
        viewModel.selectDate(datePickerState.selectedDateMillis)
    }

    Column(
        // Use the modifier passed into the function
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        DatePicker(
            state = datePickerState,
            showModeToggle = false,
            title = null,
            headline = null
        )
        Spacer(Modifier.height(16.dp))

        Text(
            text = uiState.selectedDate?.formatAsString() ?: "No date selected",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(Modifier.height(16.dp))

        // --- FIX 2: Used eventTitle in value ---
        TextField(
            value = eventTitle,
            onValueChange = { eventTitle = it },
            label = { Text("Event Title") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                uiState.selectedDate?.let { date ->
                    // --- FIX 3: Fixed .isNotBlank() syntax ---
                    if(eventTitle.isNotBlank()){
                        viewModel.saveEvent(eventTitle, date)
                        eventTitle = "" // Clear text field
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add event")
        }

        Spacer(Modifier.height(24.dp))

        // --- FIX 4: Re-added LazyColumn to show events ---
        Text(
            text = "Events on this day:",
            style = MaterialTheme.typography.titleMedium
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 200.dp) // Give it a max height
        ) {
            if (uiState.events.isEmpty()) {
                item {
                    Text(
                        text = "No events.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                items(uiState.events) { event ->
                    EventItem(event = event)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = {
                authViewModel.signout()
                // --- FIX 5: Added popUpTo for clean sign-out navigation ---
                navController.navigate("login") {
                    popUpTo(navController.graph.startDestinationId) {
                        inclusive = true
                    }
                }
            } ) {
                Text("SignOut")
            }
            Button(onClick = {
                googleSignInLauncher.launch(googleSignInClient.signInIntent)
            }) {
                Text("Connect Google Calendar")
            }
        }

    }
}

@Composable
fun EventItem(event : MyEvent){
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f))
            {
                Text(text = event.title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = "${event.eventStartDate} to ${event.eventEndDate}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}