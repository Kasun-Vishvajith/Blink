package com.app.awareness.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaSessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: MediaSessionEntity)

    @Query("SELECT * FROM media_sessions WHERE date = :date")
    fun queryByDate(date: String): Flow<List<MediaSessionEntity>>

    @Query("SELECT play_minutes FROM media_sessions WHERE package_name = :pkg AND date = :date LIMIT 1")
    suspend fun getPlayMinutesToday(pkg: String, date: String): Int?
}
