// MINE: código propio del fork MyDashWheel (#19).
package com.openauto.dash.mine

import com.openauto.dash.MapServers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** The chooser offers what the owner asked for, and recognises the URLs the app ships with (#19). */
class MapStylesTest {

    @Test
    fun theLightSlotOffersPositronBrightAndLiberty() {
        assertEquals(listOf("Positron", "Bright", "Liberty"), MapStyles.lightChoices.map { it.name })
    }

    @Test
    fun theDarkSlotOffersDarkAndFiord() {
        assertEquals(listOf("Dark", "Fiord"), MapStyles.darkChoices.map { it.name })
    }

    @Test
    fun aKeptUrlIsMatchedBackToItsStyle() {
        assertEquals("Fiord", MapStyles.of("https://tiles.openfreemap.org/styles/fiord")?.name)
        assertEquals("Fiord", MapStyles.of("  https://tiles.openfreemap.org/styles/fiord ")?.name)
    }

    @Test
    fun aUrlFromSomewhereElseIsNoStyleOfOurs() {
        assertNull(MapStyles.of("https://example.com/my-style.json"))
        assertNull(MapStyles.of(""))
    }

    @Test
    fun theDefaultsTheAppShipsWithAreInTheLists() {
        assertNotNull("the light default is one the driver can pick", MapStyles.of(MapServers.DEFAULT_STYLE_LIGHT))
        assertNotNull("the dark default is one the driver can pick", MapStyles.of(MapServers.DEFAULT_STYLE_DARK))
    }
}
