// MINE: código propio del fork MyDashWheel (#15).
package com.openauto.dash.mine

import com.openauto.dash.WazeScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MapsCardTest {

    private fun piece(text: String, top: Int = 600) = WazeScreen.Piece(text, top, top + 40, 0)

    /** The card as this Maps writes it (logcat, 2026-10-30 12:08:07). */
    private val card = listOf(
        piece("Gran Terraza Oblatos", 663),
        piece("Cómo llegar a Gran Terraza Oblatos, Av. Circunvalación Oblatos 2700", 663),
        piece("Iniciar", 700)
    )

    private fun cardOf(vararg text: String) = text.map { piece(it) }

    // --- reading the card ----------------------------------------------------

    @Test
    fun theDestinationIsTheWholeCardWithoutItsPrefix() {
        assertEquals(
            "Gran Terraza Oblatos, Av. Circunvalación Oblatos 2700",
            MapsCard.from(card)
        )
    }

    @Test
    fun englishCardsReadToo() {
        assertEquals(
            "Tour Eiffel, 5 Av. Anatole France",
            MapsCard.from(cardOf("Directions to Tour Eiffel, 5 Av. Anatole France"))
        )
    }

    @Test
    fun stepsAndButtonsAreNotADestination() {
        // The route preview: a step, a button, the header. Nothing to read.
        assertNull(
            MapsCard.from(
                cardOf(
                    "Dirígete hacia Calle Hacienda de Tala.",
                    "Vista previa de ruta",
                    "Iniciar",
                    "Descartar"
                )
            )
        )
        // And a bare prefix with nothing after it is not a place either.
        assertNull(MapsCard.from(cardOf("Cómo llegar a ")))
        assertNull(MapsCard.from(emptyList()))
    }

    // --- when it is handed over ----------------------------------------------

    @Test
    fun lookingAtACardHandsNothingOver() {
        // The driver opened the place's card and is reading it: Maps is not guiding.
        assertNull(MapsCard().next(card, mapsGuiding = false, nowMs = 1_000))
    }

    @Test
    fun theCardIsHandedOverWhenGuidanceStarts() {
        val maps = MapsCard()
        // Card on screen, not guiding yet.
        assertNull(maps.next(card, mapsGuiding = false, nowMs = 1_000))
        // "Iniciar" pressed: Maps guides, and the card is gone from the screen.
        assertEquals(
            "Gran Terraza Oblatos, Av. Circunvalación Oblatos 2700",
            maps.next(cardOf("Gira a la izquierda"), mapsGuiding = true, nowMs = 3_000)
        )
    }

    @Test
    fun aCardTooOldToBeWhatMapsGuidesToIsDropped() {
        val maps = MapsCard()
        assertNull(maps.next(card, mapsGuiding = false, nowMs = 0))
        assertNull(maps.next(cardOf("Gira a la derecha"), mapsGuiding = true, nowMs = MapsCard.DEFAULT_TTL_MS + 1))
    }

    @Test
    fun theSameDestinationIsHandedOverOnce() {
        val maps = MapsCard()
        maps.next(card, mapsGuiding = false, nowMs = 1_000)
        assertEquals("Gran Terraza Oblatos, Av. Circunvalación Oblatos 2700", maps.next(card, mapsGuiding = true, nowMs = 2_000))
        // Another read of the same card must not hand it over again.
        maps.next(card, mapsGuiding = false, nowMs = 3_000)
        assertNull(maps.next(card, mapsGuiding = true, nowMs = 4_000))
    }
}
