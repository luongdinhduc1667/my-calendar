package vn.lichviet.app.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Query("SELECT * FROM events ORDER BY solarDate, hour, minute, id")
    fun observeAll(): Flow<List<EventEntity>>
    @Query("SELECT * FROM events ORDER BY solarDate, hour, minute, id")
    suspend fun getAll(): List<EventEntity>
    @Query("SELECT * FROM events WHERE id = :id")
    suspend fun getById(id: Long): EventEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(event: EventEntity): Long
    @Query("DELETE FROM events WHERE id = :id")
    suspend fun delete(id: Long)
    @Query("DELETE FROM events")
    suspend fun deleteAll()
}

class EventConverters {
    @TypeConverter fun repeatToString(value: RepeatRule): String = value.name
    @TypeConverter fun stringToRepeat(value: String): RepeatRule = RepeatRule.valueOf(value)
    @TypeConverter fun typeToString(value: EventType): String = value.name
    @TypeConverter fun stringToType(value: String): EventType = EventType.valueOf(value)
    @TypeConverter fun leapToString(value: LeapMonthPolicy): String = value.name
    @TypeConverter fun stringToLeap(value: String): LeapMonthPolicy = LeapMonthPolicy.valueOf(value)
    @TypeConverter fun missingToString(value: MissingDayPolicy): String = value.name
    @TypeConverter fun stringToMissing(value: String): MissingDayPolicy = MissingDayPolicy.valueOf(value)
}

@Database(entities = [EventEntity::class], version = 1, exportSchema = true)
@TypeConverters(EventConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao
    companion object {
        @Volatile private var instance: AppDatabase? = null
        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "lich-viet.db")
                .build().also { instance = it }
        }
    }
}
