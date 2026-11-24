package ru.fishplaces

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import ru.fishplaces.data.*

@RunWith(AndroidJUnit4::class)
class RoomPersistenceTest {
    @Test fun crudSurvivesClosingAndReopeningDatabase() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val filename = "persistence-test.db"
        context.deleteDatabase(filename)
        fun open() = Room.databaseBuilder(context, SpotDatabase::class.java, filename).build()
        var db = open()
        try {
            var repository = SpotRepository(db.spotDao())
            val id = repository.save(FishingSpot(name = "  Обь  ", latitude = 55.0, longitude = 83.0, biteRating = 4))
            val first = repository.spots.first().single()
            assertEquals("Обь", first.name)
            repository.save(first.copy(name = "У моста", note = "Камыш"))
            repository.setFavorite(id, true)
            db.close()
            db = open()
            repository = SpotRepository(db.spotDao())
            val restored = repository.spots.first().single()
            assertEquals(id, restored.id)
            assertEquals(first.createdAt, restored.createdAt)
            assertEquals("У моста", restored.name)
            assertEquals("Камыш", restored.note)
            assertEquals(4, restored.biteRating)
            assertTrue(restored.isFavorite)
            repository.delete(id)
            assertTrue(repository.spots.first().isEmpty())
        } finally { db.close(); context.deleteDatabase(filename) }
    }
}
