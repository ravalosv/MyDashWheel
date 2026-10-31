// MINE: código propio del fork MyDashWheel (#16).
package com.openauto.dash.mine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The traffic of a traced route: TomTom answers with the jams of ITS route, and
 * only the stretches that run along OUR line are painted on it.
 */
class RouteBandsTest {

    /** A straight north-south road, the one the map draws. */
    private val road = listOf(19.400 to -99.200, 19.410 to -99.200)

    @Test
    fun aJamOnOurRoadIsPaintedOnIt() {
        // TomTom's points sit a few metres to the side, as two systems' geometry does.
        val stretch = RouteBands.Stretch(
            listOf(19.401 to -99.19995, 19.402 to -99.19995, 19.403 to -99.19995),
            delay = 2
        )
        val bands = RouteBands.of(road, listOf(stretch))
        assertEquals("one jam, one band", 1, bands.size)
        assertEquals(2, bands[0].delay)
        assertEquals(3, bands[0].points.size)
        bands[0].points.forEach {
            assertEquals("snapped onto our line", -99.200, it.second, 1e-9)
        }
    }

    @Test
    fun aJamOnAnotherStreetIsNotOurs() {
        val elsewhere = RouteBands.Stretch(
            listOf(19.401 to -99.198, 19.402 to -99.198, 19.403 to -99.198),
            delay = 3
        )
        assertTrue(RouteBands.of(road, listOf(elsewhere)).isEmpty())
    }

    @Test
    fun aStretchThatLeavesTheRouteIsCutInTwo() {
        val out = RouteBands.Stretch(
            listOf(
                19.401 to -99.200,
                19.402 to -99.200,
                19.403 to -99.198,   // another road for a moment
                19.404 to -99.200,
                19.405 to -99.200
            ),
            delay = 1
        )
        val bands = RouteBands.of(road, listOf(out))
        assertEquals("the part before and the part after, never across the gap", 2, bands.size)
        assertEquals(2, bands[0].points.size)
        assertEquals(2, bands[1].points.size)
    }

    @Test
    fun aRoadWithNothingKnownIsNotPainted() {
        val unknown = RouteBands.Stretch(listOf(19.401 to -99.200, 19.402 to -99.200), delay = 0)
        assertTrue(RouteBands.of(road, listOf(unknown)).isEmpty())
    }

    @Test
    fun tomTomsSectionsAreReadAcrossTheLegs() {
        // The indices count the whole route, so both legs are one list.
        val body = """
            {"routes":[{
              "legs":[
                {"points":[{"latitude":19.401,"longitude":-99.200},{"latitude":19.402,"longitude":-99.200}]},
                {"points":[{"latitude":19.403,"longitude":-99.200},{"latitude":19.404,"longitude":-99.200}]}
              ],
              "sections":[
                {"sectionType":"TRAFFIC","startPointIndex":0,"endPointIndex":2,"magnitudeOfDelay":3,"simpleCategory":"JAM"},
                {"sectionType":"TOLL","startPointIndex":0,"endPointIndex":3}
              ]
            }]}
        """.trimIndent()
        val route = listOf(19.400 to -99.200, 19.410 to -99.200)
        val bands = RouteBands.parse(body, route)
        assertEquals(1, bands?.size ?: -1)
        assertEquals("only the traffic section, and spanning both legs", 3, bands!![0].points.size)
        assertEquals(3, bands[0].delay)
    }

    @Test
    fun aRefusedKeyIsNoRoute() {
        assertNull(RouteBands.parse("""{"error":{"description":"Unauthorized"}}""", road))
    }
}
