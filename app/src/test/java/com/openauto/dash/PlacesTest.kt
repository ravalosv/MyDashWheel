package com.openauto.dash

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Unit tests for the places the driver keeps (Places.kt). */
class PlacesTest {
    private val bakery = Place("Bakery", 1.0, 2.0)
    private val pool = Place("Pool", 3.0, 4.0)
    private val bakeryAgain = Place("Bakery again", 1.0001, 2.0001)

    @Test
    fun filterRecent_removes_one_destination() {
        val places = Places(recent = listOf(bakery, pool)).filterRecent(bakery)
        assertEquals(listOf(pool), places.recent)
    }

    @Test
    fun filterRecent_treats_same_spot_as_the_same_destination() {
        val places = Places(recent = listOf(bakery, pool)).filterRecent(bakeryAgain)
        assertEquals(listOf(pool), places.recent)
    }

    @Test
    fun filterRecent_unknown_destination_leaves_the_list_intact() {
        val places = Places(recent = listOf(bakery, pool)).filterRecent(Place("Nowhere", 9.0, 9.0))
        assertEquals(listOf(bakery, pool), places.recent)
    }

    @Test
    fun filterRecent_does_not_touch_home_or_work() {
        val home = Place("Home", 5.0, 6.0)
        val places = Places(home = home, recent = listOf(bakery, pool)).filterRecent(bakery)
        assertEquals(home, places.home)
        assertEquals(listOf(pool), places.recent)
    }

    @Test
    fun filterRecent_on_empty_list_is_a_noop() {
        val places = Places().filterRecent(bakery)
        assertTrue(places.recent.isEmpty())
    }
}