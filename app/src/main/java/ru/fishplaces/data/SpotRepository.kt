package ru.fishplaces.data

class SpotRepository private constructor(private val dao: SpotDao?, private val cloud: CloudSpots?, private val uid: String?) {
    constructor(dao: SpotDao) : this(dao, null, null)
    constructor(cloud: CloudSpots, uid: String) : this(null, cloud, uid)
    val spots = cloud?.observe(requireNotNull(uid)) ?: requireNotNull(dao).observeAll()

    suspend fun save(spot: FishingSpot): Long {
        SpotValidation.requireValid(spot)
        if (cloud != null) return cloud.save(requireNotNull(uid), spot)
        val clean = spot.copy(name = spot.name.trim(), fishTypes = spot.fishTypes.trim(), note = spot.note.trim())
        return if (clean.id == 0L) requireNotNull(dao).insert(clean) else {
            check(requireNotNull(dao).update(clean) == 1)
            clean.id
        }
    }

    suspend fun delete(id: Long) { if (cloud != null) cloud.delete(requireNotNull(uid), id) else check(requireNotNull(dao).delete(id) == 1) }
    suspend fun setFavorite(id: Long, favorite: Boolean) {
        if (cloud != null) cloud.favorite(requireNotNull(uid), id, favorite) else check(requireNotNull(dao).setFavorite(id, favorite) == 1)
    }
}
