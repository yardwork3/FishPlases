package ru.fishplaces

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import ru.fishplaces.data.*

class FakeCloudSpots : CloudSpots {
    private val users = mutableMapOf<String, MutableStateFlow<List<FishingSpot>>>()
    override fun observe(uid: String) = users.getOrPut(uid) { MutableStateFlow(emptyList()) }
    override suspend fun save(uid: String, spot: FishingSpot): Long {
        val id = spot.id.takeIf { it != 0L } ?: 1L
        val flow = observe(uid)
        flow.value = flow.value.filterNot { it.id == id } + spot.copy(id = id)
        return id
    }
    override suspend fun delete(uid: String, id: Long) { observe(uid).value = observe(uid).value.filterNot { it.id == id } }
    override suspend fun favorite(uid: String, id: Long, value: Boolean) {
        observe(uid).value = observe(uid).value.map { if (it.id == id) it.copy(isFavorite = value) else it }
    }
}
class CloudSpotsTest {
    @Test fun accountsHaveSeparateData() = runTest {
        val source = FakeCloudSpots()
        val first = SpotRepository(source, "one")
        val second = SpotRepository(source, "two")
        first.save(FishingSpot(name = "Обь", latitude = 55.0, longitude = 83.0))
        assertEquals(1, first.spots.first().size)
        assertTrue(second.spots.first().isEmpty())
    }
}
