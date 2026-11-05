package com.openauto.dash.companion.mine

import android.os.Bundle
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.openauto.dash.link.DriveSummary
import com.openauto.dash.link.mine.EncodedPolyline
import com.openauto.dash.companion.CompanionColors
import com.openauto.dash.companion.Panel
import com.openauto.dash.companion.R
import com.openauto.dash.companion.dateText
import com.openauto.dash.companion.decimal
import com.openauto.dash.companion.durationText
import com.openauto.dash.companion.ecoCall
import com.openauto.dash.companion.ecoTint
import com.openauto.dash.companion.kmText
import org.json.JSONArray
import org.json.JSONObject
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory.lineCap
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.lineJoin
import org.maplibre.android.style.layers.PropertyFactory.lineWidth
import org.maplibre.android.style.sources.GeoJsonSource

private const val DRIVE_MAP_STYLE = "https://tiles.openfreemap.org/styles/dark"

@Composable
fun DriveDetailScreen(drive: DriveSummary, onClose: () -> Unit, onDelete: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val points = remember(drive.polyline) {
        drive.polyline?.let(EncodedPolyline::decode).orEmpty()
    }
    val mapView = remember(context) {
        MapLibre.getInstance(context)
        val options = MapLibreMapOptions.createFromAttributes(context, null).textureMode(true)
        MapView(context, options)
    }
    var showDeleteDialog by remember { mutableStateOf(false) }

    DisposableEffect(mapView, lifecycleOwner) {
        mapView.onCreate(Bundle())
        var started = false
        var resumed = false
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> if (!started) {
                    started = true
                    mapView.onStart()
                }
                Lifecycle.Event.ON_RESUME -> if (!resumed) {
                    resumed = true
                    mapView.onResume()
                }
                Lifecycle.Event.ON_PAUSE -> if (resumed) {
                    resumed = false
                    mapView.onPause()
                }
                Lifecycle.Event.ON_STOP -> if (started) {
                    started = false
                    mapView.onStop()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            if (resumed) mapView.onPause()
            if (started) mapView.onStop()
            mapView.onDestroy()
        }
    }

    LaunchedEffect(mapView, points) {
        if (points.size < 2) return@LaunchedEffect
        mapView.getMapAsync { map ->
            map.uiSettings.isLogoEnabled = true
            map.setStyle(Style.Builder().fromUri(DRIVE_MAP_STYLE)) { style ->
                drawDriveRoute(map, style, points)
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        Surface(color = CompanionColors.Surface, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = CompanionColors.Text
                    )
                }
                Text(
                    stringResource(R.string.drive_detail_title),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.drive_delete),
                        tint = CompanionColors.Red
                    )
                }
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                if (points.size >= 2) {
                    AndroidView(
                        factory = { mapView },
                        modifier = Modifier.fillMaxWidth().height(280.dp)
                    )
                } else {
                    Surface(
                        modifier = Modifier.fillMaxWidth().height(180.dp),
                        color = CompanionColors.Surface,
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                stringResource(R.string.drive_route_unavailable),
                                modifier = Modifier.padding(24.dp),
                                color = CompanionColors.Muted,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
            item {
                Panel {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            stringResource(
                                R.string.drive_period,
                                dateText(drive.startedAt),
                                dateText(drive.endedAt)
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            DetailStat(kmText(drive.distanceKm), stringResource(R.string.drive_km))
                            DetailStat(durationText(drive.elapsedMs), stringResource(R.string.drive_time))
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            DetailStat(drive.avgSpeedKmh.toString(), stringResource(R.string.drive_average))
                            DetailStat(drive.maxSpeedKmh.toString(), stringResource(R.string.drive_top))
                        }
                        val stoppedMs = (drive.elapsedMs - drive.movingMs).coerceAtLeast(0L)
                        DetailStat(durationText(stoppedMs), stringResource(R.string.drive_stopped_label))
                        drive.ecoScore?.let { score ->
                            Text(
                                stringResource(ecoCall(score)),
                                color = ecoTint(score),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        val liters = drive.fuelLiters
                        val cost = drive.fuelCost
                        if (liters != null && cost != null) {
                            Text(
                                stringResource(R.string.drive_fuel, decimal(liters, 1), decimal(cost, 2), drive.currency.orEmpty()),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        TextButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.textButtonColors(contentColor = CompanionColors.Red)
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = null)
                            Text(stringResource(R.string.drive_delete))
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.drive_delete_confirm_title)) },
            text = { Text(stringResource(R.string.drive_delete_confirm_body)) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CompanionColors.Red)
                ) {
                    Text(stringResource(R.string.drive_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun DetailStat(value: String, label: String) {
    Column {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = CompanionColors.Muted)
    }
}

private fun drawDriveRoute(map: MapLibreMap, style: Style, points: List<Pair<Double, Double>>) {
    try {
        val coordinates = JSONArray()
        points.forEach { (latitude, longitude) ->
            coordinates.put(JSONArray().put(longitude).put(latitude))
        }
        val lineString = JSONObject()
            .put("type", "Feature")
            .put("geometry", JSONObject().put("type", "LineString").put("coordinates", coordinates))
            .put("properties", JSONObject())
        val collection = JSONObject()
            .put("type", "FeatureCollection")
            .put("features", JSONArray().put(lineString))
        val sourceId = "drive-route"
        val layerId = "drive-route-line"
        val source = style.getSourceAs<GeoJsonSource>(sourceId)
            ?: GeoJsonSource(sourceId).also { style.addSource(it) }
        source.setGeoJson(collection.toString())
        if (style.getLayer(layerId) == null) {
            style.addLayer(
                LineLayer(layerId, sourceId).withProperties(
                    lineColor("#6C9BFF"),
                    lineWidth(4f),
                    lineCap("round"),
                    lineJoin("round")
                )
            )
        }

        val bounds = LatLngBounds.Builder().apply {
            points.forEach { (latitude, longitude) -> include(LatLng(latitude, longitude)) }
        }.build()
        map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 36), 500)
    } catch (e: Exception) {
        Log.e("DriveDetailScreen", "Failed to draw drive route", e)
    }
}
