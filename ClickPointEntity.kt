package com.lugmety.orderassistant.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "click_points",
    foreignKeys = [
        ForeignKey(
            entity = ClickProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["profileId"])]
)
data class ClickPointEntity(
    @PrimaryKey val id: String,
    val profileId: String,
    val orderIndex: Int,
    val x: Float,
    val y: Float,
    val delayMs: Long = 0,
    val label: String = ""
)
