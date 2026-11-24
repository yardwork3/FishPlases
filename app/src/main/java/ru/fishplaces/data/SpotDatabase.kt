package ru.fishplaces.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SpotDao {
    @Query("SELECT * FROM fishing_spots ORDER BY createdAt DESC, id DESC")
    fun observeAll(): Flow<List<FishingSpot>>

    @Insert
    suspend fun insert(spot: FishingSpot): Long

    @Update
    suspend fun update(spot: FishingSpot): Int

    @Query("DELETE FROM fishing_spots WHERE id = :id")
    suspend fun delete(id: Long): Int

    @Query("UPDATE fishing_spots SET isFavorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean): Int
}

@Database(entities = [FishingSpot::class], version = 1, exportSchema = true)
abstract class SpotDatabase : RoomDatabase() {
    abstract fun spotDao(): SpotDao
}
