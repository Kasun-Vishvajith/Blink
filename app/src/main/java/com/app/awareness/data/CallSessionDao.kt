package com.app.awareness.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CallSessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: CallSessionEntity)

    @Query("SELECT * FROM call_sessions WHERE date = :date")
    fun queryByDate(date: String): Flow<List<CallSessionEntity>>

    @Query("SELECT * FROM call_sessions WHERE package_name=:pkg AND date=:date AND end_time IS NULL LIMIT 1")
    suspend fun getOpenSession(pkg: String, date: String): CallSessionEntity?

    @Query("UPDATE call_sessions SET end_time=:endTime, duration_minutes=:mins WHERE id=:id")
    suspend fun closeSession(id: Long, endTime: Long, mins: Int)
}
