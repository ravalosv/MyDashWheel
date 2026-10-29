package com.openauto.dash

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.maplibre.geojson.Point

/** How long the drive to home and to work takes from here, read again every few minutes while the tile shows. */
private const val REFRESH_MS = 5 * 60_000L

/** Minutes of driving from [from] to [to] by the free routing server; null when it can't be had. */
private suspend fun minutesTo(context: Context, fromLat: Double, fromLng: Double, to: Place): Int? = withContext(Dispatchers.IO) {
    runCatching {
        valhallaRoute(Point.fromLngLat(fromLng, fromLat), Point.fromLngLat(to.lng, to.lat), InAppNav.locale(context).language)
            .routes.firstOrNull()?.duration?.let { (it / 60).toInt() }
    }.getOrNull()
}

/**
 * The places the driver ticked in Settings, one tap each: the time to drive
 * there from where the car is (no traffic: the free router has none), and a
 * tap starts the guidance. Home and work stand in until they tick some.
 */
@Composable
internal fun HomeWorkCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val places by PlacesStore.places.collectAsState()
    // Only whether there is a fix: the loop reads the position itself, so the tile doesn't follow each fix.
    val hasFix by LocationFeed.hasFix.collectAsState()
    val shown = places.widgetPlaces().ifEmpty { places.allPlaces().filter { it.isBuiltIn } }
    // The id and the spot: a place moved in Settings is timed again, not after the next refresh.
    val shownSpots = shown.map { Triple(it.id, it.lat, it.lng) }
    var minutes by remember { mutableStateOf<Map<String, Int?>>(emptyMap()) }
    LaunchedEffect(shownSpots, hasFix) {
        while (true) {
            val loc = LocationFeed.location.value
            if (loc != null) {
                minutes = shown.associate { place ->
                    place.id to minutesTo(context, loc.latitude, loc.longitude, place.asPlace())
                }
            }
            delay(REFRESH_MS)
        }
    }
    // Guidance starts after the held step ("Guidance to Home", Cancel, three
    // seconds), as on every tile that changes the route: a finger that meant
    // the other button, or nothing, takes it back.
    val pending = rememberPendingAction()
    fun go(name: String, place: SavedPlace) =
        pending.arm(context.getString(R.string.phone_guidance_to, name)) { NavHandoff.start(context, place.asPlace()) }
    Card(modifier = modifier) {
        if (shown.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(DashSpace.Lg), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.widgets_hw_empty), color = DashColors.TextSecondary, style = MaterialTheme.typography.bodyMedium)
            }
            return@Card
        }
        Box(Modifier.fillMaxSize()) {
            // One place takes the tile, two share a line, three and four take two
            // lines with the odd one out on top (T-01 in places-personalizados-plan.md).
            Column(
                modifier = Modifier.fillMaxSize().padding(DashSpace.Md),
                verticalArrangement = Arrangement.spacedBy(DashSpace.Md)
            ) {
                when (shown.size) {
                    1 -> PlaceButton(shown[0], places, minutes[shown[0].id], Modifier.weight(1f).fillMaxWidth(), ::go)
                    2 -> Row(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(DashSpace.Md)
                    ) { shown.forEach { PlaceButton(it, places, minutes[it.id], Modifier.weight(1f), ::go) } }
                    3 -> {
                        PlaceButton(shown[0], places, minutes[shown[0].id], Modifier.weight(1f).fillMaxWidth(), ::go)
                        Row(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(DashSpace.Md)
                        ) { shown.drop(1).forEach { PlaceButton(it, places, minutes[it.id], Modifier.weight(1f), ::go) } }
                    }
                    else -> {
                        Row(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(DashSpace.Md)
                        ) { shown.take(2).forEach { PlaceButton(it, places, minutes[it.id], Modifier.weight(1f), ::go) } }
                        Row(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(DashSpace.Md)
                        ) { shown.drop(2).forEach { PlaceButton(it, places, minutes[it.id], Modifier.weight(1f), ::go) } }
                    }
                }
            }
            PendingActionStrip(pending, Modifier.align(Alignment.BottomCenter).padding(DashSpace.Sm))
        }
    }
}

/** One place on the tile: its icon, what it is called, and how long the drive is. */
@Composable
private fun PlaceButton(
    place: SavedPlace,
    places: Places,
    minutes: Int?,
    modifier: Modifier,
    onTap: (String, SavedPlace) -> Unit
) {
    val name = savedPlaceTitle(place, places)
    val tap = rememberTapFeedback()
    Box(
        modifier = modifier.fillMaxHeight().clip(DashShape.Medium).background(DashColors.CardHi)
            .clickable(role = Role.Button, onClickLabel = name) { tap(); onTap(name, place) }.padding(DashSpace.Md),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(placeIcon(place.category), contentDescription = null, tint = DashColors.Accent, modifier = Modifier.size(30.dp))
            Text(name, color = DashColors.TextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                minutes?.let { formatMinutes(it) } ?: "--",
                color = DashColors.TextPrimary, fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineSmall
            )
        }
    }
}

@Composable
private fun formatMinutes(min: Int): String =
    if (min >= 60) stringResource(R.string.info_map_duration_hm, min / 60, min % 60) else stringResource(R.string.info_map_duration_min, min)

@Composable
internal fun homeWorkFace(): WidgetFace {
    val context = LocalContext.current
    val places by PlacesStore.places.collectAsState()
    val shown = places.widgetPlaces().ifEmpty { places.allPlaces().filter { it.isBuiltIn } }
    // The same held step as the standard card, in the designed tile's strip.
    val pending = LocalPendingAction.current
    fun go(name: String, place: SavedPlace) =
        pending.arm(context.getString(R.string.phone_guidance_to, name)) { NavHandoff.start(context, place.asPlace()) }
    val firstIcon: ImageVector = shown.firstOrNull()?.let { placeIcon(it.category) } ?: Icons.Filled.Home
    return WidgetFace(
        icon = firstIcon,
        title = BuiltinKind.HOME_WORK.label,
        value = shown.firstOrNull()?.let { savedPlaceTitle(it, places) } ?: stringResource(R.string.widgets_hw_home),
        textValue = true,
        caption = if (shown.isEmpty()) stringResource(R.string.widgets_hw_empty) else "",
        actions = shown.mapIndexed { index, place ->
            val name = savedPlaceTitle(place, places)
            FaceAction(placeIcon(place.category), name, onClick = { go(name, place) }, primary = index == 0)
        }
    )
}
