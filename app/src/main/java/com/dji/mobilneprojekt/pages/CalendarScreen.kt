package com.dji.mobilneprojekt.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.rememberTooltipState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import java.time.LocalDate
import com.dji.mobilneprojekt.CalendarViewModel
import java.time.Instant
import com.dji.mobilneprojekt.AuthViewModel



fun LocalDate.formatAsString() : String {
    val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    return this.format(formatter)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(modifier: Modifier = Modifier, navController: NavController ,viewModel: CalendarViewModel, authViewModel : AuthViewModel){

    //observe the state from viewModel
    val uiState by viewModel.uiState.collectAsState()

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = Instant.now().toEpochMilli())

    Column(

            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally

    ) {
        Button(onClick = {viewModel.showDatePickerDialog() }) {
            Text("Show Calendar Dialog")
        }
        Spacer(Modifier.height(16.dp))

        Text(
            text = uiState.selectedDate?.formatAsString() ?: "No date selected",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(Modifier.height(16.dp))

        Text("Events on this day: ${uiState.events.size}")

        Button(onClick = {
            uiState.selectedDate?.let { date -> viewModel.saveEvent("New event Title", date) }
        }) {
            Text("Add Event")
        }

        if (uiState.isDatePickerDialogVisible) {
            DatePickerDialog(
                onDismissRequest = { viewModel.hideDatePickerDialog() },
                confirmButton = {
                    TextButton(onClick = {
                        // --- UI triggers ViewModel actions ---
                        viewModel.selectDate(datePickerState.selectedDateMillis)
                        viewModel.hideDatePickerDialog()
                    }) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    // --- UI triggers ViewModel actions ---
                    TextButton(onClick = { viewModel.hideDatePickerDialog() }) {
                        Text("Cancel")
                    }
                }
            ) {
                DatePicker(
                    state = datePickerState,
                    showModeToggle = false
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            authViewModel.signout()
            navController.navigate("login")
    } ) {
            Text("Wyloguj")
        }
    }
}


