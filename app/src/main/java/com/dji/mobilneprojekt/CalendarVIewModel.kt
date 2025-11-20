package com.dji.mobilneprojekt
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.util.copy
import com.google.firebase.firestore.firestore
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

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
        loadFirestoreEvents(_uiState.value.selectedDate)
    }


    // 1. Add a new StateFlow for Google events
    private val _googleEvents = MutableStateFlow<List<MyEvent>>(emptyList())

    // 2. Update uiState to be a *combination* of both lists
    val uiStateCalendar : StateFlow<CalendarUiState> =
        combine(_uiState, _googleEvents) {firestoreState, googleEventsList ->
            firestoreState.copy(events = firestoreState.events + googleEventsList)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CalendarUiState())

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
        loadFirestoreEvents(date)
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
                loadFirestoreEvents(date)
            }
            .addOnFailureListener {
                //Handle Error
                print("Error saving event")
                return@addOnFailureListener
            }
    }


    // --- Private Logic (Data Fetching) ---
    private fun loadFirestoreEvents(date: LocalDate?) {
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

    private fun loadGoogleEventsForDate(date: LocalDate?){

    }
    private fun loadEventsForDate(date: LocalDate?) {

    }
}
//need to ask for explanation on this function
fun Long.toLocalDate(): LocalDate {
    return Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
}