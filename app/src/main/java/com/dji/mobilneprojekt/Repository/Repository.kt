package com.dji.mobilneprojekt.Repository

import com.dji.mobilneprojekt.data.AppDatabase
import com.dji.mobilneprojekt.data.EventEntity
import com.dji.mobilneprojekt.data.UserEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class Repository(private val db: AppDatabase){

    //Auth-logic
    suspend fun login(username : String, password : String): UserEntity?{
        return db.userDao().loginUser(username,password)
    }

    suspend fun register (username : String, password : String) : Boolean {

        val existing = db.userDao().checkIfUserExists(username)
        if(existing != null){return false}

        db.userDao().registerUser(UserEntity(username = username, password = password))
        return true
    }

    //Event logic
    fun getEvents(date:LocalDate, userID: Int):Flow<List<EventEntity>>{
        return db.eventDao().getEventsForDate(date, userID)
    }

    suspend fun addEvent(title : String, date: LocalDate, userID: Int){
        val newEvent = EventEntity(
            title = title,
            date = date,
            ownerID = userID
        )
        db.eventDao().insertEvent(newEvent)
    }

    suspend fun deleteEvent (eventID : Int) {
        db.eventDao().deleteEvent(eventID)
    }

    suspend fun deleteEventsForUser(userID: Int){
        db.eventDao().deleteAllEventsForUser(userID)
    }

}