@file:OptIn(ExperimentalCoroutinesApi::class)

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

data class CalendarUiState (
    val selectedDate: LocalDate? = LocalDate.now(),
    val events: List<EventEntity> = emptyList(),
    val holidays : List <Holiday> = emptyList(),
    val isDatePickerDialogVisible : Boolean = false
)



class CalendarViewModel(application: Application) : AndroidViewModel(application) {


    private val repository = Repository(AppDatabase.getDatabase(application))

    private val _currentUserID = MutableStateFlow(1)


    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    private val _isDatePickerVisible = MutableStateFlow(false)
    private val _apiHolidays = MutableStateFlow<List<Holiday>>(emptyList())
    private var allCachedHolidays = listOf<Holiday>()

    private val eventsFlow: Flow<List<EventEntity>> = combine(_selectedDate, _currentUserID) { date, userId ->
        Pair(date, userId)
    }.flatMapLatest { (date, userId) ->
        if (userId != -1) {
            Log.d("CalendarViewModel", "Fetching events for User: $userId on Date: $date")
            // Because Room returns a Flow, this will AUTOMATICALLY update when you save/delete events!
            repository.getEvents(date, userId)
        } else {
            // If no valid user, return empty list
            Log.d("CalendarViewModel", "No valid user ($userId), returning empty list")
            flowOf(emptyList<EventEntity>())
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<CalendarUiState> = combine(
        _selectedDate,
        _isDatePickerVisible,
        _apiHolidays,
        eventsFlow
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

    fun setUserId(id : Int) {
        _currentUserID.value = id
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
        val userId = _currentUserID.value
        if (userId != -1) {
            viewModelScope.launch(Dispatchers.IO) {
                Log.d("CalendarViewModel", "Attempting to save event: $title for user $userId")
                repository.addEvent(title, date, userId)
                // Note: No need to call selectDate() manually.
                // The Room Flow inside eventsFlow will detect the database change and update the UI automatically.
            }
        } else {
            Log.e("CalendarViewModel", "Cannot save event: UserID is still -1")
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
        val userId = _currentUserID.value
        if (userId != -1) {
            viewModelScope.launch(Dispatchers.IO) {
                repository.deleteEvent(eventId)
            }
        }
    }
    }




fun Long.toLocalDate(): LocalDate {
    return Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
    }
