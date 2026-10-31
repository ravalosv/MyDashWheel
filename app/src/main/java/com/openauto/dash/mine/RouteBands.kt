// MINE: el tráfico que se pinta sobre la ruta trazada (#16). TomTom devuelve
// los tramos congestionados de SU ruta; aquí se pegan a la nuestra —solo lo que
// corre junto a ella— para que el color caiga sobre la línea que el mapa dibuja
// y no sobre las calles de al lado, que es lo que se ve en Google Maps.
package com.openauto.dash.mine

import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.cos
import kotlin.math.hypot

/**
 * One congested stretch lying on our own line: (lat, lon) points and TomTom's
 * delay step, 1 minor … 4 blocked.
 */
internal data class RouteBand(val points: List<Pair<Double, Double>>, val delay: Int)

internal object RouteBands {
    /**
     * How far TomTom's stretch may run from our line and still be painted on
     * it: another street's jam is not the jam of the road we are taking.
     */
    const val MATCH_M = 40.0

    /** A stretch of TomTom's route: its own (lat, lon) points and the step that says how bad it is. */
    data class Stretch(val points: List<Pair<Double, Double>>, val delay: Int)

    /**
     * [stretches] as bands on [route]: every point snapped onto our line, and a
     * stretch that leaves it cut in two, so no colour is drawn across a road we
     * never take. A stretch anywhere else leaves nothing.
     */
    fun of(route: List<Pair<Double, Double>>, stretches: List<Stretch>): List<RouteBand> {
        if (route.size < 2) return emptyList()
        val snap = Snap(route)
        val out = ArrayList<RouteBand>()
        for (stretch in stretches) {
            // 0 is "unknown", not "clear": nothing is painted for a guess.
            if (stretch.delay < 1) continue
            val delay = stretch.delay.coerceIn(1, 4)
            var run = ArrayList<Pair<Double, Double>>()
            fun flush() {
                if (run.size >= 2) out += RouteBand(run, delay)
                run = ArrayList()
            }
            for (p in stretch.points) {
                val on = snap.on(p.first, p.second, MATCH_M)
                if (on == null) flush() else run += on
            }
            flush()
        }
        return out
    }

    /**
     * TomTom's traffic route as bands on [route]; null when the body is not a
     * route at all (an error, or the key was refused).
     */
    fun parse(body: String, route: List<Pair<Double, Double>>): List<RouteBand>? {
        val json = runCatching { JSONObject(body) }.getOrNull() ?: return null
        val first = json.optJSONArray("routes")?.optJSONObject(0) ?: return null
        // The sections count points across the whole route, so the legs are read as one list.
        val points = ArrayList<Pair<Double, Double>>()
        val legs = first.optJSONArray("legs") ?: JSONArray()
        for (i in 0 until legs.length()) {
            val leg = legs.optJSONObject(i)?.optJSONArray("points") ?: continue
            for (j in 0 until leg.length()) {
                val p = leg.optJSONObject(j) ?: continue
                points += p.optDouble("latitude") to p.optDouble("longitude")
            }
        }
        val stretches = ArrayList<Stretch>()
        val sections = first.optJSONArray("sections") ?: JSONArray()
        for (i in 0 until sections.length()) {
            val s = sections.optJSONObject(i) ?: continue
            if (!s.optString("sectionType").equals("TRAFFIC", ignoreCase = true)) continue
            val start = s.optInt("startPointIndex", -1).coerceAtLeast(0)
            val end = s.optInt("endPointIndex", -1).coerceAtMost(points.size - 1)
            if (end <= start) continue
            stretches += Stretch(points.subList(start, end + 1).toList(), s.optInt("magnitudeOfDelay", 0))
        }
        return of(route, stretches)
    }

    /** Where a point falls on a line: the closest point of it, if it is within [withinM]. */
    class Snap(route: List<Pair<Double, Double>>) {
        private val kLat = 111_320.0
        private val kLon = 111_320.0 * cos(Math.toRadians(route.first().first))
        private val xs = DoubleArray(route.size) { route[it].second * kLon }
        private val ys = DoubleArray(route.size) { route[it].first * kLat }

        fun on(lat: Double, lon: Double, withinM: Double): Pair<Double, Double>? {
            val px = lon * kLon
            val py = lat * kLat
            var best = Double.MAX_VALUE
            var bx = 0.0
            var by = 0.0
            for (i in 1 until xs.size) {
                val ax = xs[i - 1]
                val ay = ys[i - 1]
                val dx = xs[i] - ax
                val dy = ys[i] - ay
                val len2 = dx * dx + dy * dy
                val t = if (len2 == 0.0) 0.0 else (((px - ax) * dx + (py - ay) * dy) / len2).coerceIn(0.0, 1.0)
                val cx = ax + t * dx
                val cy = ay + t * dy
                val off = hypot(px - cx, py - cy)
                if (off < best) {
                    best = off
                    bx = cx
                    by = cy
                }
            }
            return if (best <= withinM) (by / kLat) to (bx / kLon) else null
        }
    }
}
