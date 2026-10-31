// MINE: Ajustes → Aspecto → Mapa (#16). La hoja que escribe [MapOptions];
// el mapa lo aplica [MapLook]. Todo lo que hay aquí es nuestro.
package com.openauto.dash.mine

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material.icons.filled.Traffic
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.openauto.dash.DashColors
import com.openauto.dash.DashShape
import com.openauto.dash.DashSize
import com.openauto.dash.Label
import com.openauto.dash.MapServers
import com.openauto.dash.R
import com.openauto.dash.SettingsSection
import com.openauto.dash.SettingsSheet
import com.openauto.dash.SettingsToggle
import com.openauto.dash.SheetButton

/**
 * The map as the driver wants it: touched by hand or not, the compass, its
 * basemap —the theme's, dark or light, and which OpenFreeMap style each one
 * loads— and the traffic over it.
 */
@Composable
internal fun MapSettingsSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    MapOptionsStore.load(context)
    MapServers.load(context)
    var options by remember { mutableStateOf(MapOptionsStore.current.value) }
    // The basemaps are the inherited store's: the map already reads them from there.
    val servers by MapServers.current.collectAsState()
    fun keep(next: MapOptions) {
        options = next
        MapOptionsStore.save(context, next)
    }
    SettingsSheet(
        title = stringResource(R.string.mine_map_title),
        onDismiss = onDismiss,
        actions = {
            // The TomTom key is not this sheet's any more (Expert → Your API keys, #20).
            SheetButton(stringResource(R.string.mine_map_reset), primary = false) { keep(MapOptions()) }
            SheetButton(stringResource(R.string.ai_done), onClick = onDismiss)
        }
    ) {
        SettingsSection(stringResource(R.string.mine_map_widget))
        SettingsToggle(
            Icons.Filled.Swipe,
            stringResource(R.string.mine_map_gestures),
            stringResource(R.string.mine_map_gestures_detail),
            options.gestures
        ) { keep(options.copy(gestures = it)) }
        SettingsToggle(
            Icons.Filled.Explore,
            stringResource(R.string.mine_map_compass),
            stringResource(R.string.mine_map_compass_detail),
            options.compass
        ) { keep(options.copy(compass = it)) }
        ThemeRow(options.theme) { keep(options.copy(theme = it)) }

        SettingsSection(stringResource(R.string.mine_map_styles))
        Text(
            stringResource(R.string.mine_map_styles_detail),
            color = DashColors.TextSecondary,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        Spacer(Modifier.height(8.dp))
        StyleRow(stringResource(R.string.mine_map_style_light), MapStyles.lightChoices, servers.styleLight) {
            MapServers.save(context, servers.copy(styleLight = it.url))
        }
        StyleRow(stringResource(R.string.mine_map_style_dark), MapStyles.darkChoices, servers.styleDark) {
            MapServers.save(context, servers.copy(styleDark = it.url))
        }

        SettingsSection(stringResource(R.string.mine_map_layers))
        SettingsToggle(
            Icons.Filled.Traffic,
            stringResource(R.string.mine_map_traffic),
            stringResource(R.string.mine_map_traffic_detail),
            options.traffic
        ) { keep(options.copy(traffic = it)) }
        SettingsToggle(
            Icons.Filled.AltRoute,
            stringResource(R.string.mine_map_alternatives),
            stringResource(R.string.mine_map_alternatives_detail),
            options.alternatives
        ) { keep(options.copy(alternatives = it)) }
    }
}

/**
 * Which basemap the map is drawn with (#18): the theme's, always dark, always
 * light. One row of three pills, the chosen one in the accent, because three
 * switches cannot say "one of these".
 */
@Composable
private fun ThemeRow(value: MapTheme, onChange: (MapTheme) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp)) {
            Icon(
                Icons.Filled.DarkMode,
                contentDescription = null,
                tint = DashColors.TextSecondary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.mine_map_theme), color = DashColors.TextPrimary, style = MaterialTheme.typography.bodyLarge)
                Text(
                    stringResource(R.string.mine_map_theme_detail),
                    color = DashColors.TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MapTheme.entries.forEach { theme ->
                ThemePill(theme, theme == value, Modifier.weight(1f)) { onChange(theme) }
            }
        }
        Spacer(Modifier.height(10.dp))
    }
    HorizontalDivider(color = DashColors.Line, modifier = Modifier.padding(horizontal = 12.dp))
}

/** One of the three basemaps: filled with the accent when it is the one in use. */
@Composable
private fun ThemePill(theme: MapTheme, chosen: Boolean, modifier: Modifier, onClick: () -> Unit) = Pill(
    label = stringResource(
        when (theme) {
            MapTheme.THEME -> R.string.mine_map_theme_panel
            MapTheme.DARK -> R.string.mine_map_theme_dark
            MapTheme.LIGHT -> R.string.mine_map_theme_light
        }
    ),
    chosen = chosen,
    modifier = modifier,
    onClick = onClick
)

/**
 * One slot's basemaps —the light one or the dark one— with the style in use
 * filled with the accent (#19). A URL kept by hand in the servers sheet is no
 * style of ours, so no pill is marked and the custom one stays.
 */
@Composable
private fun StyleRow(label: String, choices: List<MapStyle>, kept: String, onPick: (MapStyle) -> Unit) {
    Label(label)
    Spacer(Modifier.height(6.dp))
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        choices.forEach { style ->
            Pill(style.name, chosen = style.url == kept.trim(), modifier = Modifier.weight(1f)) { onPick(style) }
        }
    }
    Spacer(Modifier.height(10.dp))
}

/** A pill of a chooser: the accent when it is the one in use, a card otherwise. */
@Composable
private fun Pill(label: String, chosen: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .heightIn(min = DashSize.Bar)
            .clip(DashShape.Medium)
            .background(if (chosen) DashColors.Accent else DashColors.CardHi)
            .border(1.dp, if (chosen) DashColors.Accent else DashColors.Line, DashShape.Medium)
            .clickable(role = Role.RadioButton) { onClick() }
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (chosen) DashColors.OnAccent else DashColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
