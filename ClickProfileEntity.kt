package com.lugmety.orderassistant.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "click_profiles")
data class ClickProfileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val intervalMs: Long,
    val gestureDurationMs: Long,
    val createdAt: Long,
    val updatedAt: Long
)
