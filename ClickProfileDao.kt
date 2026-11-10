package com.lugmety.orderassistant.data.local.dao

import androidx.room.*
import com.lugmety.orderassistant.data.local.entity.ClickPointEntity
import com.lugmety.orderassistant.data.local.entity.ClickProfileEntity
import kotlinx.coroutines.flow.Flow

data class ProfileWithPoints(
    @Embedded val profile: ClickProfileEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "profileId"
    )
    val points: List<ClickPointEntity>
)

@Dao
interface ClickProfileDao {
    @Transaction
    @Query("SELECT * FROM click_profiles ORDER BY updatedAt DESC")
    fun getAllProfilesWithPoints(): Flow<List<ProfileWithPoints>>

    @Transaction
    @Query("SELECT * FROM click_profiles WHERE id = :profileId")
    suspend fun getProfileWithPoints(profileId: String): ProfileWithPoints?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ClickProfileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPoints(points: List<ClickPointEntity>)

    @Query("DELETE FROM click_points WHERE profileId = :profileId")
    suspend fun deletePointsByProfileId(profileId: String)

    @Delete
    suspend fun deleteProfile(profile: ClickProfileEntity)

    @Transaction
    suspend fun saveProfileWithPoints(profile: ClickProfileEntity, points: List<ClickPointEntity>) {
        insertProfile(profile)
        deletePointsByProfileId(profile.id)
        insertPoints(points)
    }
}
