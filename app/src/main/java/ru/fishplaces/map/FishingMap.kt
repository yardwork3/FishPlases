package ru.fishplaces.map

import android.content.ComponentCallbacks
import android.content.res.Configuration
import android.graphics.RectF
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression.get
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory.*
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point
import ru.fishplaces.data.FishingSpot

data class CameraTarget(val latitude: Double, val longitude: Double, val requestId: Long)
data class MapPosition(val latitude: Double, val longitude: Double, val zoom: Double)

// Local style works without a style server or token. Only visible raster tiles use the network.
private const val MAP_STYLE = """
{"version":8,"sources":{"osm":{"type":"raster","tiles":["https://tile.openstreetmap.org/{z}/{x}/{y}.png"],"tileSize":256,"minzoom":0,"maxzoom":19,"attribution":"© OpenStreetMap contributors"}},"layers":[{"id":"osm","type":"raster","source":"osm"}]}
"""

@Composable
fun FishingMap(
    spots: List<FishingSpot>,
    target: CameraTarget?,
    position: MapPosition,
    onMoved: (MapPosition) -> Unit,
    appliedTargetId: Long?,
    onTargetApplied: (Long) -> Unit,
    onLongPress: (Double, Double) -> Unit,
    onSelect: (Long) -> Unit,
    onError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val latestSpots by rememberUpdatedState(spots)
    val longPress by rememberUpdatedState(onLongPress)
    val select by rememberUpdatedState(onSelect)
    val error by rememberUpdatedState(onError)
    val moved by rememberUpdatedState(onMoved)
    val mapView = remember { MapView(context).apply { onCreate(null) } }
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var source by remember { mutableStateOf<GeoJsonSource?>(null) }

    DisposableEffect(mapView, owner) {
        // Compose can insert a view after the activity has already resumed.
        var started = false
        var resumed = false
        fun start() { if (!started) { mapView.onStart(); started = true } }
        fun resume() { start(); if (!resumed) { mapView.onResume(); resumed = true } }
        fun pause() { if (resumed) { mapView.onPause(); resumed = false } }
        fun stop() { pause(); if (started) { mapView.onStop(); started = false } }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> start()
                Lifecycle.Event.ON_RESUME -> resume()
                Lifecycle.Event.ON_PAUSE -> pause()
                Lifecycle.Event.ON_STOP -> stop()
                else -> Unit
            }
        }
        owner.lifecycle.addObserver(observer)
        val memoryCallbacks = object : ComponentCallbacks {
            override fun onConfigurationChanged(newConfig: Configuration) = Unit
            override fun onLowMemory() { mapView.onLowMemory() }
        }
        context.applicationContext.registerComponentCallbacks(memoryCallbacks)
        if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) start()
        if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) resume()
        mapView.addOnDidFailLoadingMapListener { error() }
        mapView.getMapAsync { ready ->
            map = ready
            ready.setPrefetchZoomDelta(0)
            ready.cameraPosition = CameraPosition.Builder()
                .target(LatLng(position.latitude, position.longitude)).zoom(position.zoom).build()
            ready.uiSettings.apply {
                // A Russian attribution link is always displayed by the Compose screen.
                isAttributionEnabled = false
                isLogoEnabled = false
            }
            ready.addOnCameraIdleListener {
                val camera = ready.cameraPosition
                camera.target?.let { moved(MapPosition(it.latitude, it.longitude, camera.zoom)) }
            }
            ready.addOnMapLongClickListener { point ->
                longPress(point.latitude, point.longitude); true
            }
            ready.addOnMapClickListener { point ->
                val screen = ready.projection.toScreenLocation(point)
                val radius = 24 * context.resources.displayMetrics.density
                val features = ready.queryRenderedFeatures(
                    RectF(screen.x - radius, screen.y - radius, screen.x + radius, screen.y + radius),
                    "spot-circles"
                )
                features.firstOrNull()?.getNumberProperty("spotId")?.toLong()?.let { select(it) }
                features.isNotEmpty()
            }
            ready.setStyle(Style.Builder().fromJson(MAP_STYLE)) { style ->
                val points = GeoJsonSource("spots", collection(latestSpots))
                style.addSource(points)
                style.addLayer(CircleLayer("spot-circles", "spots").withProperties(
                    circleRadius(10f), circleColor(get("color")),
                    circleStrokeWidth(3f), circleStrokeColor("#FFFFFF")
                ))
                source = points
            }
        }
        onDispose {
            map?.cameraPosition?.let { camera ->
                camera.target?.let { moved(MapPosition(it.latitude, it.longitude, camera.zoom)) }
            }
            owner.lifecycle.removeObserver(observer)
            context.applicationContext.unregisterComponentCallbacks(memoryCallbacks)
            stop()
            mapView.onDestroy()
        }
    }

    AndroidView(factory = { mapView }, modifier = modifier, update = {
        source?.setGeoJson(collection(spots))
        val ready = map
        if (target != null && ready != null && appliedTargetId != target.requestId) {
            ready.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(target.latitude, target.longitude), 14.0))
            onTargetApplied(target.requestId)
        }
    })
}

private fun collection(spots: List<FishingSpot>): FeatureCollection = FeatureCollection.fromFeatures(
    spots.map { spot ->
        Feature.fromGeometry(Point.fromLngLat(spot.longitude, spot.latitude)).apply {
            addNumberProperty("spotId", spot.id)
            addStringProperty("color", if (spot.isFavorite) "#A56714" else "#18675B")
        }
    }
)
