package com.openauto.dash

import org.junit.Assert.assertEquals
import org.junit.Test

/** How a refused Valhalla answer is turned into the map's error line. */
class ValhallaFailureTest {

    @Test
    fun osrmStyleMessageIsKeptWithTheStatus() {
        assertEquals(
            "Path distance exceeds the max distance limit. (HTTP 400)",
            valhallaFailure(400, """{"code":"DistanceExceeded","message":"Path distance exceeds the max distance limit."}""")
        )
    }

    @Test
    fun nativeStyleErrorIsKeptWithTheStatus() {
        assertEquals(
            "Failed to parse json request (HTTP 400)",
            valhallaFailure(400, """{"error_code":100,"error":"Failed to parse json request","status_code":400}""")
        )
    }

    @Test
    fun blankOrUnreadableBodiesFallBackToTheStatusAlone() {
        assertEquals("HTTP 503", valhallaFailure(503, ""))
        assertEquals("HTTP 400", valhallaFailure(400, "not-json"))
    }
}
