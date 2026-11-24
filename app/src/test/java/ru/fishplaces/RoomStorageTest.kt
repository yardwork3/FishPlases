package ru.fishplaces

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.SQLiteMode
import ru.fishplaces.data.*

/** Real Room/SQLite on the JVM. Does not claim to test MapLibre rendering or phone permissions. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class, manifest = Config.NONE)
@SQLiteMode(SQLiteMode.Mode.NATIVE)
class RoomStorageTest {
    @Test fun roomPersistsEditsFavoritesAndDeletionAcrossReopening() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val filename = "room-jvm.db"
        context.deleteDatabase(filename)
        fun open() = Room.databaseBuilder(context, SpotDatabase::class.java, filename).build()
        var db = open()
        try {
            var repository = SpotRepository(db.spotDao())
            val id = repository.save(FishingSpot(name = "  Заводь  ", latitude = 55.0, longitude = 83.0))
            val original = repository.spots.first().single()
            assertNull(original.biteRating)
            assertEquals("Заводь", original.name)
            repository.save(original.copy(name = "У моста", biteRating = 5, note = "Камыш"))
            repository.setFavorite(id, true)
            db.close()
            db = open()
            repository = SpotRepository(db.spotDao())
            val restored = repository.spots.first().single()
            assertEquals(id, restored.id)
            assertEquals(original.createdAt, restored.createdAt)
            assertEquals("У моста", restored.name)
            assertEquals("Камыш", restored.note)
            assertEquals(5, restored.biteRating)
            assertTrue(restored.isFavorite)
            repository.delete(id)
            db.close()
            db = open()
            assertTrue(db.spotDao().observeAll().first().isEmpty())
        } finally { db.close(); context.deleteDatabase(filename) }
    }
}
