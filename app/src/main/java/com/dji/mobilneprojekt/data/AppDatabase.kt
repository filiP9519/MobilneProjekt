package com.dji.mobilneprojekt.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

// ENTITIES (The Tables)

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val userID: Int = 0,
    val username : String,
    val password : String
)

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val eventID : Int = 0,
    val ownerID : Int, //Links to specific UserEntity (userID)
    val title : String,
    val date : LocalDate,
    val startTime : String? = null,
    val endTime : String? = null,
)

// DAOs (The Data Access Objects)

@Dao
interface UserDao {
    //suspends inserts with same ID/Username/etc..
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun registerUser(user : UserEntity) : Long

    @Query("SELECT * FROM users where username = :username AND password = :password LIMIT 1")
    suspend fun loginUser(username: String, password:String) : UserEntity?

    @Query("SELECT * FROM users where username = :username LIMIT 1")
    suspend fun checkIfUserExists(username :String): UserEntity?
}

@Dao
interface eventDao {
    @Query("SELECT * FROM events where date = :date AND ownerID = :userID")
    fun getEventsForDate(date : LocalDate, userID : Int): Flow<List<EventEntity>>

    @Insert
    suspend fun insertEvent(event: EventEntity)

    @Query("DELETE FROM events WHERE eventID = :id")
    suspend fun deleteEvent(id: Int)

    //Delete All events for specific user
    @Query("DELETE FROM events where ownerID = :userID")
    suspend fun deleteAllEventsForUser(userID : Int)
}

//CONVERTERS (LocalDate <-> String)

class Converters {
    @TypeConverter
    fun fromTimestamp(value : String?): LocalDate? {
        return value?.let {LocalDate.parse(it)}
    }

    @TypeConverter
    fun dateToTimestamp(date : LocalDate?): String? {
        return date?.toString()
    }
}

// DATABASE Instance

@Database(entities = [EventEntity::class, UserEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao() : UserDao
    abstract fun eventDao() : eventDao

    companion object {
        @Volatile
        private var INSTANCE : AppDatabase? = null

        fun getDatabase(context: Context) : AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "local_calendar_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}



