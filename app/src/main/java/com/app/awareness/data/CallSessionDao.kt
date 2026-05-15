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
}
