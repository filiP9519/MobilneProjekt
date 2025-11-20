package com.dji.mobilneprojekt

import android.annotation.SuppressLint
import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import kotlinx.coroutines.flow.asStateFlow

data class Holiday (
    val date: String,
    val localName: String,
    val name: String,
    val countryCode: String
)
data class MyEvent(
    val id: String = "",
    val title: String = "",
    val eventStartDate : String = "",
    val eventEndDate : String = ""
)

data class CalendarUiState (
    val selectedDate: LocalDate? = LocalDate.now(),
    val events: List<MyEvent> = emptyList(),
    val holidays : List <Holiday> = emptyList(),
    val isDatePickerDialogVisible : Boolean = false
)



class CalendarViewModel(application: Application) : AndroidViewModel(application) { // why did we changed this ?
    private val db by lazy { Firebase.firestore }
    private val auth by lazy { FirebaseAuth.getInstance() }

    private val eventCache = mutableMapOf<String, List<MyEvent>>()
    private val _datesWithEvents = MutableStateFlow<Set<LocalDate>>(emptySet())
    val datesWithEvents: StateFlow<Set<LocalDate>> = _datesWithEvents.asStateFlow()
    private val currentUserId: String?
        get() = auth.currentUser?.uid
    private val firestoreDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val _uiState = MutableStateFlow(CalendarUiState())
    //private val _deviceEvents = MutableStateFlow<List<MyEvent>>(emptyList())

    private val _apiHolidays = MutableStateFlow<List<Holiday>>(emptyList())
    // CRITICAL: This list holds ALL holidays for the year (cached from API)
    // We fetch this once, and filter from it later.
    private var allCachedHolidays = listOf<Holiday>()
    val uiState: StateFlow<CalendarUiState> =
        combine(_uiState, _apiHolidays) {firestoreState, apiHolidays ->
            firestoreState.copy(
                events = firestoreState.events,
                holidays = apiHolidays
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CalendarUiState())

    init {
        // 1. Fetch the whole year's holidays immediately
        fetchHolidaysForYear()
        // 2. Select today's date (which will trigger loading other events)
        selectDate(Instant.now().toEpochMilli())
        loadEventsForMonth(LocalDate.now().year, LocalDate.now().monthValue)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    fun onVisibleMonthChanged(datePickerState: DatePickerState) {
        datePickerState.displayedMonthMillis?.let { monthMillis ->
            val monthDate = Instant.ofEpochMilli(monthMillis).atZone(ZoneId.systemDefault()).toLocalDate()
            loadEventsForMonth(monthDate.year, monthDate.monthValue)
        }
    }
    //Actions called by the View
    fun showDatePickerDialog(){ _uiState.update {it.copy(isDatePickerDialogVisible = true)} }
    fun hideDatePickerDialog(){ _uiState.update {it.copy(isDatePickerDialogVisible = false)} }
    fun selectDate(dateMillis : Long?){
        if (dateMillis == null) return
        val date = dateMillis.toLocalDate()
        _uiState.update {
            it.copy(
                selectedDate = date
                //events = emptyList()
            )
        }
        loadFirestoreEvents(date)

        val dateString = date.format(firestoreDateFormatter)
        val holidaysForThisDay = allCachedHolidays.filter {it.date == dateString}
        _apiHolidays.value = holidaysForThisDay
    }
    fun saveEvent(title: String, date: LocalDate){
        val userId = currentUserId ?: return
        val dateString = date.format(firestoreDateFormatter)
        val newEvent = MyEvent(
            title = title,
            eventStartDate = dateString,
            eventEndDate = dateString
            )

        _uiState.update { it.copy(events = it.events + newEvent) }

        //Cache update
        //adding events to cache manually
        val currentCachedList = eventCache[dateString]?:emptyList()
        eventCache[dateString] = currentCachedList + newEvent

        db.collection("users").document(userId).collection("events").add(newEvent)
            //ID patching
            //The event is saved, but local copy has no ID, fix here:
            .addOnSuccessListener { documentReference->
                val id = documentReference.id
                val eventWithId = newEvent.copy(id = id)

                //Update UI State with ID-version of event
                _uiState.update { state ->
                    val updatedList = state.events.map {
                        if (it == newEvent) eventWithId else it
                    }
                    state.copy(events = updatedList)
                }
                // Update Cache with the ID-version of the event
                eventCache[dateString]?.let { cachedList ->
                    val updatedCache = cachedList.map {
                        if (it == newEvent) eventWithId else it
                    }
                    eventCache[dateString] = updatedCache
                }
                Log.d("ViewModel", "Event saved and ID patched: $id")
                // CRITICAL: Do NOT call loadFirestoreEvents(date) here.
                // It avoids the "Stale Query" race condition.
            }
            .addOnFailureListener {
                //Handle Error
                print("Error saving event")
                // Only on failure do we invalidate cache to force a refresh/retry state
                eventCache.remove(dateString)
                loadFirestoreEvents(date)
            }
    }


    private fun fetchHolidaysForYear() {
        viewModelScope.launch(Dispatchers.IO) {
            try{
                val currentYear = LocalDate.now().year
                val rawHolidays = RetrofitInstance.api.getHolidays(currentYear, "SK")

                allCachedHolidays = rawHolidays
                Log.d("API_SUCCESS", "Cached ${allCachedHolidays.size} holidays for year $currentYear")

                _uiState.value.selectedDate?.let {date->
                    val dateString = date.format(firestoreDateFormatter)
                    val holidaysForThisDay = allCachedHolidays.filter {it.date == dateString}
                    _apiHolidays.value = holidaysForThisDay

                }

            }
            catch(e: Exception){
                Log.e("API_ERROR", "Failed to fetch holidays: ${e.message}")
                allCachedHolidays = emptyList()
            }
        }
    }

    private fun loadFirestoreEvents(date: LocalDate?){
        val userId = currentUserId


        if (date == null || userId == null) {
            _uiState.update { it.copy(events = emptyList()) }
            return
        }

        val dateString = date.format(firestoreDateFormatter)

        if(eventCache.containsKey(dateString)){
            _uiState.update { it.copy(events = eventCache[dateString] ?: emptyList()) }
            return
        }

        viewModelScope.launch{
            db.collection("users")
                .document(userId)
                .collection("events")
                .whereEqualTo("eventStartDate", dateString)
                .get()
                .addOnSuccessListener { querySnapshot ->

                    val firestoreEvents: List<MyEvent> = querySnapshot.documents.mapNotNull { document ->
                        val event = document.toObject(MyEvent::class.java)
                        event?.copy(id = document.id)
                    }

                    eventCache[dateString] = firestoreEvents

                    _uiState.update {it.copy(events = firestoreEvents) }
                }
                .addOnFailureListener {
                    //Handle Error
                    _uiState.update {it.copy(events = emptyList())}
                }
        }
    }

    @SuppressLint("SuspiciousIndentation")
    private fun loadEventsForMonth(year : Int, month: Int){
        val userId = currentUserId ?: return

        viewModelScope.launch(Dispatchers.IO){
            val eventDates = mutableSetOf<LocalDate>()

            db.collection("users").document(userId).collection("events").get()
                .addOnSuccessListener { snapshot ->
                    snapshot.documents.forEach { doc ->
                        try{
                            val eventDateStr = doc.getString("eventStartDate")
                            if (eventDateStr != null){
                            val eventDate = LocalDate.parse(eventDateStr, firestoreDateFormatter)
                                if (eventDate.year == year && eventDate.monthValue == month){
                                eventDates.add(eventDate)
                                }
                            }
                        } catch (e: Exception){
                            Log.e("ViewModel", "Failed to parse date from Firestore", e)
                        }
                    }
                    allCachedHolidays.forEach {holiday ->
                        try{
                            val holidayDate = LocalDate.parse(holiday.date, firestoreDateFormatter)
                            if(holidayDate.year == year && holidayDate.monthValue == month){
                                eventDates.add(holidayDate)
                            }
                        } catch (e: Exception){
                            Log.e("ViewModel", "Failed to parse date from Holiday cache", e)
                        }
                    }
                    _datesWithEvents.value = eventDates
                }
                .addOnFailureListener {
                    Log.e("ViewModel", "Failed to load month events from Firestore", it)
                }
        }
    }

    fun deleteEvent (eventId: String) {

        if(eventId.isBlank() || eventId == "device" ){
            Log.w("ViewModel", "Invalid event ID: $eventId")
            return
        }

        val userId = currentUserId?: return
        _uiState.value.selectedDate?.let {date ->
            val dateString = date.format(firestoreDateFormatter)
            eventCache.remove(dateString)
        }
        try{
            _uiState.update { currentState ->
                val updatedEvents = currentState.events.filter {it.id != eventId}
                currentState.copy(events = updatedEvents)
            }

            db.collection("users").document(userId).collection("events").document(eventId).delete()
                .addOnSuccessListener { loadFirestoreEvents(_uiState.value.selectedDate)
                    Log.d("Firestore", "Event $eventId successfully deleted.")
                }
                .addOnFailureListener {e->
                    Log.w("Firestore", "Error deleting document", e)
                    _uiState.value.selectedDate.let{ loadFirestoreEvents(it) }
                }
        }catch (e: Exception){
            Log.e("ViewModel", "Failed to delete event", e)
        }

    }

fun Long.toLocalDate(): LocalDate {
    return Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
    }
}