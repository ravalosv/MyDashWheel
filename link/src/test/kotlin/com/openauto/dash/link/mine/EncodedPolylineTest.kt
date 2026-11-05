package com.openauto.dash.link.mine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EncodedPolylineTest {

    @Test
    fun decodesAndReencodesTheStandardPrecisionFiveExample() {
        val encoded = "_p~iF~ps|U_ulLnnqC_mqNvxq`@"
        val points = EncodedPolyline.decode(encoded)

        assertEquals(3, points.size)
        assertEquals(38.5, points[0].first, 1e-5)
        assertEquals(-120.2, points[0].second, 1e-5)
        assertEquals(encoded, EncodedPolyline.encode(points))
    }

    @Test
    fun supportsPrecisionSixAndKeepsAValidPrefixForTruncatedInput() {
        val encoded = "_p~iF~ps|U_ulLnnqC_mqNvxq`@"
        assertEquals(3.85, EncodedPolyline.decode(encoded, precision = 6)[0].first, 1e-5)
        assertEquals(1, EncodedPolyline.decode("_p~iF~ps|U_ulL").size)
        assertTrue(EncodedPolyline.decode("").isEmpty())
    }

    @Test
    fun emptyPointsEncodeToAnEmptyString() {
        assertEquals("", EncodedPolyline.encode(emptyList()))
    }
}
