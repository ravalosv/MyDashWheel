// MINE: código propio del fork MyDashWheel (#14).
package com.openauto.dash.mine

import com.openauto.dash.Place
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaceQueryTest {

    // --- the house number the driver typed -----------------------------------

    @Test
    fun houseNumberIsFound() {
        assertEquals("1234", PlaceQuery.typed("Av. Insurgentes 1234").number)
        assertEquals("45", PlaceQuery.typed("Juárez 45, Centro").number)
    }

    @Test
    fun numberInTheStreetsNameIsNotTheHouseNumber() {
        // "Calle 5 de Mayo 12": the 5 belongs to the name, the 12 is where the car goes.
        assertEquals("12", PlaceQuery.typed("Calle 5 de Mayo 12").number)
        assertEquals("45", PlaceQuery.typed("2 de Octubre 45").number)
    }

    @Test
    fun floorOrInteriorDoesNotWinOverTheStreetNumber() {
        assertEquals("1234", PlaceQuery.typed("Insurgentes 1234, piso 2").number)
        assertEquals("45", PlaceQuery.typed("Juárez 45 int 3").number)
    }

    @Test
    fun noNumberTyped() {
        assertNull(PlaceQuery.typed("Soriana Oblatos").number)
        assertNull(PlaceQuery.typed("").number)
    }

    @Test
    fun numberMustMatchAsAWhole() {
        assertTrue(PlaceQuery.hasNumber("Insurgentes 1234, CDMX", "1234"))
        assertTrue(PlaceQuery.hasNumber("Av. 1234-A", "1234"))
        // 1234 is not inside 12345: that is another address.
        assertFalse(PlaceQuery.hasNumber("Insurgentes 12345, CDMX", "1234"))
        assertTrue(PlaceQuery.numberMissing("Juárez 45", "Juárez 400"))
        assertFalse(PlaceQuery.numberMissing("Juárez 45", "Juárez 45, Centro"))
        // Nothing typed: nothing to miss.
        assertFalse(PlaceQuery.numberMissing("Juárez", "Juárez 400"))
    }

    // --- how a result reads --------------------------------------------------

    @Test
    fun titleIsTheNameAndDetailIsTheRest() {
        val name = "Gran Terraza Oblatos, Av. Circunvalación Oblatos 2700, Jalisco"
        assertEquals("Gran Terraza Oblatos", PlaceQuery.title(name))
        assertEquals("Av. Circunvalación Oblatos 2700, Jalisco", PlaceQuery.detail(name))
    }

    @Test
    fun addressWithNoCommaStillHasATitle() {
        assertEquals("Juárez 45", PlaceQuery.title("Juárez 45"))
        assertEquals("", PlaceQuery.detail("Juárez 45"))
    }

    // --- the question Nominatim is asked -------------------------------------

    @Test
    fun countryAndLanguageRideAlong() {
        val url = PlaceQuery.url("Juárez 45", "", 8, "es", "MX")
        assertTrue(url, url.contains("countrycodes=MX"))
        assertTrue(url, url.contains("accept-language=es"))
        assertTrue(url, url.contains("addressdetails=1"))
        assertTrue(url, url.contains("dedupe=1"))
        assertTrue(url, url.contains("limit=8"))
    }

    @Test
    fun unknownCountryOrLanguageIsLeftOut() {
        val url = PlaceQuery.url("Juárez 45", "", 8, null, null)
        assertFalse(url, url.contains("countrycodes"))
        assertFalse(url, url.contains("accept-language"))
    }

    @Test
    fun theBoxIsAHardLimitOnlyWhenAsked() {
        val box = PlaceQuery.around(20.66, -103.35, 0.15)
        // First search: only what is around the car ("hacienda la palma" is a street 5 km away,
        // and Nominatim ranked a village of the same name 30 km away first).
        assertTrue(PlaceQuery.url("x", box, 8, "es", "MX", bounded = true).contains("bounded=1"))
        // The fallback keeps it as a preference, so a real place further out is still found.
        assertFalse(PlaceQuery.url("x", box, 8, "es", "MX").contains("bounded=1"))
        assertFalse(PlaceQuery.url("x", "", 8, "es", "MX", bounded = true).contains("bounded=1"))
    }

    @Test
    fun theBoxIsCentredOnTheCar() {
        val box = PlaceQuery.around(20.66, -103.35, 0.15)
        assertTrue(box, box.startsWith("&viewbox=-103.5000,20.7680,-103.2000,20.5520"))
    }

    @Test
    fun accentsAreEncodedAndNotSentRaw() {
        val url = PlaceQuery.url("José María", "", 8, "es", "MX")
        assertTrue(url, url.contains("Jos%C3%A9+Mar%C3%ADa"))
    }

    // --- the order answers are shown in --------------------------------------

    private fun place(name: String, lat: Double, lng: Double = -103.35) = Place(name, lat, lng)

    @Test
    fun nearestBranchComesFirst() {
        val far = place("Soriana Chapalita", 20.7000)
        val near = place("Soriana Oblatos", 20.6600)
        val ranked = PlaceQuery.ranked(listOf(far, near), "Soriana", 20.6600, -103.35)
        assertEquals("Soriana Oblatos", ranked[0].name)
        assertEquals(2, ranked.size)
    }

    @Test
    fun theSamePlaceIsListedOnce() {
        // The same supermarket mapped twice, 30 m apart.
        val a = place("Soriana Oblatos", 20.66000)
        val b = place("Soriana Oblatos", 20.66027)
        val ranked = PlaceQuery.ranked(listOf(a, b), "Soriana", 20.6600, -103.35)
        assertEquals(1, ranked.size)
    }

    @Test
    fun answersWithTheTypedNumberWin() {
        val without = place("Av. Juárez", 20.6600)
        val with = place("Av. Juárez 45, Centro", 20.8000)
        val ranked = PlaceQuery.ranked(listOf(without, with), "Juárez 45", 20.6600, -103.35)
        assertEquals(1, ranked.size)
        assertEquals("Av. Juárez 45, Centro", ranked[0].name)
    }

    @Test
    fun whenNoAnswerHasTheNumberTheyAreAllKeptToChooseFrom() {
        val a = place("Av. Juárez 400", 20.6600)
        val b = place("Av. Juárez 500", 20.6800)
        val ranked = PlaceQuery.ranked(listOf(a, b), "Juárez 45", 20.6600, -103.35)
        assertEquals(2, ranked.size)
    }

    @Test
    fun noFixStillGivesTheAnswersBack() {
        val a = place("Soriana Oblatos", 20.6600)
        val b = place("Soriana Chapalita", 20.7000)
        assertEquals(2, PlaceQuery.ranked(listOf(a, b), "Soriana", null, null).size)
    }
}
