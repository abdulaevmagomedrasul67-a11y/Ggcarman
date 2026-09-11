package com.example.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.core.model.TrafficEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface TrafficDao {
    @Query("SELECT * FROM traffic_entries ORDER BY timestamp DESC")
    fun getAllTrafficFlow(): Flow<List<TrafficEntry>>

    @Query("SELECT * FROM traffic_entries WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): TrafficEntry?

    @Query("SELECT * FROM traffic_entries WHERE id = :id LIMIT 1")
    fun getByIdFlow(id: Long): Flow<TrafficEntry?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: TrafficEntry): Long

    @Update
    suspend fun update(entry: TrafficEntry)

    @Query("DELETE FROM traffic_entries")
    suspend fun clearAll()

    @Query("DELETE FROM traffic_entries WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM traffic_entries")
    fun getCountFlow(): Flow<Int>
}
