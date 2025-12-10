package com.dji.mobilneprojekt

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import com.dji.mobilneprojekt.Repository.Repository
import com.dji.mobilneprojekt.data.AppDatabase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import com.dji.mobilneprojekt.data.EventEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
    val events: List<EventEntity> = emptyList(),
    val holidays : List <Holiday> = emptyList(),
    val isDatePickerDialogVisible : Boolean = false
)



class CalendarViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = Repository(AppDatabase.getDatabase(application))
    var currentUserID: Int = -1

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    private val _isDatePickerVisible = MutableStateFlow(false)
    private val _apiHolidays = MutableStateFlow<List<Holiday>>(emptyList())
    private var allCachedHolidays = listOf<Holiday>()


    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<CalendarUiState> = combine(
        _selectedDate,
        _isDatePickerVisible,
        _apiHolidays,
        _selectedDate.flatMapLatest { date ->
            if (currentUserID != -1) {
                repository.getEvents(date, currentUserID)
            } else {
                flowOf(emptyList<EventEntity>())
            }
        }
    ) { date, isDialogVisible, holidays, dbEvents ->
        CalendarUiState(
            selectedDate = date,
            isDatePickerDialogVisible = isDialogVisible,
            holidays = holidays,
            events = dbEvents
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CalendarUiState()
    )

    init {
        fetchHolidaysForYear()
    }
    fun selectDate(dateMillis: Long?){
        if(dateMillis == null) return
        val date = dateMillis.toLocalDate()

        _selectedDate.value = date

        val dateString = date.format(dateFormatter)
        _apiHolidays.value = allCachedHolidays.filter { it.date == dateString }
    }

    fun showDatePickerDialog() { _isDatePickerVisible.value = true }
    fun hideDatePickerDialog() { _isDatePickerVisible.value = false }

    fun saveEvent (title : String, date : LocalDate){
        if (currentUserID != -1) {
            viewModelScope.launch(Dispatchers.IO) {
                repository.addEvent(title, date, currentUserID)
              // selectDate(date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli())
            }
        }
    }

    fun fetchHolidaysForYear(){
        viewModelScope.launch(Dispatchers.IO) {
            try{
                val currentYear = LocalDate.now().year
                val rawHolidays = RetrofitInstance.api.getHolidays(currentYear, "SK")
                allCachedHolidays = rawHolidays

                val dateString = _selectedDate.value.format(dateFormatter)
                _apiHolidays.value = allCachedHolidays.filter { it.date == dateString }
            } catch (e: Exception){
                Log.e("ViewModel","API Error", e)
            }
        }
    }
    fun deleteEvent(eventId: Int){
        if (currentUserID != -1){
            viewModelScope.launch(Dispatchers.IO) {
                repository.deleteEvent(eventId)

            }
        }
    }
}


    fun saveEvent(title: String, date: LocalDate){


    }


    private fun fetchHolidaysForYear() {

    }
/*
    private fun loadFirestoreEvents(date: LocalDate?){
        val userId = currentUserId


        if (date == null || userId == null) {
            _uiState.update { it.copy(events = emptyList()) }
            return
        }

        val dateString = date.format(dateFormatter)

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
*/


    fun deleteEvent (eventId: String) {


    }

fun Long.toLocalDate(): LocalDate {
    return Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
    }
