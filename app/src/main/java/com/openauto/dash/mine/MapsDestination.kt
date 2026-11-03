// MINE: el destino que Google Maps solo enseña en la tarjeta de un lugar (#15).
// El upstream lo busca en la vista previa de la ruta ("Tu ubicación" y el
// nombre debajo), que esta versión de Maps en español no expone a la
// accesibilidad. La tarjeta del lugar sí: su descripción es "Cómo llegar a
// <nombre>, <dirección>". Se lee ahí y se entrega cuando la guía arranca de
// verdad, no cuando el conductor solo estaba mirando el sitio.
package com.openauto.dash.mine

import android.util.Log
import android.os.SystemClock
import com.openauto.dash.NavHandoff
import com.openauto.dash.WazeScreen
import java.util.Locale

internal object MapsDestination {
    private const val TAG = "MapsDestination"
    private val card = MapsCard()
    private var guidedFromCard: String? = null

    fun onNavigationNotification(packageName: String) {
        if (packageName == NavHandoff.MAPS) {
            watch(emptyList(), mapsGuiding = true, nowMs = SystemClock.elapsedRealtime())
        }
    }

    /**
     * Reads Maps' screen for the place card and hands the destination over once
     * Maps guides ([mapsGuiding]). A place card's address is more precise than
     * the name-only route preview and remains authoritative while it guides.
     */
    fun watch(pieces: List<WazeScreen.Piece>, mapsGuiding: Boolean, nowMs: Long): Boolean {
        if (!mapsGuiding) guidedFromCard = null
        val cardVisible = MapsCard.from(pieces) != null
        val destination = card.next(pieces, mapsGuiding, nowMs) ?: return cardVisible || guidedFromCard != null
        Log.i(TAG, "place card: Maps is going to \"$destination\"")
        NavHandoff.noticed(destination)
        guidedFromCard = destination
        return true
    }
}

/**
 * The card's destination, kept while Maps goes from looking at a place to
 * guiding to it. Pure but for its little memory, so the rules can be unit
 * tested without a screen.
 */
internal class MapsCard(private val ttlMs: Long = DEFAULT_TTL_MS) {
    private var pending: String? = null
    private var pendingAt = 0L
    private var lastSent: String? = null

    /**
     * What to hand over now, or null: the card's destination when Maps guides
     * and it was read a moment ago, once per destination.
     */
    fun next(pieces: List<WazeScreen.Piece>, mapsGuiding: Boolean, nowMs: Long): String? {
        from(pieces)?.let { pending = it; pendingAt = nowMs }
        if (!mapsGuiding) return null
        val waiting = pending ?: return null
        if (nowMs - pendingAt > ttlMs) {
            pending = null
            return null
        }
        pending = null
        if (waiting == lastSent) return null
        lastSent = waiting
        return waiting
    }

    companion object {
        /** How long a card read is still the place Maps is about to guide to. */
        const val DEFAULT_TTL_MS = 120_000L

        /** The card's description in each language Maps writes it, up to the place's name. */
        private val PREFIXES = listOf(
            "cómo llegar a ", "como llegar a ",
            "directions to ", "get directions to ",
            "itinéraire vers ", "itinéraire jusqu'à ",
            "route nach ", "route zu ",
            "come arrivare a ", "indicazioni per ",
            "como chegar a ", "rotas para ",
            "routebeschrijving naar ",
            "trasa do ",
            "маршрут до "
        )

        /**
         * "Gran Terraza Oblatos, Av. Circunvalación Oblatos 2700" from a card
         * that says "Cómo llegar a Gran Terraza Oblatos, Av. Circunvalación
         * Oblatos 2700". Null when the screen has no card: a step ("Dirígete
         * hacia…"), a button ("Iniciar") or anything else.
         */
        fun from(pieces: List<WazeScreen.Piece>): String? {
            for (piece in pieces) {
                val text = piece.text.trim()
                val lower = text.lowercase(Locale.ROOT)
                val prefix = PREFIXES.firstOrNull { lower.startsWith(it) } ?: continue
                val destination = deduplicateParts(text.substring(prefix.length).trim())
                if (destination.length >= 4 && destination.any { it.isLetter() }) return destination
            }
            return null
        }

        private fun deduplicateParts(destination: String): String {
            val seen = HashSet<String>()
            return destination.split(',')
                .map(String::trim)
                .filter { part ->
                    val key = part.lowercase(Locale.ROOT).replace(Regex("\\s+"), " ")
                    part.isNotEmpty() && seen.add(key)
                }
                .joinToString(", ")
        }
    }
}
