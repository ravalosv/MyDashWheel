// MINE: búsqueda de destinos (#14). Lo que decide qué se le pregunta a
// Nominatim y cómo se ordena y se lee lo que contesta. El código heredado solo
// cambia la llamada de búsqueda y las dos líneas de cada resultado.
package com.openauto.dash.mine

import android.content.Context
import com.openauto.dash.CountryLookup
import com.openauto.dash.Http
import com.openauto.dash.InAppNav
import com.openauto.dash.OVERPASS_AGENT
import com.openauto.dash.Place
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONArray
import java.net.URLEncoder
import java.util.Locale

/**
 * What the driver typed, turned into a question Nominatim can answer well, and
 * into a list a driver can choose from. Three things the plain call did not do:
 *
 * - **the country comes from where the car is** ([CountryLookup]) instead of a
 *   hard-coded one, so a unit abroad is not told "not found" by a filter;
 * - **the answer is asked for in a usable shape** (`addressdetails`, `dedupe`,
 *   more results) and **ranked and deduplicated** here: the nearest branch of a
 *   chain first, one row per real place;
 * - **a house number is respected.** Typed "Insurgentes 1234", a street without
 *   that number is not taken for it: the results that have it win, and when none
 *   has it the driver is shown the others marked ([numberMissing]) instead of
 *   being routed to a wrong address.
 */
internal object PlaceQuery {
    private const val SEARCH_URL = "https://nominatim.openstreetmap.org/search"

    /** How many answers to ask for: enough to choose from, few enough to read on a dashboard. */
    const val LIMIT = 8

    /** Two answers this close to each other are the same place: one row. */
    private const val SAME_PLACE_M = 60.0

    /** The hard box searched first, six or seven minutes of driving each way. */
    private const val NEAR_DEG = 0.15

    /** What was typed: the house number to respect, if any. */
    data class Typed(val number: String?)

    /** A standalone number is a house number unless it is part of the street's own name ("Calle 5 de Mayo"). */
    private val NUMBER = Regex("""\b(\d{1,5})\b""")
    private val NAME_NUMBER = Regex("""^\s*(de|del|de la|de las|de los)\s""", RegexOption.IGNORE_CASE)

    /**
     * The house number in [raw], or null: the first number that is not part of
     * the street's own name. "Insurgentes 1234, piso 2" is 1234, and "Calle 5
     * de Mayo 12" skips the 5 (followed by "de") for the 12.
     */
    fun typed(raw: String): Typed {
        for (match in NUMBER.findAll(raw)) {
            if (NAME_NUMBER.containsMatchIn(raw.substring(match.range.last + 1))) continue
            return Typed(match.groupValues[1])
        }
        return Typed(null)
    }

    /** Whether [displayName] carries [number] as a word of its own (not "1234" inside "12345"). */
    fun hasNumber(displayName: String, number: String): Boolean =
        Regex("""(^|\D)${Regex.escape(number)}(\D|$)""").containsMatchIn(displayName)

    /** Whether the typed number is missing from [displayName]: the row says so instead of pretending. */
    fun numberMissing(raw: String, displayName: String): Boolean {
        val number = typed(raw).number ?: return false
        return !hasNumber(displayName, number)
    }

    /** The name part of an address: what is before the first comma ("Gran Terraza Oblatos"). */
    fun title(displayName: String): String = displayName.substringBefore(',').trim().ifBlank { displayName.trim() }

    /** The locality part ("Av. Circunvalación Oblatos 2700, Oblatos, Jalisco"): the rest, for the row's second line. */
    fun detail(displayName: String): String = displayName.substringAfter(',', "").trim()

    /**
     * The whole question, with the country and the language the answer should
     * come in. [bounded] makes [around] a filter instead of a preference.
     */
    fun url(raw: String, around: String, limit: Int, language: String?, country: String?, bounded: Boolean = false): String {
        val q = URLEncoder.encode(raw.trim(), "UTF-8")
        val code = country?.takeIf { it.length == 2 }?.let { "&countrycodes=$it" } ?: ""
        val lang = language?.takeIf { it.isNotBlank() }?.let { "&accept-language=$it" } ?: ""
        val box = if (bounded && around.startsWith("&viewbox=")) "&bounded=1" else ""
        return "$SEARCH_URL?format=json&addressdetails=1&dedupe=1&limit=$limit&q=$q$around$box$code$lang"
    }

    /**
     * The box around the car, [halfDeg] degrees each way: a soft preference by
     * default, a hard limit when asked for ([url]'s [bounded]).
     */
    fun around(lat: Double, lng: Double, halfDeg: Double = 0.25): String =
        String.format(Locale.US, "&viewbox=%.4f,%.4f,%.4f,%.4f", lng - halfDeg, lat + halfDeg * 0.72, lng + halfDeg, lat - halfDeg * 0.72)

    /**
     * The answers worth showing, best first: the ones that carry the typed house
     * number (when any does), one per place, nearest to the car.
     */
    fun ranked(answers: List<Place>, raw: String, nearLat: Double?, nearLng: Double?): List<Place> {
        val number = typed(raw).number
        val withNumber = if (number == null) answers else answers.filter { hasNumber(it.name, number) }
        // No answer carries the number: the others are still offered, marked in the row.
        val pool = withNumber.ifEmpty { answers }
        val near = nearLat != null && nearLng != null
        val ordered = if (near) pool.sortedBy { it.distanceM(nearLat!!, nearLng!!) } else pool
        val kept = ArrayList<Place>(ordered.size)
        for (place in ordered) if (kept.none { it.sameSpot(place) || it.distanceM(place.lat, place.lng) < SAME_PLACE_M }) kept += place
        return kept
    }

    /**
     * The places found for [raw] around the car: asked here (not through the
     * inherited call) so the country, the language and the answer's shape are
     * ours. Throws when offline, like the call it replaces.
     */
    suspend fun findAll(context: Context, raw: String, nearLat: Double?, nearLng: Double?, limit: Int = LIMIT): List<Place> =
        withContext(Dispatchers.IO) {
            val language = InAppNav.locale(context).language
            if (nearLat == null || nearLng == null) {
                return@withContext ranked(answers(raw, "", limit, language, null), raw, null, null)
            }
            val country = CountryLookup.code(nearLat, nearLng)
            // Around the car first, as a hard limit: "hacienda la palma" must find the street five
            // minutes away, not the village of the same name in the next city. A soft box only
            // reorders, and Nominatim ranked the far one first.
            val near = answers(raw, around(nearLat, nearLng, NEAR_DEG), limit, language, country, bounded = true)
            if (near.isNotEmpty()) return@withContext ranked(near, raw, nearLat, nearLng)
            // Nothing around the car: the whole country, with the car as a preference only.
            ranked(answers(raw, around(nearLat, nearLng), limit, language, country), raw, nearLat, nearLng)
        }

    /** One Nominatim call. */
    private fun answers(raw: String, around: String, limit: Int, language: String?, country: String?, bounded: Boolean = false): List<Place> {
        val request = Request.Builder().url(url(raw, around, limit, language, country, bounded))
            .header("User-Agent", OVERPASS_AGENT).build()
        Http.client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) return emptyList()
            val arr = JSONArray(resp.body?.string() ?: return emptyList())
            return (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                val lat = o.optString("lat").toDoubleOrNull() ?: return@mapNotNull null
                val lon = o.optString("lon").toDoubleOrNull() ?: return@mapNotNull null
                val name = o.optString("display_name").takeIf { it.isNotBlank() } ?: raw
                Place(name, lat, lon)
            }
        }
    }
}
