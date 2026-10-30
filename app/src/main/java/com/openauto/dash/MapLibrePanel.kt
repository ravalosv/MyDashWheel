package com.openauto.dash

import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.LocationComponent
import org.maplibre.android.location.LocationComponentOptions
import org.maplibre.android.location.OnCameraTrackingChangedListener
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.FillExtrusionLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.VectorSource
import org.maplibre.geojson.Point
import org.maplibre.navigation.android.navigation.ui.v5.route.NavigationMapRoute
import org.maplibre.navigation.core.models.DirectionsResponse
import org.maplibre.navigation.core.models.DirectionsRoute
import org.maplibre.navigation.core.models.RouteOptions
import java.util.Locale

// Free, no-key services by default: OpenFreeMap's dark / positron styles, Nominatim
// geocoding ([PlaceSearch]) and the public Valhalla router; the map style and the
// router can be pointed at the driver's own servers ([MapServers]).
private const val USER_AGENT = OVERPASS_AGENT

private val Accent = Color(0xFF8AB4F8)
private val OverlayBg = Color(0xE6141518)

/**
 * A free, open-source **GPS navigator** built on MapLibre GL:
 *  - map + your location (OpenFreeMap styles by default, no token/account/card),
 *  - type a destination (Nominatim geocoding) or tap the map,
 *  - home, work and the last destinations one tap away ([PlacesStore]),
 *  - route computed by a Valhalla server (the free public one by default), drawn on the map with ETA,
 *  - **Start** guides there on this map ([InAppNav]): next turn, voice, a
 *    new route when the car leaves it; or hands the destination to Google
 *    Maps / Waze ([NavHandoff]) when there is no route of our own.
 *
 * As a [wallpaper] (the Canvas theme's page) it is only the map and the
 * guided route: no search, no route panel, no gestures, a lower and steeper camera with the car in
 * the lower part of the screen, and at most [WALLPAPER_FPS] frames a second.
 */
@Composable
fun MapLibrePanel(modifier: Modifier = Modifier, wallpaper: Boolean = false) {
    // Typing a destination waits until the car has stopped (the drive lock).
    val moving = LocalDriveLock.current.moving
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var hasLocation by remember { mutableStateOf(hasLocationPerm(context)) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result -> hasLocation = result.values.any { it } }

    LaunchedEffect(Unit) {
        if (!hasLocation) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // The fixes the puck follows are Dashwheel's own feed (enableLocation): held while the map shows.
    UseLocationFeed()
    val mapView = remember {
        MapLibre.getInstance(context)
        val options = MapLibreMapOptions.createFromAttributes(context, null).textureMode(true)
        MapView(context, options).also { if (wallpaper) it.setMaximumFps(WALLPAPER_FPS) }
    }

    var mapRef by remember { mutableStateOf<MapLibreMap?>(null) }
    // On a page the pagers keep composed beside the one on screen nobody sees
    // the map, yet a camera following the car redrew it every frame: it runs
    // at [HIDDEN_FPS] there, and back at its own pace (uncapped, or the
    // wallpaper's cap) as soon as the page starts sliding in ([LocalPageActive]).
    // Paused or stopped it is not: a tile map put to sleep came back black.
    val pageActive = LocalPageActive.current
    // A wallpaper under a car standing still barely changes, yet each fix
    // animated it at full pace, and every map frame redrew the whole
    // dashboard over it (a third of the K706's CPU parked): [HIDDEN_FPS] then.
    val still = wallpaper && !rememberMoving(enabled = wallpaper, demo = false).value
    val stillNow by rememberUpdatedState(still)
    LaunchedEffect(pageActive, still) {
        mapView.setMaximumFps(if (!pageActive || still) HIDDEN_FPS else if (wallpaper) WALLPAPER_FPS else Int.MAX_VALUE)
        if (pageActive) mapRef?.triggerRepaint()
    }

    var navRoute by remember { mutableStateOf<NavigationMapRoute?>(null) }
    var route by remember { mutableStateOf<DirectionsRoute?>(null) }
    var destination by remember { mutableStateOf<Point?>(null) }
    // What the destination is called: the address typed, a saved place's name; null for a spot picked on the map.
    var destinationName by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    var info by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<Place>>(emptyList()) }

    fun clearRoute() {
        route = null; destination = null; destinationName = null; info = null; error = null
        navRoute?.removeRoute()
        mapRef?.markers?.forEach { mapRef?.removeMarker(it) }
    }

    fun routeTo(dest: Point) {
        // Destination is set regardless of GPS so "Start" (Google Maps handoff)
        // always works; the in-app route preview below just needs our own fix.
        destination = dest
        info = null
        val map = mapRef
        // Prefer the shared GPS feed: the map puck may not have the first fix yet
        // (LocationComponent is filled asynchronously), which used to look like a
        // routing failure or a route from a stale/default position.
        @SuppressLint("MissingPermission")
        val loc = LocationFeed.location.value
            ?: runCatching { map?.locationComponent?.lastKnownLocation }.getOrNull()
        if (map == null || loc == null) {
            loading = false
            error = context.getString(R.string.info_map_gps_not_ready)
            return
        }
        val origin = Point.fromLngLat(loc.longitude, loc.latitude)
        
        // Validate destination is reasonable (not in ocean or extremely far)
        val distanceToDest = calculateDistanceKm(loc.latitude, loc.longitude, dest.latitude(), dest.longitude())
        if (distanceToDest > 5000) { // More than 5000 km is suspicious
            error = context.getString(R.string.info_map_address_not_found)
            android.util.Log.w("MapLibrePanel", "Destination seems invalid: ${"%.0f".format(distanceToDest)} km away at (${dest.latitude()}, ${dest.longitude()})")
            return
        }
        
        error = null; loading = true
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { valhallaRoute(origin, dest, InAppNav.locale(context).language) }
            }
            loading = false
            result.onSuccess { resp ->
                val first = resp.routes.firstOrNull()
                if (first == null) { error = context.getString(R.string.info_map_no_route); return@onSuccess }
                route = first
                navRoute?.addRoutes(resp.routes)
                info = formatEta(context, first.distance, first.duration)
                map.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(LatLng(dest.latitude(), dest.longitude()), 13.0)
                )
            }.onFailure {
                error = it.message?.takeIf { m -> m.isNotBlank() }
                    ?.let { m -> context.getString(R.string.info_map_routing_failed_detail, m) }
                    ?: context.getString(R.string.info_map_routing_failed)
            }
        }
    }

    fun searchPlaces() {
        val q = query.trim()
        if (q.isEmpty() || loading) return
        error = null; info = null; loading = true; searchResults = emptyList()
        scope.launch {
            val here = LocationFeed.location.value
            val found = withContext(Dispatchers.IO) {
                // MINE: #14 our own question: the country of the car, nearest branch first, the typed house number respected.
                runCatching { com.openauto.dash.mine.PlaceQuery.findAll(context, q, here?.latitude, here?.longitude) }
            }
            val places = found.getOrDefault(emptyList())
            loading = false
            if (places.isEmpty()) {
                error = context.getString(if (found.isFailure) R.string.places_offline else R.string.info_map_address_not_found)
                return@launch
            }
            // MINE: #14 always the list, even for a single answer: the driver confirms, nothing is routed
            // behind their back (a wrong address costs a turn, not a tap).
            searchResults = places
        }
    }

    fun pickSearchResult(place: Place) {
        searchResults = emptyList()
        val ll = LatLng(place.lat, place.lng)
        mapRef?.addMarker(MarkerOptions().position(ll))
        mapRef?.animateCamera(CameraUpdateFactory.newLatLngZoom(ll, 14.0))
        routeTo(Point.fromLngLat(place.lng, place.lat))
        destinationName = place.name
    }

    /** Same search as [searchPlaces]: several matches are listed, never the first one taken. */
    fun searchAndRoute() = searchPlaces()

    /** A saved place or a last destination, tapped: its route, ready to start. */
    fun routeToPlace(place: Place) {
        android.util.Log.d("MapLibrePanel", "routeToPlace: name='${place.name}' lat=${place.lat} lng=${place.lng}")
        
        // MINE: #14 no fixed Mexico box: a unit abroad was told "not found" for a real address. The router
        // still refuses what it cannot route, and routeTo drops absurd distances.
        clearRoute()
        val ll = LatLng(place.lat, place.lng)
        mapRef?.addMarker(MarkerOptions().position(ll))
        routeTo(Point.fromLngLat(place.lng, place.lat))
        destinationName = place.name
    }

    /**
     * Start: guidance on this map along the route shown, or in the navigation
     * app ([inOtherApp], or no route of our own yet), and the destination
     * kept among the last ones.
     */
    fun startGuidance(dest: Point, inOtherApp: Boolean = false) {
        val lat = dest.latitude()
        val lng = dest.longitude()
        val name = destinationName
        val shown = route
        // A route the guidance refuses goes to the navigation app instead.
        if (!inOtherApp && shown != null && hasLocationPerm(context) && InAppNav.start(context, shown, dest, name)) {
            // Back to the car straight away, not after the preview's pause.
            mapRef?.let { followVehicle(it.locationComponent, null, userZooms[it]) }
        } else if (!NavHandoff.start(context, lat, lng, name.orEmpty())) {
            error = context.getString(R.string.places_no_nav_app)
            return
        }
        scope.launch {
            // A spot picked on the map is kept under its address.
            val known = name ?: withContext(Dispatchers.IO) { PlaceSearch.nameOf(lat, lng) } ?: PlaceSearch.coordinates(lat, lng)
            PlacesStore.visited(context, Place(known, lat, lng))
        }
    }

    DisposableEffect(lifecycleOwner) {
        mapView.onCreate(null)
        // The observer is replayed up to the lifecycle's current state when
        // added, so it alone forwards start / resume: calling them here as well
        // started the map twice. What it forwarded is tracked, so disposing
        // only undoes what was done.
        var started = false
        var resumed = false
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> if (!started) { started = true; mapView.onStart() }
                Lifecycle.Event.ON_RESUME -> if (!resumed) { resumed = true; mapView.onResume() }
                Lifecycle.Event.ON_PAUSE -> if (resumed) { resumed = false; mapView.onPause() }
                Lifecycle.Event.ON_STOP -> if (started) { started = false; mapView.onStop() }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        // Low memory: the map drops its tile caches.
        val memory = object : ComponentCallbacks2 {
            @Suppress("DEPRECATION")
            override fun onTrimMemory(level: Int) {
                if (level == ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL) mapView.onLowMemory()
            }
            override fun onConfigurationChanged(newConfig: Configuration) {}
            @Deprecated("Deprecated in Java")
            override fun onLowMemory() = mapView.onLowMemory()
        }
        context.applicationContext.registerComponentCallbacks(memory)
        onDispose {
            context.applicationContext.unregisterComponentCallbacks(memory)
            lifecycleOwner.lifecycle.removeObserver(observer)
            if (resumed) runCatching { mapView.onPause() }
            if (started) runCatching { mapView.onStop() }
            runCatching { mapView.onDestroy() }
        }
    }

    // Map setup runs exactly once. It used to live in AndroidView's update
    // block, which re-runs on every recomposition (each search keystroke), so
    // the style reloaded and a new click listener stacked up every time.
    var styleReady by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        mapView.getMapAsync { map ->
            // No MapLibre wordmark on the dashboard. The attribution (i)
            // stays: OpenStreetMap's licence and the tile providers ask for it.
            map.uiSettings.isLogoEnabled = false
            // Under the dashboard nothing reaches the map by touch anyway.
            if (wallpaper) map.uiSettings.setAllGesturesEnabled(false)
            mapRef = map
        }
    }
    // The basemap follows the dashboard's dark or light version. Only the
    // style swaps on a change; the route line and click listener are set up
    // on the first load, and the location puck carries over by itself.
    val lightMap = DashColors.Light
    // The listener below is registered once; it reads the lock's latest state.
    val movingNow = rememberUpdatedState(moving)
    MapServers.load(context)
    val servers by MapServers.current.collectAsState()
    val styleUrl = if (lightMap) servers.styleLight else servers.styleDark
    LaunchedEffect(mapRef, styleUrl) {
        val map = mapRef ?: return@LaunchedEffect
        map.setStyle(Style.Builder().fromUri(styleUrl)) { style ->
            add3dBuildings(style, lightMap)
            if (navRoute == null) {
                navRoute = NavigationMapRoute(mapView, map)
                // A destination is set by a long press, not a tap: a tap while
                // panning, or a knock on the screen in a bumpy lane, used to
                // drop the route being followed and start a new one. Never
                // while the car moves (the drive lock).
                map.addOnMapLongClickListener { latLng ->
                    if (movingNow.value) return@addOnMapLongClickListener true
                    clearRoute()
                    mapRef?.addMarker(MarkerOptions().position(latLng))
                    routeTo(Point.fromLngLat(latLng.longitude, latLng.latitude))
                    true
                }
            }
            styleReady = true
        }
    }
    // The guided route on every map, the tile's and the wallpaper's: drawn
    // when it starts or changes (a new route after leaving it), with its
    // travelled part and the next turn's arrow; gone when it ends.
    val guidance by InAppNav.guidance.collectAsState()
    val guidedRoute = guidance?.route
    val navigation by InAppNav.navigation.collectAsState()
    var wasGuiding by remember { mutableStateOf(false) }
    LaunchedEffect(navRoute, guidedRoute) {
        val line = navRoute ?: return@LaunchedEffect
        if (guidedRoute != null) {
            line.addRoute(guidedRoute)
            wasGuiding = true
        } else if (wasGuiding) {
            wasGuiding = false
            clearRoute()
        }
    }
    DisposableEffect(navRoute, navigation) {
        val line = navRoute
        val nav = navigation
        if (line != null && nav != null) runCatching { line.addProgressChangeListener(nav) }
        onDispose { if (line != null && nav != null) runCatching { line.removeProgressChangeListener(nav) } }
    }
    // MINE: #13 the destination handed to Maps or Waze, drawn on this map too (tile and Canvas wallpaper).
    com.openauto.dash.mine.HandedOffRouteOnMap(mapRef, navRoute)

    // Location puck: enabled once the style is up, and again if the permission
    // is granted later from the runtime prompt.
    LaunchedEffect(hasLocation, styleReady) {
        if (!hasLocation || !styleReady) return@LaunchedEffect
        val map = mapRef ?: return@LaunchedEffect
        map.getStyle { style -> enableLocation(map, style, context, scope, if (wallpaper) mapView else null) { stillNow } }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // A layer of its own: each map frame re-records only this view, not the page over it.
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize().graphicsLayer())
        // The search and route panels belong to the map tile, not to a wallpaper.
        if (!wallpaper) {
            // Destination search bar. Searching from the IME key, the search button
            // or a hardware Enter all dismiss the keyboard first so the map is
            // visible while the route loads.
            val keyboard = LocalSoftwareKeyboardController.current
            val focusManager = LocalFocusManager.current
            fun submitSearch() {
                keyboard?.hide()
                focusManager.clearFocus()
                searchPlaces()
            }
            val places by PlacesStore.places.collectAsState()
            LaunchedEffect(Unit) { PlacesStore.load(context) }
            // While guiding, the turn banner has the top of the map.
            if (guidance == null) Column(
                modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
            Surface(
                color = OverlayBg,
                shape = DashShape.Medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text(stringResource(if (moving) R.string.dash_drive_lock_notice else R.string.info_map_where_to), color = Color(0xFF9AA0A6)) },
                        enabled = !moving,
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .onPreviewKeyEvent { event ->
                                if (event.type == KeyEventType.KeyUp &&
                                    (event.key == Key.Enter || event.key == Key.NumPadEnter)
                                ) {
                                    submitSearch(); true
                                } else false
                            },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Accent,
                            unfocusedBorderColor = Color(0xFF2A2D33),
                            cursorColor = Accent
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        // Head-unit keyboards label the action key differently; accept them all.
                        keyboardActions = KeyboardActions(
                            onSearch = { submitSearch() },
                            onDone = { submitSearch() },
                            onGo = { submitSearch() },
                            onSend = { submitSearch() }
                        )
                    )
                    Spacer(Modifier.width(6.dp))
                    if (loading) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp), color = Accent, strokeWidth = 3.dp)
                    } else {
                        IconButton(onClick = { submitSearch() }) {
                            Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.info_map_search), tint = Accent)
                        }
                    }
                    if (route != null) {
                        IconButton(onClick = { clearRoute() }) {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.info_map_clear_route), tint = Color(0xFF9AA0A6))
                        }
                    }
                }
            }
            // Search results list
            if (searchResults.isNotEmpty()) {
                Surface(
                    color = OverlayBg,
                    shape = DashShape.Medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        // Dismissed without picking: the typed address stays, so it can be corrected.
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            IconButton(onClick = { searchResults = emptyList() }) {
                                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.dash_close), tint = Color(0xFF9AA0A6))
                            }
                        }
                        // MINE: #14 the number the driver typed is in none of these: say so, never route somewhere else silently.
                        val typedNumber = com.openauto.dash.mine.PlaceQuery.typed(query).number
                        if (typedNumber != null && searchResults.none { com.openauto.dash.mine.PlaceQuery.hasNumber(it.name, typedNumber) }) {
                            Text(
                                text = stringResource(R.string.mine_search_number_not_found, typedNumber),
                                color = Color(0xFFF28B82),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                            )
                        }
                        searchResults.forEach { place ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { pickSearchResult(place) }
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.LocationOn,
                                    contentDescription = null,
                                    tint = Accent,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                // MINE: #14 two lines per result, name over the rest of the address, and a mark when
                                // the typed house number is not this one.
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = com.openauto.dash.mine.PlaceQuery.title(place.name),
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val detail = com.openauto.dash.mine.PlaceQuery.detail(place.name)
                                    val missing = typedNumber != null && !com.openauto.dash.mine.PlaceQuery.hasNumber(place.name, typedNumber)
                                    Text(
                                        text = if (missing) {
                                            val mark = stringResource(R.string.mine_search_no_number)
                                            if (detail.isEmpty()) mark else "$mark · $detail"
                                        } else {
                                            detail
                                        },
                                        color = if (missing) Color(0xFFF28B82) else Color(0xFF9AA0A6),
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
            // Out of the way once a destination is set: the route is what matters then.
            if (destination == null && searchResults.isEmpty()) PlaceChips(places, onPick = { routeToPlace(it) })
            }

            val guiding = guidance
            if (guiding != null) {
                GuidancePanel(guiding, modifier = Modifier.align(Alignment.BottomStart).padding(10.dp))
                return@Box
            }
            // Route info / error + Start (on this map; or in Google Maps / Waze).
            val currentInfo = info
            val currentError = error
            val hasDest = destination != null
            if (currentInfo != null || currentError != null || hasDest) {
                Surface(
                    color = OverlayBg,
                    shape = DashShape.Medium,
                    modifier = Modifier.align(Alignment.BottomStart).padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // MINE: #14 what Start is about to take: the destination's name, next to the route's figures.
                        destinationName?.let { name ->
                            Text(
                                text = com.openauto.dash.mine.PlaceQuery.title(name),
                                color = Color.White,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = 220.dp)
                            )
                        }
                        Text(
                            text = currentInfo ?: currentError ?: stringResource(R.string.info_map_ready),
                            color = if (currentError != null) Color(0xFFF28B82) else Color.White
                        )
                        // Close button to dismiss error messages
                        if (currentError != null) {
                            IconButton(
                                onClick = { error = null },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Cerrar",
                                    tint = Color(0xFFF28B82),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        if (hasDest) {
                            Button(
                                onClick = { destination?.let { startGuidance(it) } },
                                colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Color(0xFF0B0C0F)),
                                modifier = Modifier.heightIn(min = DashSize.TouchPrimary)
                            ) {
                                Icon(Icons.Filled.Navigation, contentDescription = null, modifier = Modifier.size(22.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.info_map_start))
                            }
                            // The same trip in Google Maps or Waze instead.
                            if (route != null) {
                                IconButton(onClick = { destination?.let { startGuidance(it, inOtherApp = true) } }) {
                                    Icon(Icons.Filled.OpenInNew, contentDescription = stringResource(R.string.info_map_other_app), tint = Color(0xFF9AA0A6))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Under the map while guiding: time, distance and arrival left, the voice switch and Stop. */
@Composable
private fun GuidancePanel(guidance: InAppNav.Guidance, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val voiceOn by InAppNav.voiceOn.collectAsState()
    Surface(color = OverlayBg, shape = DashShape.Medium, modifier = modifier) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = when {
                    guidance.arrived -> stringResource(R.string.info_map_arrived)
                    guidance.rerouting -> stringResource(R.string.info_map_rerouting)
                    else -> guidance.remaining.ifEmpty { stringResource(R.string.info_map_ready) }
                },
                color = Color.White
            )
            IconButton(
                onClick = { InAppNav.setVoice(context, !voiceOn) },
                modifier = Modifier.size(DashSize.TouchPrimary)
            ) {
                Icon(
                    if (voiceOn) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                    contentDescription = stringResource(if (voiceOn) R.string.info_map_voice_off else R.string.info_map_voice_on),
                    tint = if (voiceOn) Accent else Color(0xFF9AA0A6)
                )
            }
            Button(
                onClick = { InAppNav.stop() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2D33), contentColor = Color.White),
                modifier = Modifier.heightIn(min = DashSize.TouchPrimary)
            ) {
                Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.info_map_stop))
            }
        }
    }
}

@SuppressLint("MissingPermission")
private fun enableLocation(
    map: MapLibreMap,
    style: Style,
    context: Context,
    scope: kotlinx.coroutines.CoroutineScope,
    /** Set when the map is a wallpaper: the camera sits lower and the car low on it. */
    wallpaper: MapView? = null,
    /** Whether the car stands still: a fix that only wanders a few metres then is not worth a camera move. */
    still: () -> Boolean = { false }
) {
    if (!hasLocationPerm(context)) return
    val lc = map.locationComponent
    runCatching {
        if (!lc.isLocationComponentActivated) {
            // Tracking gestures management: a two-finger pinch zooms around the
            // car and keeps following it, instead of counting as a pan.
            // The fixes are Dashwheel's own ([LocationFeed]), not a second GPS
            // listener of the map's: parked, GPS wander of a metre or two would
            // otherwise animate the camera and the puck after every fix, and
            // keep the map's workers placing labels for a picture that does not change.
            lc.activateLocationComponent(
                LocationComponentActivationOptions.builder(context, style)
                    .locationComponentOptions(
                        LocationComponentOptions.builder(context)
                            .trackingGesturesManagement(true)
                            .build()
                    )
                    .useDefaultLocationEngine(false)
                    .build()
            )
            scope.launch {
                var last: android.location.Location? = null
                LocationFeed.location.collect { loc ->
                    if (loc == null) return@collect
                    val l = last
                    if (l != null && still() && loc.distanceTo(l) < STILL_WANDER_M) return@collect
                    last = loc
                    runCatching { lc.forceLocationUpdate(loc) }
                    // The first fix: tracking was turned on below with nowhere to fly to
                    // yet, so the camera only followed from where the style left it (the
                    // whole world). Now there is a car to close in on, however long the
                    // GPS took to find it.
                    if (l == null) followVehicle(lc, wallpaper, userZooms[map])
                }
            }
            // The zoom the driver picks by hand is the one tracking comes back
            // to; route previews and search hits don't change it.
            var byGesture = false
            map.addOnCameraMoveStartedListener { reason ->
                byGesture = reason == MapLibreMap.OnCameraMoveStartedListener.REASON_API_GESTURE
            }
            map.addOnCameraIdleListener {
                if (byGesture) userZooms[map] = map.cameraPosition.zoom
                byGesture = false
            }
            // Any gesture or programmatic camera move (route preview, search hit)
            // drops the component out of tracking mode. In a car the map has to
            // come back to the vehicle by itself, so tracking resumes a few
            // seconds after the last interruption.
            var resume: Job? = null
            lc.addOnCameraTrackingChangedListener(object : OnCameraTrackingChangedListener {
                override fun onCameraTrackingDismissed() {
                    resume?.cancel()
                    resume = scope.launch {
                        delay(TRACKING_RESUME_MS)
                        followVehicle(lc, wallpaper, userZooms[map])
                    }
                }

                override fun onCameraTrackingChanged(currentMode: Int) {
                    if (currentMode != CameraMode.NONE) resume?.cancel()
                }
            })
        }
        lc.isLocationComponentEnabled = true
        // Directional puck; the camera follows position AND heading (map rotates
        // with the car) at a close zoom with pitch for the driving-nav look.
        lc.renderMode = RenderMode.COMPASS
        // With a fix already known (the permission granted later, the style
        // swapped) the camera goes to it now; else the first fix above takes it
        // there. Through the component either way: a plain animateCamera would
        // count as a developer move and cancel tracking straight away.
        followVehicle(lc, wallpaper, userZooms[map])
    }
}

/** Seconds of free panning before the camera snaps back to the vehicle. */
private const val TRACKING_RESUME_MS = 10_000L

/** A fix this close to the last one shown, with the car standing still, is GPS wander: the map keeps the last. */
private const val STILL_WANDER_M = 3f

/** Zoom last set by a pinch on each map; tracking resumes at it instead of the default. */
private val userZooms = java.util.WeakHashMap<MapLibreMap, Double>()

/**
 * Follow position and heading with the driving-nav zoom and pitch, keeping
 * tracking on. A wallpaper looks further ahead: steeper, a little closer, and
 * the car pushed down to the lower part of the screen, between the panels.
 *
 * Turning tracking on flies to the car only when the component knows where
 * it is at that moment; asked for again once it does, setCameraMode returns
 * at once (same mode) and the camera stayed where the style had left it, the
 * world in view with the puck lost on it. Already following, the zoom and
 * the tilt are set on the tracking camera instead.
 */
private fun followVehicle(lc: LocationComponent, wallpaper: MapView? = null, zoom: Double? = null) {
    val close = zoom ?: if (wallpaper == null) 16.5 else 17.0
    val tilt = if (wallpaper == null) 45.0 else 60.0
    runCatching {
        if (lc.cameraMode == CameraMode.TRACKING_GPS) {
            if (lc.lastKnownLocation != null) {
                lc.zoomWhileTracking(close, 1000L)
                lc.tiltWhileTracking(tilt, 1000L)
            }
        } else {
            lc.setCameraMode(CameraMode.TRACKING_GPS, 1000L, close, null, tilt, null)
        }
        // Measured when called: at the first fix the view has its height, which at set-up it had not.
        if (wallpaper != null) lc.paddingWhileTracking(doubleArrayOf(0.0, wallpaper.height * 0.42, 0.0, 0.0))
    }
}

/** The wallpaper map's frame cap: smooth enough for a car moving under it, half the work of 60. */
private const val WALLPAPER_FPS = 30

/**
 * A map's frame cap on a page off screen. The cap is a sleep after each frame
 * on the map's render thread, which lifting it does not cut short, and the
 * main thread waits it out when the map's view goes: a tenth of a second at
 * most, over by the time a sliding page shows much of the map.
 */
private const val HIDDEN_FPS = 10

/**
 * Adds extruded 3D buildings to the vector basemap, tinted for its [light] or dark
 * version. The building geometry lives in the style's vector source under the
 * OpenMapTiles `building` source-layer with `render_height` / `render_min_height`.
 * We detect the source id at runtime so this works regardless of what the style
 * names it, and skip silently if unavailable.
 */
private fun add3dBuildings(style: Style, light: Boolean) {
    runCatching {
        val sourceId = style.sources.firstOrNull { it is VectorSource }?.id ?: return
        if (style.getLayer("3d-buildings") != null) return
        val layer = FillExtrusionLayer("3d-buildings", sourceId).apply {
            sourceLayer = "building"
            setProperties(
                PropertyFactory.fillExtrusionColor(android.graphics.Color.parseColor(if (light) "#D5D8DE" else "#2B2F36")),
                PropertyFactory.fillExtrusionHeight(Expression.get("render_height")),
                PropertyFactory.fillExtrusionBase(Expression.get("render_min_height")),
                PropertyFactory.fillExtrusionOpacity(0.85f)
            )
            setMinZoom(14f)
        }
        style.addLayer(layer)
    }
}

private fun hasLocationPerm(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED

/**
 * Home, work and the last destinations as buttons under the search bar: one
 * tap shows the route, moving or not, where typing has to wait for a stop.
 */
@Composable
private fun PlaceChips(places: Places, onPick: (Place) -> Unit) {
    val home = stringResource(R.string.places_home)
    val work = stringResource(R.string.places_work)
    val chips = remember(places, home, work) {
        listOfNotNull(
            places.home?.let { Triple(Icons.Filled.Home, home, it) },
            places.work?.let { Triple(Icons.Filled.Work, work, it) }
        ) + places.recent.map { Triple(Icons.Filled.History, it.name, it) }
    }
    if (chips.isEmpty()) return
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        chips.forEach { (icon, label, place) ->
            Surface(onClick = { onPick(place) }, color = OverlayBg, shape = DashShape.Pill) {
                Row(
                    modifier = Modifier.heightIn(min = DashSize.TouchPrimary).padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(icon, contentDescription = null, tint = Accent, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(label, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 220.dp))
                }
            }
        }
    }
}

/** Meters around each pin to snap onto a road (parked off-street, building centroid). */
private const val SNAP_RADIUS_M = 200

/** Calculates distance in km between two points using Haversine formula. */
private fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val R = 6371.0 // Earth's radius in km
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2)
    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    return R * c
}

/** Fetches a driving route from a free Valhalla server (OSRM-format response). */
internal fun valhallaRoute(origin: Point, dest: Point, language: String): DirectionsResponse {
    fun loc(lon: Double, lat: Double) = mapOf(
        "lon" to lon, "lat" to lat, "type" to "break", "radius" to SNAP_RADIUS_M
    )
    val body = mapOf(
        "format" to "osrm",
        "costing" to "auto",
        "banner_instructions" to true,
        "voice_instructions" to true,
        "language" to language,
        // Top-level units (directions_options is deprecated and some servers reject it).
        "units" to if (Units.current.value.imperial) "miles" else "kilometers",
        "locations" to listOf(loc(origin.longitude(), origin.latitude()), loc(dest.longitude(), dest.latitude()))
    )
    val json = Gson().toJson(body)
    android.util.Log.d("MapLibrePanel", "Routing request: origin=(${origin.latitude()},${origin.longitude()}) dest=(${dest.latitude()},${dest.longitude()})")
    android.util.Log.d("MapLibrePanel", "Request body: $json")
    val routingUrl = MapServers.current.value.routing
    val request = Request.Builder()
        .header("User-Agent", USER_AGENT)
        .url(routingUrl)
        .post(json.toRequestBody("application/json; charset=utf-8".toMediaType()))
        .build()
    Http.client.newCall(request).execute().use { resp ->
        if (!resp.isSuccessful) {
            val errBody = resp.body?.string().orEmpty()
            android.util.Log.e("MapLibrePanel", "Routing failed: HTTP ${resp.code} - Response: $errBody")
            error(valhallaFailure(resp.code, errBody))
        }
        val rb = resp.body?.string() ?: error("Empty routing response")
        // Valhalla doesn't echo the request back, and the navigation refuses a
        // route without it (its spoken turns need voice and banner instructions).
        val options = RouteOptions(
            baseUrl = routingUrl,
            user = "valhalla",
            profile = "auto",
            coordinates = listOf(origin, dest),
            language = language,
            geometries = "polyline6",
            steps = true,
            voiceInstructions = true,
            bannerInstructions = true
        )
        val parsed = DirectionsResponse.fromJson(rb)
        return parsed.copy(routes = parsed.routes.map { it.copy(routeOptions = options) })
    }
}

/**
 * Valhalla's refusal, for the map's error line: its own message when the body
 * has one (DistanceExceeded, NoSegment, …), else just the HTTP status.
 */
internal fun valhallaFailure(code: Int, body: String): String {
    val detail = runCatching {
        val o = com.google.gson.JsonParser.parseString(body).asJsonObject
        sequenceOf("message", "error").mapNotNull { key ->
            o.get(key)?.takeIf { it.isJsonPrimitive }?.asString?.trim()?.takeIf { it.isNotEmpty() }
        }.firstOrNull()
    }.getOrNull()
    return if (detail != null) "$detail (HTTP $code)" else "HTTP $code"
}

private fun formatEta(context: Context, distanceMeters: Double?, durationSeconds: Double?): String {
    val units = Units.current.value
    val d = units.distance((distanceMeters ?: 0.0) / 1000.0)
    val mins = ((durationSeconds ?: 0.0) / 60.0).toInt()
    val dist = (if (d >= 10) "%.0f %s" else "%.1f %s").format(d, units.distanceUnit)
    val time = if (mins >= 60) context.getString(R.string.info_map_duration_hm, mins / 60, mins % 60)
    else context.getString(R.string.info_map_duration_min, mins)
    return "$dist · $time"
}
