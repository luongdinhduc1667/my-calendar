package vn.lichviet.app.horoscope

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface HoroscopeProfileDao {
    @Query("SELECT * FROM horoscope_profiles ORDER BY id")
    fun observeAll(): Flow<List<HoroscopeProfileEntity>>
    @Query("SELECT * FROM horoscope_profiles ORDER BY id")
    suspend fun getAll(): List<HoroscopeProfileEntity>
    @Query("SELECT * FROM horoscope_profiles WHERE id = :id")
    suspend fun getById(id: Long): HoroscopeProfileEntity?
    @Insert suspend fun insert(profile: HoroscopeProfileEntity): Long
    @Update suspend fun update(profile: HoroscopeProfileEntity): Int
    suspend fun save(profile: HoroscopeProfileEntity): Long {
        if (profile.id == 0L) return insert(profile)
        check(update(profile) == 1) { "Profile no longer exists" }
        return profile.id
    }
    @Query("DELETE FROM horoscope_profiles WHERE id = :id")
    suspend fun delete(id: Long)
    @Query("DELETE FROM horoscope_profiles")
    suspend fun deleteAll()
}
