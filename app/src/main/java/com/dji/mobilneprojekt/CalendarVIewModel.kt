package com.dji.mobilneprojekt
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.firestore
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MyEvent(
    val id: String = "",
    val title: String = "",
    val eventDate: String = "",
    val eventStartDate : String = "",
    val eventEndDate : String = ""
)

data class CalendarUiState (
    val selectedDate: LocalDate? = LocalDate.now(),
    val events: List<MyEvent> = emptyList(),
    val isDatePickerDialogVisible : Boolean = false
)

class CalendarViewModel : ViewModel() {

    private val db = Firebase.firestore
    private val auth by lazy { FirebaseAuth.getInstance() }

    private val currentUserId: String?
        get() = auth.currentUser?.uid

    private val firestoreDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    init {
        loadEventsForDate(_uiState.value.selectedDate)
    }

    //Actions called by the View
    fun showDatePickerDialog(){
        _uiState.update {it.copy(isDatePickerDialogVisible = true)}
    }

    fun hideDatePickerDialog(){
        _uiState.update {it.copy(isDatePickerDialogVisible = false)}
    }

    fun selectDate(dateMillis : Long?){
        val date = dateMillis?.toLocalDate()
        _uiState.update {it.copy(selectedDate = date)}
        loadEventsForDate(date)
    }

    fun saveEvent(title: String, date: LocalDate){
        val userId = currentUserId ?: return
        val dateString = date.format(firestoreDateFormatter)
        val newEvent = MyEvent(
            title = title,
            eventDate = dateString,
            eventStartDate = dateString,
            eventEndDate = dateString
            )
        db.collection("users").document(userId).collection("events").add(newEvent)
            .addOnSuccessListener {
                loadEventsForDate(date)
            }
            .addOnFailureListener {
                //Handle Error
                print("Error saving event")
                return@addOnFailureListener
            }
    }


    // --- Private Logic (Data Fetching) ---
    private fun loadEventsForDate(date: LocalDate?) {
        val userId = currentUserId
        if (date == null || userId == null) {
            _uiState.update { it.copy(events = emptyList()) }
            return
        }
        val dateString = date.format(firestoreDateFormatter)

        viewModelScope.launch {
            try {
                db.collection("users")
                    .document(userId)
                    .collection("events")
                    .whereEqualTo("eventDate", dateString).get().addOnSuccessListener {
                        querySnapshot -> _uiState.update {
                           it.copy(events = querySnapshot.toObjects(MyEvent::class.java))
                    }
            }
                    .addOnFailureListener {
                        //Handle Error
                        _uiState.update {it.copy(events = emptyList())}
                    }
            } catch (e : Exception){
                //Handle error
                print("Error loading events for date: $dateString")
            }
        }
    }
}
//need to ask for explanation on this function
fun Long.toLocalDate(): LocalDate {
    return Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
}