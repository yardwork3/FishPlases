package ru.fishplaces.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.fishplaces.R
import ru.fishplaces.data.FishingSpot
import ru.fishplaces.data.SpotRepository
import ru.fishplaces.data.SpotValidation

data class SpotsState(
    val spots: List<FishingSpot> = emptyList(),
    val loading: Boolean = true,
    val loadFailed: Boolean = false,
    val favoritesOnly: Boolean = false,
    val selectedId: Long? = null,
    val draft: FishingSpot? = null,
    val nameError: Boolean = false,
    val busy: Boolean = false,
    val message: Int? = null,
) {
    val visibleSpots get() = spots.filter { !favoritesOnly || it.isFavorite }
    val selected get() = spots.firstOrNull { it.id == selectedId }
}

class SpotsViewModel(private val repository: SpotRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(SpotsState())
    val state = mutableState.asStateFlow()
    private var loadJob: Job? = null

    init { reload() }

    fun reload() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            mutableState.update { it.copy(loading = true, loadFailed = false) }
            try {
                repository.spots.collect { spots ->
                    mutableState.update { it.copy(spots = spots, loading = false) }
                }
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) {
                mutableState.update { it.copy(loading = false, loadFailed = true, message = R.string.load_error) }
            }
        }
    }

    fun filter(favorites: Boolean) { mutableState.update { it.copy(favoritesOnly = favorites) } }
    fun select(id: Long?) { if (!state.value.busy) mutableState.update { it.copy(selectedId = id) } }
    fun add(latitude: Double, longitude: Double) {
        if (state.value.busy || state.value.draft != null) return
        mutableState.update { it.copy(draft = FishingSpot(name = "", latitude = latitude, longitude = longitude), nameError = false, selectedId = null) }
    }
    fun edit(spot: FishingSpot) {
        if (!state.value.busy) mutableState.update { it.copy(draft = spot, nameError = false, selectedId = null) }
    }
    fun changeDraft(transform: (FishingSpot) -> FishingSpot) {
        if (state.value.busy) return
        mutableState.update { state -> state.copy(draft = state.draft?.let(transform), nameError = false) }
    }
    fun cancelEdit() {
        if (!state.value.busy) mutableState.update { it.copy(draft = null, nameError = false) }
    }
    fun clearMessage() { mutableState.update { it.copy(message = null) } }

    fun save() {
        val spot = state.value.draft ?: return
        if (state.value.busy) return
        if (!SpotValidation.validName(spot.name)) {
            mutableState.update { it.copy(nameError = true) }; return
        }
        if (!SpotValidation.validRating(spot.biteRating) || !SpotValidation.validCoordinates(spot.latitude, spot.longitude)) {
            mutableState.update { it.copy(message = R.string.invalid_data) }; return
        }
        // Set busy BEFORE launch: a second fast click cannot start another insert.
        mutableState.update { it.copy(busy = true) }
        viewModelScope.launch {
            try {
                repository.save(spot)
                mutableState.update { it.copy(draft = null, message = R.string.saved) }
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) {
                mutableState.update { it.copy(message = R.string.save_error) }
            } finally { mutableState.update { it.copy(busy = false) } }
        }
    }

    fun favorite(spot: FishingSpot, value: Boolean) = mutate {
        repository.setFavorite(spot.id, value)
    }
    fun delete(spot: FishingSpot) = mutate {
        repository.delete(spot.id)
        mutableState.update { it.copy(selectedId = null, message = R.string.deleted) }
    }

    private fun mutate(action: suspend () -> Unit) {
        if (state.value.busy) return
        mutableState.update { it.copy(busy = true) }
        viewModelScope.launch {
            try { action()
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) {
                mutableState.update { it.copy(message = R.string.change_error) }
            } finally { mutableState.update { it.copy(busy = false) } }
        }
    }
}
