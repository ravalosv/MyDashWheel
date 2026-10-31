package com.openauto.dash.mine

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.maplibre.android.location.modes.CameraMode

/** The «Centrar» button shows while the camera is off the car, and only then (#17). */
class MapRecenteringTest {
    @Test
    fun withoutTrackingTheCameraIsOffTheCar() {
        assertTrue(MapRecentering.awayFromCar(CameraMode.NONE))
    }

    @Test
    fun followingTheCarPutsTheButtonAway() {
        assertFalse(MapRecentering.awayFromCar(CameraMode.TRACKING_GPS))
        assertFalse(MapRecentering.awayFromCar(CameraMode.TRACKING_COMPASS))
    }
}
