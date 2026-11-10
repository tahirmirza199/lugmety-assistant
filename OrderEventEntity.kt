package com.lugmety.orderassistant.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "order_events")
data class OrderEventEntity(
    @PrimaryKey val id: String,
    val orderNumber: String,
    val storeName: String,
    val pickupAddress: String,
    val dropoffAddress: String,
    val payoutEstimate: String?,
    val detectedAt: Long,
    val status: String,
    val isSimulated: Boolean = false
)
