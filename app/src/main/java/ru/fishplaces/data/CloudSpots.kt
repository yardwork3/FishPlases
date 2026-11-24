package ru.fishplaces.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

/** IDs remain Long for compatibility with the existing Room database and map.
 * The decimal ID is also the exact Firestore document ID. */
interface CloudSpots {
    fun observe(uid: String): Flow<List<FishingSpot>>
    suspend fun save(uid: String, spot: FishingSpot): Long
    suspend fun delete(uid: String, id: Long)
    suspend fun favorite(uid: String, id: Long, value: Boolean)
}
class FirestoreSpots(private val db: FirebaseFirestore) : CloudSpots {
    private val failures = MutableSharedFlow<Pair<String, Exception>>(extraBufferCapacity = 16)
    private fun collection(uid: String) = db.collection("users").document(uid).collection("spots")
    override fun observe(uid: String) = callbackFlow {
        val errors = launch { failures.collect { (owner, error) -> if (owner == uid) close(error) } }
        val registration = collection(uid).addSnapshotListener { snapshot, error ->
            if (error != null) close(error)
            else try {
                trySend(snapshot!!.documents.map { d ->
                    FishingSpot(id = d.id.toLong(), name = d.getString("name").orEmpty(),
                        latitude = d.getDouble("latitude") ?: Double.NaN,
                        longitude = d.getDouble("longitude") ?: Double.NaN,
                        fishTypes = d.getString("fishTypes").orEmpty(), biteRating = d.getLong("biteRating")?.toInt(),
                        note = d.getString("note").orEmpty(), isFavorite = d.getBoolean("isFavorite") ?: false,
                        category = d.getString("category") ?: "fishing", createdAt = d.getLong("createdAt") ?: 0L)
                        .also(SpotValidation::requireValid)
                }.sortedByDescending { it.createdAt })
            } catch (e: Exception) { close(e) }
        }
        awaitClose { registration.remove(); errors.cancel() }
    }
    override suspend fun save(uid: String, spot: FishingSpot): Long {
        SpotValidation.requireValid(spot)
        val id = spot.id.takeIf { it != 0L } ?: (java.util.UUID.randomUUID().mostSignificantBits and Long.MAX_VALUE).coerceAtLeast(1)
        // Queue writes locally: awaiting server acknowledgement would block forever offline.
        collection(uid).document(id.toString()).set(mapOf("name" to spot.name.trim(),
            "latitude" to spot.latitude, "longitude" to spot.longitude, "fishTypes" to spot.fishTypes.trim(),
            "biteRating" to spot.biteRating, "note" to spot.note.trim(), "isFavorite" to spot.isFavorite,
            "category" to spot.category, "createdAt" to spot.createdAt))
            .addOnFailureListener { failures.tryEmit(uid to it) }
        return id
    }
    override suspend fun delete(uid: String, id: Long) { collection(uid).document(id.toString()).delete().addOnFailureListener { failures.tryEmit(uid to it) } }
    override suspend fun favorite(uid: String, id: Long, value: Boolean) { collection(uid).document(id.toString()).update("isFavorite", value).addOnFailureListener { failures.tryEmit(uid to it) } }
}
