package com.example.core.repository

import com.example.core.database.TrafficDao
import com.example.core.model.TrafficEntry
import kotlinx.coroutines.flow.Flow

class TrafficRepository(private val trafficDao: TrafficDao) {
    val allTraffic: Flow<List<TrafficEntry>> = trafficDao.getAllTrafficFlow()
    val totalCount: Flow<Int> = trafficDao.getCountFlow()

    fun getTrafficByIdFlow(id: Long): Flow<TrafficEntry?> = trafficDao.getByIdFlow(id)

    suspend fun getTrafficById(id: Long): TrafficEntry? = trafficDao.getById(id)

    suspend fun insert(entry: TrafficEntry): Long = trafficDao.insert(entry)

    suspend fun update(entry: TrafficEntry) = trafficDao.update(entry)

    suspend fun clearAll() = trafficDao.clearAll()

    suspend fun deleteById(id: Long) = trafficDao.deleteById(id)
}
