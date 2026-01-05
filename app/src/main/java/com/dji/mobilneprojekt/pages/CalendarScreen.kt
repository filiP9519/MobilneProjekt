package com.dji.mobilneprojekt.pages


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextField
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import java.time.LocalDate
import com.dji.mobilneprojekt.CalendarViewModel
import java.time.Instant
import com.dji.mobilneprojekt.AuthViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import com.dji.mobilneprojekt.Holiday
import com.dji.mobilneprojekt.data.EventEntity

fun LocalDate.formatAsString() : String {
    val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    return this.format(formatter)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    modifier: Modifier = Modifier,
    navController: NavController,
    viewModel: CalendarViewModel,
    authViewModel: AuthViewModel
) {
    val userId by authViewModel.currentUserId.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    var eventTitle by remember { mutableStateOf("") }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = Instant.now().toEpochMilli()
    )

    LaunchedEffect(datePickerState.selectedDateMillis) {
        viewModel.selectDate(datePickerState.selectedDateMillis)
    }
    LaunchedEffect(userId) {
        if (userId != -1){
            viewModel.setUserId(userId)
        }else {
            Error()
        }
    }

    Column(
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

        TextField(
            value = eventTitle,
            onValueChange = { eventTitle = it },
            label = { Text("Event Title") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                val currentDate = uiState.selectedDate
                if(eventTitle.isNotBlank() && currentDate != null){
                    // Use the selected date from UI state directly
                    viewModel.saveEvent(eventTitle, currentDate)
                    eventTitle = ""
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add event")
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Events on this day:",
            style = MaterialTheme.typography.titleMedium
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 200.dp)
        ) {

            // --- SECTION 1: PUBLIC HOLIDAYS ---
            items(uiState.holidays) { holiday ->
                HolidayItem(holiday)
            }

            // --- SECTION 2: USER EVENTS (Firebase + Device) ---
            if (uiState.events.isEmpty() && uiState.holidays.isEmpty()) {
                item {
                    Text(
                        text = "No events.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                items(uiState.events) { event ->
                    EventItem(
                        event = event,
                        onDelete = {eventId ->
                            viewModel.deleteEvent(eventId)
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
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
}

// A standard item for User/Device events
@Composable
fun EventItem(event: EventEntity, onDelete: (Int) -> Unit) {
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
            Column(modifier = Modifier.weight(1f)) {
                Text(text = event.title, style = MaterialTheme.typography.bodyLarge)
                // If you want to show date details, you can add them here
            }

            IconButton(onClick = { onDelete(event.eventID) }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Event",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

// A Distinct item for Public Holidays (Different Color/Style)
@Composable
fun HolidayItem(holiday: Holiday) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🎉 ${holiday.name}", // Add an icon for fun
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

