package com.openauto.dash

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PropaneTank
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/*
 * The settings of what Dashwheel does without a tap: how what it says by
 * itself is named in Settings, Alerts, and the places a button leads to
 * (Settings, Driving).
 */

/** The spoken events' icon, name and line in the Alerts grid (AlertStyleDialog.kt). */
internal val SpokenEvent.icon: ImageVector
    get() = when (this) {
        SpokenEvent.BRIEFING -> Icons.Filled.WbSunny
        SpokenEvent.CAR_TIPS -> Icons.Filled.Campaign
        SpokenEvent.FAULT_CODES -> Icons.Filled.Build
        SpokenEvent.SPEED_LIMIT -> Icons.Filled.Speed
        SpokenEvent.SPEED_CAMERAS -> Icons.Filled.CameraAlt
        SpokenEvent.WEATHER -> Icons.Filled.Thunderstorm
        SpokenEvent.MESSAGES -> Icons.AutoMirrored.Filled.Message
        SpokenEvent.LPG -> Icons.Filled.PropaneTank
    }

internal val SpokenEvent.label: Int
    get() = when (this) {
        SpokenEvent.BRIEFING -> R.string.briefing_setting
        SpokenEvent.CAR_TIPS -> R.string.voice_warnings
        SpokenEvent.FAULT_CODES -> R.string.voice_faults
        SpokenEvent.SPEED_LIMIT -> R.string.widgets_limit
        SpokenEvent.SPEED_CAMERAS -> R.string.widgets_cam
        SpokenEvent.WEATHER -> R.string.voice_weather
        SpokenEvent.MESSAGES -> R.string.voice_messages
        SpokenEvent.LPG -> R.string.voice_lpg
    }

internal val SpokenEvent.detail: Int
    get() = when (this) {
        SpokenEvent.BRIEFING -> R.string.briefing_setting_detail
        SpokenEvent.CAR_TIPS -> R.string.voice_warnings_detail
        SpokenEvent.FAULT_CODES -> R.string.voice_faults_detail
        SpokenEvent.SPEED_LIMIT -> R.string.voice_limit_detail
        SpokenEvent.SPEED_CAMERAS -> R.string.widgets_cam_silent
        SpokenEvent.WEATHER -> R.string.voice_weather_detail
        SpokenEvent.MESSAGES -> R.string.voice_messages_detail
        SpokenEvent.LPG -> R.string.voice_lpg_detail
    }

/** The icon a category draws: the same in Settings and on the map tile. */
internal fun placeIcon(category: String): ImageVector = when (PlaceCategories.known(category)) {
    PlaceCategories.HOME -> Icons.Filled.Home
    PlaceCategories.WORK -> Icons.Filled.Work
    PlaceCategories.GYM -> Icons.Filled.FitnessCenter
    PlaceCategories.SCHOOL -> Icons.Filled.School
    PlaceCategories.HOSPITAL -> Icons.Filled.LocalHospital
    PlaceCategories.RESTAURANT -> Icons.Filled.Restaurant
    PlaceCategories.STORE -> Icons.Filled.ShoppingCart
    else -> Icons.Filled.Place
}

/** What the driver calls [place]: the renamed home and work, or what they typed for their own. */
@Composable
internal fun savedPlaceTitle(place: SavedPlace, places: Places): String = when (place.id) {
    PlaceCategories.HOME -> places.homeLabel ?: stringResource(R.string.widgets_hw_home)
    PlaceCategories.WORK -> places.workLabel ?: stringResource(R.string.widgets_hw_work)
    else -> place.name
}

private fun categoryLabel(category: String): Int = when (PlaceCategories.known(category)) {
    PlaceCategories.GYM -> R.string.places_custom_category_gym
    PlaceCategories.SCHOOL -> R.string.places_custom_category_school
    PlaceCategories.HOSPITAL -> R.string.places_custom_category_hospital
    PlaceCategories.RESTAURANT -> R.string.places_custom_category_restaurant
    PlaceCategories.STORE -> R.string.places_custom_category_store
    else -> R.string.places_custom_category_generic
}

/** Settings, Driving: the row that opens the driver's places. */
@Composable
internal fun PlacesRow(onOpen: () -> Unit) {
    val places by PlacesStore.places.collectAsState()
    val set = buildList {
        places.home?.let { add(places.homeLabel ?: stringResource(R.string.places_home)) }
        places.work?.let { add(places.workLabel ?: stringResource(R.string.places_work)) }
        addAll(places.customPlaces.map { it.name })
    }
    SettingsRow(
        Icons.Filled.Place, stringResource(R.string.places_title),
        if (set.isEmpty()) stringResource(R.string.places_detail_empty) else set.joinToString(", "),
        onOpen
    )
}

/**
 * Home and work: typed once here, then one tap on the map tile or one
 * steering wheel button away. Set from an address, or from where the car is.
 */
@Composable
internal fun PlacesSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val places by PlacesStore.places.collectAsState()
    LaunchedEffect(Unit) { PlacesStore.load(context) }
    SettingsSheet(
        title = stringResource(R.string.places_title),
        onDismiss = onDismiss,
        actions = { SheetButton(stringResource(R.string.ai_done), onClick = onDismiss) }
    ) {
        Text(stringResource(R.string.places_explanation), color = DashColors.TextSecondary, style = MaterialTheme.typography.bodyMedium)
        PlaceEditor(
            stringResource(R.string.places_home), places.home, places.homeLabel,
            onRename = { PlacesStore.renamePlace(context, PlaceCategories.HOME, it) }
        ) { PlacesStore.setHome(context, it) }
        PlaceEditor(
            stringResource(R.string.places_work), places.work, places.workLabel,
            onRename = { PlacesStore.renamePlace(context, PlaceCategories.WORK, it) }
        ) { PlacesStore.setWork(context, it) }
        CustomPlacesList(places)
        WidgetPlacesSelector(places)
        if (places.recent.isNotEmpty()) {
            Label(stringResource(R.string.places_recent))
            Text(places.recent.joinToString("\n") { it.name }, color = DashColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
            SheetButton(stringResource(R.string.places_forget_recent), primary = false) { PlacesStore.forgetRecent(context) }
        }
    }
}

/** One place: where it is now, an address to find it by, the car's own position, and forgetting it. */
@Composable
private fun PlaceEditor(
    title: String,
    place: Place?,
    label: String?,
    onRename: (String) -> Unit,
    onSet: (Place?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    var address by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var problem by remember { mutableStateOf<String?>(null) }
    var renaming by remember { mutableStateOf(false) }
    // More than one match: the driver picks, as the map tile does.
    var results by remember { mutableStateOf<List<Place>>(emptyList()) }
    val notFound = stringResource(R.string.info_map_address_not_found)
    val offline = stringResource(R.string.places_offline)
    val noPosition = stringResource(R.string.voice_no_position)

    fun find() {
        val q = address.trim()
        if (q.isEmpty() || busy) return
        keyboard?.hide()
        busy = true
        problem = null
        results = emptyList()
        scope.launch {
            val here = HandsFree.position(context)
            val found = withContext(Dispatchers.IO) {
                runCatching { PlaceSearch.findAll(q, here?.latitude, here?.longitude, limit = 5) }
            }
            busy = false
            val places = found.getOrDefault(emptyList())
            when {
                found.isFailure -> problem = offline
                places.isEmpty() -> problem = notFound
                // One match needs no list; several could be the wrong town.
                places.size == 1 -> {
                    onSet(places[0])
                    address = ""
                }
                else -> results = places
            }
        }
    }

    fun here() {
        val at = HandsFree.position(context)
        if (at == null) {
            problem = noPosition
            return
        }
        busy = true
        problem = null
        results = emptyList()
        scope.launch {
            val name = withContext(Dispatchers.IO) { PlaceSearch.nameOf(at.latitude, at.longitude) }
                ?: PlaceSearch.coordinates(at.latitude, at.longitude)
            busy = false
            onSet(Place(name, at.latitude, at.longitude))
        }
    }

    Label(title)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            label ?: place?.name ?: stringResource(R.string.places_not_set),
            color = if (place != null) DashColors.TextPrimary else DashColors.Muted,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (place != null) {
            IconButton(onClick = { renaming = true }) {
                Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.places_custom_rename), tint = DashColors.TextSecondary)
            }
        }
    }
    if (renaming && place != null) {
        RenamePlaceDialog(
            title = title,
            current = label ?: place.name,
            onDismiss = { renaming = false },
            onRename = onRename
        )
    }
    OutlinedTextField(
        value = address,
        onValueChange = { address = it; problem = null; results = emptyList() },
        placeholder = { Text(stringResource(R.string.places_address_hint), color = DashColors.Muted) },
        singleLine = true,
        enabled = !busy,
        modifier = Modifier.fillMaxWidth(),
        colors = fieldColors(),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { find() }, onDone = { find() }, onGo = { find() })
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        SheetButton(stringResource(R.string.places_find)) { find() }
        SheetButton(stringResource(R.string.places_here), primary = false) { here() }
        if (place != null) SheetButton(stringResource(R.string.places_forget), primary = false) { onSet(null) }
    }
    // Several matches: tap the right one, or dismiss the list and keep the address typed.
    if (results.isNotEmpty()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            IconButton(onClick = { results = emptyList() }) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.dash_close), tint = DashColors.TextSecondary)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.heightIn(max = 200.dp)) {
            results.forEach { candidate ->
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .clickable {
                            onSet(candidate)
                            results = emptyList()
                            address = ""
                        }
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.LocationOn, contentDescription = null, tint = DashColors.Accent)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        candidate.name,
                        color = DashColors.TextPrimary,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
    problem?.let { Text(it, color = DashColors.Warning, style = MaterialTheme.typography.bodySmall) }
    Spacer(Modifier.height(8.dp))
}

/** Renames home or work: only the name the driver sees changes, not the spot. */
@Composable
private fun RenamePlaceDialog(title: String, current: String, onDismiss: () -> Unit, onRename: (String) -> Unit) {
    var text by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DashColors.Card,
        title = { Text(title, color = DashColors.TextPrimary) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(stringResource(R.string.places_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = fieldColors()
            )
        },
        confirmButton = {
            TextButton(
                enabled = text.isNotBlank(),
                onClick = { onRename(text); onDismiss() }
            ) { Text(stringResource(R.string.places_custom_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.places_custom_cancel)) } }
    )
}


/** The places the driver added: one row each, and the way to add one while there is room. */
@Composable
private fun CustomPlacesList(places: Places) {
    val context = LocalContext.current
    var adding by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<SavedPlace?>(null) }
    var deleting by remember { mutableStateOf<SavedPlace?>(null) }

    Label(stringResource(R.string.places_custom_title))
    if (places.customPlaces.isEmpty()) {
        Text(
            stringResource(R.string.places_custom_empty),
            color = DashColors.TextSecondary,
            style = MaterialTheme.typography.bodyMedium
        )
    } else {
        Spacer(Modifier.height(8.dp))
        places.customPlaces.forEach { place ->
            CustomPlaceRow(place, onEdit = { editing = place }, onDelete = { deleting = place })
            Spacer(Modifier.height(8.dp))
        }
    }
    if (places.customPlaces.size >= Places.MAX_CUSTOM_PLACES) {
        Text(
            stringResource(R.string.places_custom_max_reached),
            color = DashColors.Muted,
            style = MaterialTheme.typography.bodySmall
        )
    } else {
        SheetButton(stringResource(R.string.places_custom_add), primary = false) { adding = true }
    }

    if (adding) {
        CustomPlaceDialog(initial = null, onDismiss = { adding = false }) { name, category, lat, lng ->
            PlacesStore.addCustomPlace(context, name, lat, lng, category)
            adding = false
        }
    }
    editing?.let { place ->
        CustomPlaceDialog(initial = place, onDismiss = { editing = null }) { name, category, lat, lng ->
            PlacesStore.updateCustomPlace(context, place.id, name, lat, lng, category)
            editing = null
        }
    }
    deleting?.let { place ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            containerColor = DashColors.Card,
            title = { Text(stringResource(R.string.places_custom_confirm_delete), color = DashColors.TextPrimary) },
            confirmButton = {
                TextButton(onClick = { PlacesStore.removeCustomPlace(context, place.id); deleting = null }) {
                    Text(stringResource(R.string.places_custom_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) { Text(stringResource(R.string.places_custom_cancel)) }
            }
        )
    }
}

/** One custom place: its icon, its name, and the way to change or drop it. */
@Composable
private fun CustomPlaceRow(place: SavedPlace, onEdit: () -> Unit, onDelete: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(placeIcon(place.category), contentDescription = null, tint = DashColors.Accent)
        Text(
            place.name,
            color = DashColors.TextPrimary,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onEdit) {
            Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.places_custom_edit), tint = DashColors.TextSecondary)
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.places_custom_delete), tint = DashColors.TextSecondary)
        }
    }
}

/**
 * Adds a place or changes one: what it is called, the icon it gets, and where
 * it is (an address, or wherever the car is standing).
 */
@Composable
private fun CustomPlaceDialog(
    initial: SavedPlace?,
    onDismiss: () -> Unit,
    onSave: (name: String, category: String, lat: Double, lng: Double) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var category by remember { mutableStateOf(PlaceCategories.known(initial?.category ?: PlaceCategories.GENERIC)) }
    var lat by remember { mutableStateOf(initial?.lat ?: Double.NaN) }
    var lng by remember { mutableStateOf(initial?.lng ?: Double.NaN) }
    var address by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var problem by remember { mutableStateOf<String?>(null) }
    var categoryMenu by remember { mutableStateOf(false) }
    // More than one match: the driver picks, as the map tile does.
    var results by remember { mutableStateOf<List<Place>>(emptyList()) }
    val notFound = stringResource(R.string.info_map_address_not_found)
    val offline = stringResource(R.string.places_offline)
    val noPosition = stringResource(R.string.voice_no_position)
    val spotSet = !lat.isNaN() && !lng.isNaN()

    fun setName(found: Place) {
        lat = found.lat
        lng = found.lng
        if (name.isBlank()) name = found.name
    }

    fun find() {
        val q = address.trim()
        if (q.isEmpty() || busy) return
        keyboard?.hide()
        busy = true
        problem = null
        results = emptyList()
        scope.launch {
            val here = HandsFree.position(context)
            val found = withContext(Dispatchers.IO) {
                runCatching { PlaceSearch.findAll(q, here?.latitude, here?.longitude, limit = 5) }
            }
            busy = false
            val places = found.getOrDefault(emptyList())
            when {
                found.isFailure -> problem = offline
                places.isEmpty() -> problem = notFound
                // One match needs no list; several could be the wrong town.
                places.size == 1 -> {
                    setName(places[0])
                    address = ""
                }
                else -> results = places
            }
        }
    }

    fun pick(found: Place) {
        setName(found)
        results = emptyList()
        address = ""
    }

    fun here() {
        val at = HandsFree.position(context)
        if (at == null) {
            problem = noPosition
            return
        }
        busy = true
        problem = null
        results = emptyList()
        scope.launch {
            val found = withContext(Dispatchers.IO) { PlaceSearch.nameOf(at.latitude, at.longitude) }
                ?: PlaceSearch.coordinates(at.latitude, at.longitude)
            busy = false
            setName(Place(found, at.latitude, at.longitude))
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DashColors.Card,
        title = {
            Text(
                stringResource(if (initial == null) R.string.places_custom_add else R.string.places_custom_edit),
                color = DashColors.TextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; problem = null },
                    label = { Text(stringResource(R.string.places_name)) },
                    singleLine = true,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors()
                )

                Box {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { categoryMenu = true }
                    ) {
                        Icon(placeIcon(category), contentDescription = null, tint = DashColors.Accent)
                        Text(
                            stringResource(categoryLabel(category)),
                            color = DashColors.TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            stringResource(R.string.places_custom_category),
                            color = DashColors.Muted,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    DropdownMenu(expanded = categoryMenu, onDismissRequest = { categoryMenu = false }) {
                        PlaceCategories.pickable.forEach { option ->
                            DropdownMenuItem(
                                leadingIcon = { Icon(placeIcon(option), contentDescription = null) },
                                text = { Text(stringResource(categoryLabel(option))) },
                                onClick = { category = option; categoryMenu = false }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it; problem = null; results = emptyList() },
                    placeholder = { Text(stringResource(R.string.places_address_hint), color = DashColors.Muted) },
                    singleLine = true,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { find() }, onDone = { find() }, onGo = { find() })
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    SheetButton(stringResource(R.string.places_find)) { find() }
                    SheetButton(stringResource(R.string.places_here), primary = false) { here() }
                }
                // Several matches: tap the right one, or dismiss the list and keep the address typed.
                if (results.isNotEmpty()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        IconButton(onClick = { results = emptyList() }) {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.dash_close), tint = DashColors.TextSecondary)
                        }
                    }
                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.heightIn(max = 200.dp)
                    ) {
                        results.forEach { candidate ->
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .clickable { pick(candidate) }
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.LocationOn, contentDescription = null, tint = DashColors.Accent)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    candidate.name,
                                    color = DashColors.TextPrimary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
                Text(
                    stringResource(if (spotSet) R.string.places_custom_location_set else R.string.places_custom_location_missing),
                    color = if (spotSet) DashColors.TextSecondary else DashColors.Muted,
                    style = MaterialTheme.typography.bodySmall
                )
                problem?.let { Text(it, color = DashColors.Warning, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && spotSet && !busy,
                onClick = { onSave(name, category, lat, lng) }
            ) { Text(stringResource(R.string.places_custom_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.places_custom_cancel)) }
        }
    )
}

/** Which places the map tile shows, up to [Places.MAX_WIDGET_PLACES], in the order they are ticked. */
@Composable
private fun WidgetPlacesSelector(places: Places) {
    val context = LocalContext.current
    val all = places.allPlaces()
    Label(stringResource(R.string.places_custom_widget_title))
    if (all.isEmpty()) {
        Text(
            stringResource(R.string.places_custom_widget_empty),
            color = DashColors.TextSecondary,
            style = MaterialTheme.typography.bodyMedium
        )
        return
    }
    Text(
        stringResource(R.string.places_custom_widget_hint, Places.MAX_WIDGET_PLACES),
        color = DashColors.Muted,
        style = MaterialTheme.typography.bodySmall
    )
    all.forEach { place ->
        val checked = place.id in places.widgetPlaceIds
        val roomLeft = places.widgetPlaceIds.size < Places.MAX_WIDGET_PLACES
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().clickable(enabled = checked || roomLeft) {
                PlacesStore.toggleWidgetPlace(context, place.id)
            }
        ) {
            Checkbox(
                checked = checked,
                enabled = checked || roomLeft,
                onCheckedChange = { PlacesStore.toggleWidgetPlace(context, place.id) }
            )
            Icon(placeIcon(place.category), contentDescription = null, tint = DashColors.Accent)
            Spacer(Modifier.width(8.dp))
            Text(
                savedPlaceTitle(place, places),
                color = DashColors.TextPrimary,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
