// MINE: volver al coche a mano (#17). Antes el mapa se recentraba solo a los
// diez segundos de moverlo; en el coche eso es peor que dejarlo quieto, porque
// el conductor mueve el mapa justo cuando quiere mirar otra cosa. Ahora la
// cámara se queda donde la dejó y el botón «Centrar» ([MapCenterButton]) la
// devuelve al coche con su zoom de conducción.
package com.openauto.dash.mine

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.maplibre.android.location.LocationComponent
import org.maplibre.android.location.OnCameraTrackingChangedListener
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.maps.MapLibreMap

internal object MapRecentering {
    /** Per map: whether the camera is off the car, i.e. whether the button is shown. */
    private val away = java.util.WeakHashMap<MapLibreMap, MutableStateFlow<Boolean>>()

    /** Per map: what takes the camera back, as the map's own call site knows how to do it. */
    private val centres = java.util.WeakHashMap<MapLibreMap, () -> Unit>()

    /** Per map: the component the map follows the car with, so a camera of ours can stop it. */
    private val components = java.util.WeakHashMap<MapLibreMap, LocationComponent>()

    /** No map, no car: nothing to come back to. */
    private val none = MutableStateFlow(false)

    /**
     * Watches the camera tracking of [lc] from now on; [centre] takes the
     * camera back to the car. The component reports both ways —the dismissal
     * and the mode it lands in— so both callbacks write the same state.
     */
    fun watch(map: MapLibreMap, lc: LocationComponent, centre: () -> Unit) {
        synchronized(this) {
            state(map).value = false
            centres[map] = centre
            components[map] = lc
        }
        lc.addOnCameraTrackingChangedListener(object : OnCameraTrackingChangedListener {
            override fun onCameraTrackingDismissed() {
                state(map).value = true
            }

            override fun onCameraTrackingChanged(currentMode: Int) {
                state(map).value = awayFromCar(currentMode)
            }
        })
    }

    /**
     * Stops the camera from following the car, so a camera of ours stays where
     * it is put: the whole route (#23) would otherwise snap back to the car on
     * the next fix, or before the animation even ends.
     */
    fun stopFollowing(map: MapLibreMap?) {
        if (map == null) return
        val lc = synchronized(this) { components[map] } ?: return
        runCatching { lc.cameraMode = CameraMode.NONE }
    }

    /** Whether the button for [map] is to be shown, as the camera moves on and off the car. */
    fun away(map: MapLibreMap?): StateFlow<Boolean> = if (map == null) none else state(map)

    /** Takes the camera back to the car at the driving zoom, as the map itself knows how. */
    fun centre(map: MapLibreMap?) {
        if (map == null) return
        val back = synchronized(this) { centres[map] } ?: return
        back()
    }

    private fun state(map: MapLibreMap): MutableStateFlow<Boolean> =
        synchronized(this) { away.getOrPut(map) { MutableStateFlow(false) } }

    /**
     * The camera is off the car unless a tracking mode is on: the component
     * leaves tracking on any gesture and on any camera move of ours (a route
     * preview, a search hit), and [CameraMode.NONE] is what is left behind.
     */
    internal fun awayFromCar(mode: Int): Boolean = mode == CameraMode.NONE
}
