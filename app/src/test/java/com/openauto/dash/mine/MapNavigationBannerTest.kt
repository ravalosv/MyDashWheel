package com.openauto.dash.mine

import com.openauto.dash.NavDirections
import com.openauto.dash.NavState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MapNavigationBannerTest {
    @Test
    fun hidesDestinationControlsForActiveExternalNavigationBeforeRouteIsDrawn() {
        for (packageName in NavDirections.PACKAGES) {
            assertTrue(mapHasExternalNavigation(NavState(active = true, packageName = packageName), false))
        }
    }

    @Test
    fun keepsDestinationControlsHiddenWhenDirectionsDisappearButRouteRemains() {
        assertTrue(mapHasExternalNavigation(NavState(), true))
    }

    @Test
    fun restoresDestinationControlsWhenExternalNavigationAndItsRouteEnd() {
        assertFalse(mapHasExternalNavigation(NavState(), false))
        assertFalse(mapHasExternalNavigation(NavState(packageName = "com.google.android.apps.maps"), false))
        assertFalse(mapHasExternalNavigation(NavState(active = true, packageName = "other.navigation.app"), false))
    }

    @Test
    fun showsActiveMapsAndWazeDirections() {
        for (packageName in NavDirections.PACKAGES) {
            val state = NavState(active = true, packageName = packageName)
            assertEquals(
                state,
                mapBannerNavigation(state, false, null, null, "Continue")
            )
        }
    }

    @Test
    fun hidesInactiveUnknownAndInAppGuidance() {
        assertNull(mapBannerNavigation(NavState(packageName = "com.google.android.apps.maps"), false, null, null, "Continue"))
        assertNull(mapBannerNavigation(NavState(active = true, packageName = "other.navigation.app"), false, null, null, "Continue"))
        assertNull(mapBannerNavigation(NavState(active = true, packageName = "com.openauto.dash"), false, null, null, "Continue"))
    }

    @Test
    fun keepsCancelAvailableWhileExternalRouteIsDrawnWithoutReadableDirections() {
        val fallback = mapBannerNavigation(
            state = NavState(),
            hasHandedRoute = true,
            runningApp = "com.waze",
            preferredApp = "com.google.android.apps.maps",
            continueLabel = "Continue"
        )
        assertEquals(true, fallback?.active)
        assertEquals("com.waze", fallback?.packageName)
        assertEquals("Continue", fallback?.instruction)
    }

    @Test
    fun drawnRouteUsesPreferredNavigationAppWhenItsRunningNotificationIsMissing() {
        val fallback = mapBannerNavigation(
            state = NavState(),
            hasHandedRoute = true,
            runningApp = null,
            preferredApp = "com.waze",
            continueLabel = "Continue"
        )
        assertEquals("com.waze", fallback?.packageName)
    }
}
