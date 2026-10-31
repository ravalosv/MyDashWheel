// MINE: opciones del mapa que el conductor ajusta desde Ajustes (#16).
// Lo heredado dibuja el mapa con todo fijo; esto guarda lo elegido y lo deja
// leer a [MapLook], que es el único que lo aplica.
package com.openauto.dash.mine

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Which basemap the map is drawn with: forced dark or light, or whatever the panel's theme says (#18). */
internal enum class MapTheme {
    THEME, DARK, LIGHT;

    /** Whether the light basemap is the one to draw, [panelLight] being the panel's theme. */
    fun isLight(panelLight: Boolean): Boolean = when (this) {
        LIGHT -> true
        DARK -> false
        THEME -> panelLight
    }
}

internal data class MapOptions(
    /** Touches on the map: drag, pinch and turn it by hand (off, the camera keeps the car). */
    val gestures: Boolean = true,
    /** The compass over the map, turning with it. */
    val compass: Boolean = true,
    /** TomTom's traffic on the map: the flow's colours and the incidents ([MapTraffic]). */
    val traffic: Boolean = false,
    /** The basemap's own light or dark, or the theme's (the default, which is what the map did before). */
    val theme: MapTheme = MapTheme.THEME,
    /** More than one route to the destination, the driver picking one (#25). */
    val alternatives: Boolean = true
) {
    val isDefault: Boolean get() = this == MapOptions()
}

/** [MapOptions] as kept between runs, and the flow a map watches. */
internal object MapOptionsStore {
    private const val PREFS = "map_options"
    private const val KEY_GESTURES = "gestures"
    private const val KEY_COMPASS = "compass"
    private const val KEY_TRAFFIC = "traffic"
    private const val KEY_THEME = "theme"
    private const val KEY_ALTERNATIVES = "alternatives"

    private val _current = MutableStateFlow(MapOptions())
    val current: StateFlow<MapOptions> = _current
    private var loaded = false

    @Synchronized
    fun load(context: Context) {
        if (loaded) return
        loaded = true
        val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val d = MapOptions()
        // A name that is no longer ours (a renamed enum, a file edited by hand) falls back to the default.
        val theme = MapTheme.entries.firstOrNull { it.name == p.getString(KEY_THEME, d.theme.name) } ?: d.theme
        _current.value = MapOptions(
            gestures = p.getBoolean(KEY_GESTURES, d.gestures),
            compass = p.getBoolean(KEY_COMPASS, d.compass),
            traffic = p.getBoolean(KEY_TRAFFIC, d.traffic),
            theme = theme,
            alternatives = p.getBoolean(KEY_ALTERNATIVES, d.alternatives)
        )
    }

    fun save(context: Context, options: MapOptions) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_GESTURES, options.gestures)
            .putBoolean(KEY_COMPASS, options.compass)
            .putBoolean(KEY_TRAFFIC, options.traffic)
            .putString(KEY_THEME, options.theme.name)
            .putBoolean(KEY_ALTERNATIVES, options.alternatives)
            .apply()
        _current.value = options
    }
}
