package com.lugmety.orderassistant.data.local.dao

import androidx.room.*
import com.lugmety.orderassistant.data.local.entity.OrderEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDao {
    @Query("SELECT * FROM order_events ORDER BY detectedAt DESC")
    fun getAllOrders(): Flow<List<OrderEventEntity>>

    @Query("SELECT COUNT(*) FROM order_events WHERE id = :orderId")
    suspend fun countById(orderId: String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrder(order: OrderEventEntity): Long

    @Query("DELETE FROM order_events")
    suspend fun clearAll()
}
