package ru.fishplaces.ui

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import ru.fishplaces.R
import ru.fishplaces.data.FishingSpot
import ru.fishplaces.location.currentLocation
import ru.fishplaces.location.hasLocationPermission
import ru.fishplaces.map.CameraTarget
import ru.fishplaces.map.FishingMap
import ru.fishplaces.map.MapPosition
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val colors = lightColorScheme(
    primary = Color(0xFF18675B), onPrimary = Color.White,
    primaryContainer = Color(0xFFD2EDE4), secondary = Color(0xFF56665F),
    background = Color(0xFFF7F9F5), surface = Color(0xFFF7F9F5),
    surfaceVariant = Color(0xFFE1E9E2),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FishPlacesApp(viewModel: SpotsViewModel, email: String = "", signOut: () -> Unit = {}) {
    var accountOpen by remember { mutableStateOf(false) }
    if (accountOpen) AlertDialog(onDismissRequest = { accountOpen = false },
        title = { Text(stringResource(R.string.auth_account)) }, text = { Text(email) },
        confirmButton = { TextButton(onClick = signOut) { Text(stringResource(R.string.auth_logout)) } },
        dismissButton = { TextButton(onClick = { accountOpen = false }) { Text(stringResource(R.string.close)) } })
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var listTab by rememberSaveable { mutableStateOf(false) }
    var target by remember { mutableStateOf<CameraTarget?>(null) }
    var cameraLatitude by rememberSaveable { mutableDoubleStateOf(55.0084) }
    var cameraLongitude by rememberSaveable { mutableDoubleStateOf(82.9357) }
    var cameraZoom by rememberSaveable { mutableDoubleStateOf(10.0) }
    var appliedTargetId by rememberSaveable { mutableStateOf<Long?>(null) }
    var locating by remember { mutableStateOf(false) }
    var deleteId by rememberSaveable { mutableStateOf<Long?>(null) }

    fun message(res: Int) { scope.launch { snackbar.showSnackbar(context.getString(res)) } }
    fun locate() {
        if (locating) return
        locating = true
        scope.launch {
            try {
                val location = currentLocation(context)
                if (location == null) message(R.string.location_unavailable)
                else {
                    target = CameraTarget(location.latitude, location.longitude, System.nanoTime())
                    message(R.string.location_found)
                }
            } finally { locating = false }
        }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (hasLocationPermission(context)) locate() else message(R.string.location_denied)
    }
    LaunchedEffect(state.message) {
        state.message?.let {
            val text = context.getString(it)
            viewModel.clearMessage()
            snackbar.showSnackbar(text)
        }
    }

    MaterialTheme(colorScheme = colors) {
        val draft = state.draft
        if (draft != null) {
            SpotForm(draft, state.busy, state.nameError, viewModel::changeDraft, viewModel::save, viewModel::cancelEdit, snackbar)
        } else {
            Scaffold(
                topBar = {
                    TopAppBar(title = { Column {
                        Text(stringResource(R.string.app_name))
                        Text(stringResource(R.string.subtitle), style = MaterialTheme.typography.bodySmall)
                    } }, actions = { TextButton(onClick = { accountOpen = true }) { Text(stringResource(R.string.auth_account)) } })
                },
                bottomBar = {
                    NavigationBar {
                        NavigationBarItem(selected = !listTab, onClick = { listTab = false },
                            icon = { Text(stringResource(R.string.map_symbol)) }, label = { Text(stringResource(R.string.map_tab)) })
                        NavigationBarItem(selected = listTab, onClick = { listTab = true },
                            icon = { Text(stringResource(R.string.list_symbol)) }, label = { Text(stringResource(R.string.list_tab)) })
                    }
                },
                snackbarHost = { SnackbarHost(snackbar) },
            ) { padding ->
                Column(Modifier.fillMaxSize().padding(padding)) {
                    Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = !state.favoritesOnly, onClick = { viewModel.filter(false) }, label = { Text(stringResource(R.string.all_filter)) })
                        FilterChip(selected = state.favoritesOnly, onClick = { viewModel.filter(true) }, label = { Text(stringResource(R.string.favorites_filter)) })
                    }
                    if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                    if (state.loadFailed) {
                        Text(stringResource(R.string.load_error), Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = viewModel::reload) { Text(stringResource(R.string.retry)) }
                    }
                    if (listTab) {
                        SpotList(state.visibleSpots, state.favoritesOnly, state.loading, viewModel::select, { listTab = false })
                    } else {
                        Text(stringResource(R.string.add_hint), Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.bodyMedium)
                        if (state.favoritesOnly && state.visibleSpots.isEmpty() && !state.loading) {
                            Text(stringResource(R.string.empty_favorites_title), Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.bodySmall)
                        }
                        Box(Modifier.weight(1f).fillMaxWidth()) {
                            FishingMap(
                                spots = state.visibleSpots, target = target,
                                position = MapPosition(cameraLatitude, cameraLongitude, cameraZoom),
                                onMoved = { cameraLatitude = it.latitude; cameraLongitude = it.longitude; cameraZoom = it.zoom },
                                appliedTargetId = appliedTargetId, onTargetApplied = { appliedTargetId = it },
                                onLongPress = viewModel::add, onSelect = viewModel::select,
                                onError = { message(R.string.map_error) }, modifier = Modifier.fillMaxSize()
                            )
                            FilledTonalButton(
                                onClick = {
                                    if (hasLocationPermission(context)) locate()
                                    else permission.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
                                }, enabled = !locating, modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)
                            ) { Text(stringResource(if (locating) R.string.locating else R.string.my_location)) }
                            val uri = LocalUriHandler.current
                            val url = stringResource(R.string.attribution_url)
                            Surface(Modifier.align(Alignment.BottomStart), tonalElevation = 2.dp) {
                                TextButton(onClick = { uri.openUri(url) }) { Text(stringResource(R.string.attribution), style = MaterialTheme.typography.labelSmall) }
                            }
                        }
                        Text(stringResource(R.string.map_network_hint), Modifier.padding(horizontal = 16.dp, vertical = 6.dp), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        state.selected?.takeIf { draft == null }?.let { spot ->
            SpotDetails(spot, state.busy, { viewModel.select(null) },
                { viewModel.favorite(spot, it) }, { viewModel.edit(spot) }, { deleteId = spot.id }, {
                    target = CameraTarget(spot.latitude, spot.longitude, System.nanoTime())
                    listTab = false; viewModel.select(null)
                })
        }
        val deleting = state.spots.firstOrNull { it.id == deleteId }
        if (deleting != null) AlertDialog(
            onDismissRequest = { if (!state.busy) deleteId = null },
            title = { Text(stringResource(R.string.delete_title)) },
            text = { Text(stringResource(R.string.delete_message, deleting.name)) },
            confirmButton = { TextButton(enabled = !state.busy, onClick = { viewModel.delete(deleting); deleteId = null }) { Text(stringResource(R.string.delete)) } },
            dismissButton = { TextButton(enabled = !state.busy, onClick = { deleteId = null }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}

@Composable
private fun SpotList(spots: List<FishingSpot>, favorites: Boolean, loading: Boolean, select: (Long) -> Unit, openMap: () -> Unit) {
    if (loading && spots.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(stringResource(R.string.loading)) }
    } else if (spots.isEmpty()) {
        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(if (favorites) R.string.empty_favorites_title else R.string.empty_title), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))
            Text(stringResource(if (favorites) R.string.empty_favorites_text else R.string.empty_text))
            Spacer(Modifier.height(16.dp))
            Button(onClick = openMap) { Text(stringResource(R.string.open_map)) }
        }
    } else LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text(stringResource(R.string.places_count, spots.size), style = MaterialTheme.typography.labelLarge) }
        items(spots, key = { it.id }) { spot ->
            OutlinedCard(onClick = { select(spot.id) }, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(spot.name, style = MaterialTheme.typography.titleMedium)
                    if (spot.isFavorite) Text(stringResource(R.string.favorite_badge), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                    if (spot.fishTypes.isNotBlank()) Text(spot.fishTypes, maxLines = 2)
                    Text(spot.biteRating?.let { stringResource(R.string.rating_value, it) } ?: stringResource(R.string.no_rating), style = MaterialTheme.typography.bodySmall)
                    Text(stringResource(R.string.coordinates, spot.latitude, spot.longitude), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun SpotForm(spot: FishingSpot, busy: Boolean, nameError: Boolean, change: ((FishingSpot) -> FishingSpot) -> Unit, save: () -> Unit, cancel: () -> Unit, snackbar: SnackbarHostState) {
    BackHandler { if (!busy) cancel() }
    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = { TopAppBar(title = { Text(stringResource(if (spot.id == 0L) R.string.add_title else R.string.edit_title)) }) },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = cancel, enabled = !busy, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.cancel)) }
                    Button(onClick = save, enabled = !busy, modifier = Modifier.weight(1f)) { Text(stringResource(if (busy) R.string.saving else R.string.save)) }
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.coordinates, spot.latitude, spot.longitude), style = MaterialTheme.typography.bodyMedium)
            OutlinedTextField(value = spot.name, onValueChange = { value -> change { it.copy(name = value) } },
                label = { Text(stringResource(R.string.name_label)) }, isError = nameError,
                supportingText = { if (nameError) Text(stringResource(R.string.name_error)) }, singleLine = true,
                enabled = !busy, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences))
            OutlinedTextField(value = spot.fishTypes, onValueChange = { value -> change { it.copy(fishTypes = value) } },
                label = { Text(stringResource(R.string.fish_label)) }, placeholder = { Text(stringResource(R.string.fish_hint)) },
                enabled = !busy, modifier = Modifier.fillMaxWidth(), minLines = 1, maxLines = 3)
            Column {
                Text(stringResource(R.string.rating_label), style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = spot.biteRating == null, onClick = { change { it.copy(biteRating = null) } }, enabled = !busy,
                        label = { Text(stringResource(R.string.no_rating)) })
                    (1..5).forEach { rating ->
                        FilterChip(selected = spot.biteRating == rating, onClick = { change { it.copy(biteRating = rating) } }, enabled = !busy,
                            label = { Text(stringResource(R.string.rating_number, rating)) })
                    }
                }
            }
            OutlinedTextField(value = spot.note, onValueChange = { value -> change { it.copy(note = value) } },
                label = { Text(stringResource(R.string.note_label)) }, minLines = 3, enabled = !busy, modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences))
            FavoriteRow(spot.isFavorite, !busy) { value -> change { it.copy(isFavorite = value) } }
            Text(stringResource(R.string.required_hint), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun FavoriteRow(value: Boolean, enabled: Boolean, change: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.favorite_label), Modifier.weight(1f))
        Switch(checked = value, onCheckedChange = change, enabled = enabled)
    }
}

@Composable
private fun SpotDetails(spot: FishingSpot, busy: Boolean, close: () -> Unit, favorite: (Boolean) -> Unit, edit: () -> Unit, delete: () -> Unit, onMap: () -> Unit) {
    AlertDialog(onDismissRequest = { if (!busy) close() },
        title = { Text(spot.name) },
        text = {
            Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.category_fishing), style = MaterialTheme.typography.labelMedium)
                Text(stringResource(R.string.coordinates, spot.latitude, spot.longitude))
                DetailField(R.string.fish_detail, spot.fishTypes)
                DetailField(R.string.rating_label, spot.biteRating?.let { stringResource(R.string.rating_value, it) } ?: stringResource(R.string.no_rating))
                DetailField(R.string.note_detail, spot.note)
                val date = remember(spot.createdAt) { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.forLanguageTag("ru-RU")).format(Date(spot.createdAt)) }
                Text(stringResource(R.string.created_at, date), style = MaterialTheme.typography.bodySmall)
                FavoriteRow(spot.isFavorite, !busy, favorite)
                OutlinedButton(onClick = onMap, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.on_map)) }
                OutlinedButton(onClick = edit, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.edit)) }
                TextButton(onClick = delete, enabled = !busy, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text(stringResource(R.string.delete)) }
            }
        },
        confirmButton = { TextButton(onClick = close, enabled = !busy) { Text(stringResource(R.string.close)) } }
    )
}

@Composable
private fun DetailField(label: Int, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(label), style = MaterialTheme.typography.labelLarge)
        Text(value.ifBlank { stringResource(R.string.not_specified) })
    }
}
