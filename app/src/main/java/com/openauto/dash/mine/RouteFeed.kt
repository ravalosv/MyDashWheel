// MINE: la ruta que el mapa está dibujando, para el tráfico (#16). Tres
// dueños posibles de la línea: la guía dentro de la app, la ruta que el mapa
// enseña antes de guiar, y la que se sigue por detrás cuando el destino se
// entregó a Maps o Waze ([ShadowRoute]). Aquí se publica una sola, la que se ve.
package com.openauto.dash.mine

import com.openauto.dash.InAppNav
import com.openauto.dash.Polyline
import com.openauto.dash.ShadowRoute
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.maplibre.navigation.core.models.DirectionsRoute

/** A route as (lat, lon) points, with [key] telling one route from the next. */
internal class RouteLine(val key: String, val points: List<Pair<Double, Double>>)

internal object RouteFeed {
    private val _route = MutableStateFlow<RouteLine?>(null)

    /** The line the map is drawing; null when there is none. */
    val route: StateFlow<RouteLine?> = _route

    private val previewed = MutableStateFlow<RouteLine?>(null)
    private val handed = MutableStateFlow<RouteLine?>(null)
    /** The line currently drawn for a destination handed to Google Maps or Waze. */
    val handedRoute: StateFlow<RouteLine?> = handed
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var started = false

    /** Watches the owners from now on; call once. */
    @Synchronized
    fun start() {
        if (started) return
        started = true
        scope.launch {
            combine(InAppNav.guidance, handed, previewed, ShadowRoute.route) { guidance, given, preview, shadow ->
                pick(guidance, given, preview, shadow)
            }
                .distinctUntilChanged { old, new -> old?.key == new?.key }
                .collect {
                    _route.value = it
                }
        }
    }

    /**
     * Whose line the map shows: the guidance's, else the one drawn to a
     * destination handed to Maps or Waze, else the preview's, else the
     * shadow's. In that order, because a handed-off trip replaces the preview.
     */
    internal fun pick(
        guidance: InAppNav.Guidance?,
        handed: RouteLine?,
        preview: RouteLine?,
        shadow: List<Pair<Double, Double>>
    ): RouteLine? = of(guidance?.route) ?: handed ?: preview ?: of(shadow)

    /** [route] as (lat, lon) points, its own encoded geometry as the key. */
    fun of(route: DirectionsRoute?): RouteLine? = route?.geometry?.let { of(it) }

    /** The last line decoded: the guidance republishes itself with every fix, its geometry does not change. */
    @Volatile
    private var decoded: RouteLine? = null

    fun of(geometry: String): RouteLine? {
        decoded?.let { if (it.key == geometry) return it }
        val points = Polyline.decode(geometry, 6)
        if (points.size < 2) return null
        return RouteLine(geometry, points).also { decoded = it }
    }

    fun of(points: List<Pair<Double, Double>>): RouteLine? {
        if (points.size < 2) return null
        return RouteLine("${points.size}:${points.first()}:${points.last()}", points)
    }

    /** The route the map is previewing: traced, not yet guided. Called by the map when it draws one. */
    fun preview(route: DirectionsRoute?) {
        previewed.value = of(route)
    }

    /** The preview is gone (the map cleared its line). */
    fun clearPreview() {
        previewed.value = null
    }

    /** The line drawn to a destination handed to Maps or Waze ([HandedOffRoute]). */
    fun handed(route: DirectionsRoute?) {
        handed.value = of(route)
    }

    fun clearHanded() {
        handed.value = null
    }
}
