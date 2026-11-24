package ru.fishplaces

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import ru.fishplaces.data.*
import ru.fishplaces.ui.SpotsViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class SpotsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var dao: MemoryDao
    private lateinit var vm: SpotsViewModel
    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
        dao = MemoryDao()
        vm = SpotsViewModel(SpotRepository(dao))
    }
    @After fun tearDown() { vm.viewModelScope.cancel(); Dispatchers.resetMain() }

    @Test fun rapidSaveClicksProduceOneInsertAndTrimName() = runTest(dispatcher) {
        vm.add(55.0, 83.0)
        vm.changeDraft { it.copy(name = "  Тихая заводь  ") }
        repeat(10) { vm.save() }
        assertTrue(vm.state.value.busy)
        advanceUntilIdle()
        assertEquals(1, dao.insertCount)
        assertEquals("Тихая заводь", vm.state.value.spots.single().name)
        assertNull(vm.state.value.draft)
        assertFalse(vm.state.value.busy)
    }

    @Test fun blankNameAndBadRatingNeverReachDatabase() = runTest(dispatcher) {
        vm.add(55.0, 83.0)
        vm.changeDraft { it.copy(name = "   ") }
        vm.save(); advanceUntilIdle()
        assertTrue(vm.state.value.nameError)
        vm.changeDraft { it.copy(name = "Обь", biteRating = 6) }
        vm.save(); advanceUntilIdle()
        assertEquals(0, dao.insertCount)
        assertNotNull(vm.state.value.draft)
    }

    @Test fun failedSaveKeepsFormAndCanBeRetried() = runTest(dispatcher) {
        vm.add(55.0, 83.0)
        vm.changeDraft { it.copy(name = "Обь", note = "У камня") }
        dao.failWrite = true
        vm.save(); advanceUntilIdle()
        assertEquals("У камня", vm.state.value.draft?.note)
        assertEquals(R.string.save_error, vm.state.value.message)
        assertFalse(vm.state.value.busy)
        dao.failWrite = false
        vm.save(); advanceUntilIdle()
        assertEquals(1, vm.state.value.spots.size)
    }

    @Test fun editFavoriteAndDeleteUpdateObservedState() = runTest(dispatcher) {
        vm.add(55.0, 83.0); vm.changeDraft { it.copy(name = "Обь") }
        vm.save(); advanceUntilIdle()
        val original = vm.state.value.spots.single()
        vm.edit(original)
        vm.changeDraft { it.copy(name = "Обь у моста", biteRating = 5) }
        vm.save(); advanceUntilIdle()
        val edited = vm.state.value.spots.single()
        assertEquals(original.id, edited.id)
        assertEquals(original.createdAt, edited.createdAt)
        assertEquals(5, edited.biteRating)
        vm.filter(true); assertTrue(vm.state.value.visibleSpots.isEmpty())
        vm.favorite(edited, true); advanceUntilIdle()
        assertEquals(1, vm.state.value.visibleSpots.size)
        vm.select(edited.id)
        vm.delete(edited); advanceUntilIdle()
        assertTrue(vm.state.value.spots.isEmpty())
        assertNull(vm.state.value.selected)
    }

    @Test fun repositoryRejectsInvalidInputEvenWithoutUI() = runTest(dispatcher) {
        val result = runCatching { SpotRepository(dao).save(FishingSpot(name = " ", latitude = 55.0, longitude = 83.0)) }
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertEquals(0, dao.insertCount)
    }
}

/** Fake storage lets these tests run on the JVM; real SQLite is tested on Android. */
private class MemoryDao : SpotDao {
    val rows = MutableStateFlow<List<FishingSpot>>(emptyList())
    var insertCount = 0
    var failWrite = false
    override fun observeAll() = rows
    override suspend fun insert(spot: FishingSpot): Long {
        delay(50)
        check(!failWrite)
        insertCount++
        val id = insertCount.toLong()
        rows.value = rows.value + spot.copy(id = id)
        return id
    }
    override suspend fun update(spot: FishingSpot): Int {
        val exists = rows.value.any { it.id == spot.id }
        rows.value = rows.value.map { if (it.id == spot.id) spot else it }
        return if (exists) 1 else 0
    }
    override suspend fun delete(id: Long): Int {
        val size = rows.value.size
        rows.value = rows.value.filterNot { it.id == id }
        return size - rows.value.size
    }
    override suspend fun setFavorite(id: Long, favorite: Boolean): Int {
        val spot = rows.value.firstOrNull { it.id == id } ?: return 0
        return update(spot.copy(isFavorite = favorite))
    }
}
