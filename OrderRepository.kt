package com.lugmety.orderassistant.data.repository

import com.lugmety.orderassistant.data.local.dao.ClickProfileDao
import com.lugmety.orderassistant.data.local.dao.LogDao
import com.lugmety.orderassistant.data.local.dao.OrderDao
import com.lugmety.orderassistant.data.local.dao.ProfileWithPoints
import com.lugmety.orderassistant.data.local.entity.ClickPointEntity
import com.lugmety.orderassistant.data.local.entity.ClickProfileEntity
import com.lugmety.orderassistant.data.local.entity.DiagnosticLogEntity
import com.lugmety.orderassistant.data.local.entity.OrderEventEntity
import kotlinx.coroutines.flow.Flow

class OrderRepository(
    private val orderDao: OrderDao,
    private val logDao: LogDao,
    private val clickProfileDao: ClickProfileDao? = null
) {
    val allOrders: Flow<List<OrderEventEntity>> = orderDao.getAllOrders()
    val recentLogs: Flow<List<DiagnosticLogEntity>> = logDao.getRecentLogs()

    // Auto Clicker profiles stream
    val allProfiles: Flow<List<ProfileWithPoints>>? = clickProfileDao?.getAllProfilesWithPoints()

    suspend fun isDuplicate(eventId: String): Boolean {
        return orderDao.countById(eventId) > 0
    }

    suspend fun recordNewOrder(order: OrderEventEntity) {
        orderDao.insertOrder(order)
    }

    suspend fun insertLog(log: DiagnosticLogEntity) {
        logDao.insertLog(log)
    }

    suspend fun clearLogs() {
        logDao.clearLogs()
    }

    suspend fun clearOrders() {
        orderDao.clearAll()
    }

    // Click profile operations
    suspend fun saveClickProfile(profile: ClickProfileEntity, points: List<ClickPointEntity>) {
        clickProfileDao?.saveProfileWithPoints(profile, points)
    }

    suspend fun deleteClickProfile(profile: ClickProfileEntity) {
        clickProfileDao?.deleteProfile(profile)
    }

    suspend fun getProfile(profileId: String): ProfileWithPoints? {
        return clickProfileDao?.getProfileWithPoints(profileId)
    }
}
