// MINE: código propio del fork MyDashWheel (#16).
package com.openauto.dash.mine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MapOptionsTest {

    @Test
    fun theDefaultsLeaveTheMapAsItWas() {
        val d = MapOptions()
        assertTrue("the map is touched by hand as before", d.gestures)
        assertTrue("the compass is drawn", d.compass)
        assertFalse("no tiles from another service until asked", d.traffic)
        assertTrue(d.isDefault)
    }

    @Test
    fun aKeptFileWithTheDefaultsIsStillTheDefault() {
        // The store hands back what it read; the defaults are what an untouched map keeps.
        val read = MapOptions(gestures = true, compass = true, traffic = false)
        assertTrue(read.isDefault)
        assertEquals(MapOptions(), read)
    }

    @Test
    fun switchingOneIsNotTheDefault() {
        assertFalse(MapOptions(gestures = false).isDefault)
        assertFalse(MapOptions(compass = false).isDefault)
        assertFalse(MapOptions(traffic = true).isDefault)
    }

    @Test
    fun theBasemapIsThePanelsUnlessTheDriverClampsIt() {
        assertTrue("the theme's light", MapTheme.THEME.isLight(panelLight = true))
        assertFalse("the theme's dark", MapTheme.THEME.isLight(panelLight = false))
        assertTrue("always light, even with a dark panel", MapTheme.LIGHT.isLight(panelLight = false))
        assertFalse("always dark, even with a light panel", MapTheme.DARK.isLight(panelLight = true))
    }

    @Test
    fun theBasemapIsTheThemesByDefault() {
        assertEquals(MapTheme.THEME, MapOptions().theme)
        assertTrue(MapOptions().isDefault)
        assertFalse(MapOptions(theme = MapTheme.DARK).isDefault)
        assertFalse(MapOptions(theme = MapTheme.LIGHT).isDefault)
    }
}
