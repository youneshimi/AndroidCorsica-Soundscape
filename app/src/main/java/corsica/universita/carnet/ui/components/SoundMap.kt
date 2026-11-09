package corsica.universita.carnet.ui.components

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import corsica.universita.carnet.data.local.SoundEntity
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.BoundingBox
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import android.graphics.Color
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import android.graphics.drawable.BitmapDrawable
import android.animation.ValueAnimator
import corsica.universita.carnet.ui.theme.categoryColor
import corsica.universita.carnet.ui.theme.categoryEmoji
import kotlin.math.roundToInt
import androidx.compose.ui.unit.dp

private data class MapViewport(val latitude: Double, val longitude: Double, val zoom: Double)

private val viewportSaver = listSaver<MapViewport, Double>(
    save = { listOf(it.latitude, it.longitude, it.zoom) },
    restore = { MapViewport(it[0], it[1], it[2]) },
)

@Composable
fun SoundMap(
    sounds: List<SoundEntity>,
    modifier: Modifier = Modifier,
    onSoundClick: (Long) -> Unit = {},
    route: List<SoundEntity> = emptyList(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnSoundClick by rememberUpdatedState(onSoundClick)
    var viewport by rememberSaveable(stateSaver = viewportSaver) {
        mutableStateOf(MapViewport(42.15, 9.1, 8.5))
    }
    val mapView = remember(context) {
        Configuration.getInstance().apply {
            load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
            userAgentValue = context.packageName
        }
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            setBuiltInZoomControls(false)
            // Une limite serrée empêche presque tout déplacement sur un écran portrait.
            // La carte reste centrée sur la Corse au démarrage, puis libre à explorer.
            setMinZoomLevel(7.0)
            setMaxZoomLevel(18.0)
            isHorizontalMapRepetitionEnabled = false
            isVerticalMapRepetitionEnabled = false
            controller.setZoom(viewport.zoom.coerceIn(7.0, 18.0))
            controller.setCenter(GeoPoint(viewport.latitude, viewport.longitude))
            addMapListener(object : MapListener {
                private fun saveViewport() {
                    val center = mapCenter
                    viewport = MapViewport(center.latitude, center.longitude, zoomLevelDouble)
                }

                override fun onScroll(event: ScrollEvent): Boolean {
                    saveViewport()
                    return false
                }

                override fun onZoom(event: ZoomEvent): Boolean {
                    saveViewport()
                    return false
                }
            })
        }
    }

    DisposableEffect(mapView, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) mapView.onResume()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onPause()
            mapView.onDetach()
        }
    }

    LaunchedEffect(mapView, route) {
        if (route.size > 1) {
            val points = route.map { GeoPoint(it.latitude, it.longitude) }
            mapView.post { mapView.zoomToBoundingBox(BoundingBox.fromGeoPoints(points), true, 48) }
        }
    }

    // Les marqueurs dépendent du niveau de zoom, pas de chaque pixel de déplacement.
    // Les recréer à chaque onScroll rendait le glissement et le pincement saccadés.
    LaunchedEffect(mapView, sounds, route, viewport.zoom.toInt()) {
        mapView.post {
            mapView.overlays.removeAll { it is Marker || it is Polyline }
            if (route.size > 1) {
                val points = route.map { GeoPoint(it.latitude, it.longitude) }
                mapView.overlays.add(Polyline(mapView).apply {
                    setPoints(points)
                    outlinePaint.color = Color.rgb(212, 118, 67)
                    outlinePaint.strokeWidth = 8f
                })
            }
            val cell = (55 * context.resources.displayMetrics.density).roundToInt().coerceAtLeast(1)
            val buckets = sounds.filter { it.latitude.isFinite() && it.longitude.isFinite() }
                .groupBy { sound ->
                    val pixel = mapView.projection.toPixels(GeoPoint(sound.latitude, sound.longitude), Point())
                    Pair(Math.floorDiv(pixel.x, cell), Math.floorDiv(pixel.y, cell))
                }
            buckets.values.forEach { group ->
                val cluster = group.size > 1
                val center = GeoPoint(group.map { it.latitude }.average(), group.map { it.longitude }.average())
                mapView.overlays.add(Marker(mapView).apply {
                    position = center
                    title = if (cluster) "${group.size} sons" else group.first().title
                    icon = markerIcon(context, if (cluster) group.size.toString() else categoryEmoji(group.first().category),
                        if (cluster) android.graphics.Color.rgb(27, 107, 147) else categoryColor(group.first().category).toArgb())
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    setOnMarkerClickListener { clicked, _ ->
                        ValueAnimator.ofFloat(1f, 1.18f, 1f).apply {
                            duration = 220
                            addUpdateListener { animation ->
                                clicked.icon = markerIcon(context,
                                    if (cluster) group.size.toString() else categoryEmoji(group.first().category),
                                    if (cluster) Color.rgb(27, 107, 147) else categoryColor(group.first().category).toArgb(),
                                    animation.animatedValue as Float)
                                mapView.invalidate()
                            }
                            start()
                        }
                        if (cluster) {
                            mapView.controller.animateTo(center)
                            mapView.controller.setZoom((mapView.zoomLevelDouble + 2).coerceAtMost(18.0))
                        } else currentOnSoundClick(group.first().id)
                        true
                    }
                })
            }
            mapView.invalidate()
        }
    }

    Box(modifier) {
        AndroidView(factory = { mapView }, modifier = Modifier.matchParentSize())
        Surface(
            modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 3.dp,
            shadowElevation = 3.dp,
        ) {
            Column {
                IconButton(onClick = { mapView.controller.setZoom((mapView.zoomLevelDouble + 1).coerceAtMost(18.0)) }) {
                    Text("+", style = androidx.compose.material3.MaterialTheme.typography.headlineSmall)
                }
                IconButton(onClick = { mapView.controller.setZoom((mapView.zoomLevelDouble - 1).coerceAtLeast(7.0)) }) {
                    Text("−", style = androidx.compose.material3.MaterialTheme.typography.headlineSmall)
                }
            }
        }
    }
}

private fun markerIcon(context: Context, label: String, fill: Int, scale: Float = 1f): BitmapDrawable {
    val size = (46 * context.resources.displayMetrics.density * scale).roundToInt()
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
    paint.color = fill
    canvas.drawCircle(size / 2f, size / 2f, size * 0.43f, paint)
    paint.color = Color.WHITE
    paint.textAlign = Paint.Align.CENTER
    paint.textSize = size * if (label.length > 2) 0.36f else 0.48f
    canvas.drawText(label, size / 2f, size / 2f - (paint.ascent() + paint.descent()) / 2f, paint)
    return BitmapDrawable(context.resources, bitmap)
}
