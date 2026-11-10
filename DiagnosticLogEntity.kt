package com.lugmety.orderassistant.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "diagnostic_logs")
data class DiagnosticLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val level: String, // INFO, SUCCESS, WARN, ERROR
    val category: String, // ACCESSIBILITY, GESTURE, DETECTION, ACTION, SECURITY
    val message: String,
    val details: String? = null
)
