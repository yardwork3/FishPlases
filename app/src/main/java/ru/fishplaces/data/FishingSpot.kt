package ru.fishplaces.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fishing_spots")
data class FishingSpot(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val fishTypes: String = "",
    val biteRating: Int? = null,
    val note: String = "",
    val isFavorite: Boolean = false,
    val category: String = "fishing",
    val createdAt: Long = System.currentTimeMillis(),
)

/** Pure validation also runs in the repository, so UI cannot bypass it. */
object SpotValidation {
    fun validName(value: String) = value.isNotBlank()
    fun validRating(value: Int?) = value == null || value in 1..5
    fun validCoordinates(latitude: Double, longitude: Double) =
        latitude.isFinite() && longitude.isFinite() && latitude in -90.0..90.0 &&
            longitude in -180.0..180.0

    fun requireValid(spot: FishingSpot) {
        require(validName(spot.name))
        require(validRating(spot.biteRating))
        require(validCoordinates(spot.latitude, spot.longitude))
        require(spot.category == "fishing")
    }
}
