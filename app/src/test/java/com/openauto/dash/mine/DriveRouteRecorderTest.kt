package com.openauto.dash.mine

import com.openauto.dash.link.mine.EncodedPolyline
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DriveRouteRecorderTest {

    @Test
    fun recordsAndTakesOnlyTheRouteForTheRequestedDrive() {
        val recorder = DriveRouteRecorder()
        recorder.record(10L, 48.0 to 2.0, 48.0001 to 2.0)
        recorder.record(20L, 49.0 to 3.0, 49.0001 to 3.0)

        val route = recorder.take(10L)
        assertEquals(2, route.size)
        assertEquals(48.0, route.first().first, 1e-5)
        assertTrue(recorder.take(10L).isEmpty())
        assertEquals(49.0, recorder.take(20L).first().first, 1e-5)
    }

    @Test
    fun restoresTheRouteAfterTheTripIsSaved() {
        val recorder = DriveRouteRecorder()
        recorder.record(10L, 48.0 to 2.0, 48.0001 to 2.0)
        val saved = recorder.encode(10L)
        val restored = DriveRouteRecorder().apply { restore(10L, saved) }

        assertEquals(EncodedPolyline.decode(saved), restored.take(10L))
    }

    @Test
    fun ignoresFixesCloserThanEightMeters() {
        val recorder = DriveRouteRecorder()
        recorder.record(10L, 48.0 to 2.0, 48.00001 to 2.0)

        assertEquals(1, recorder.take(10L).size)
    }
}
