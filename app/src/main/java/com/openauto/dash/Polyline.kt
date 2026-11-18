package com.openauto.dash

import com.openauto.dash.link.mine.EncodedPolyline // MINE: #41

/**
 * Google's encoded polyline, as routing services return a route's geometry
 * (precision 5, or 6 for "polyline6" as the in-app navigation asks for).
 *
 * Also what the drive log writes: the GPS fixes of a drive, so the linked
 * phone can draw the route on its own map (#41).
 */
object Polyline {

    /** The (lat, lon) points of [encoded]; a broken string gives the points read up to the break. */
    fun decode(encoded: String, precision: Int = 5): List<Pair<Double, Double>> {
        val factor = Math.pow(10.0, precision.toDouble())
        val out = ArrayList<Pair<Double, Double>>()
        var index = 0
        var lat = 0L
        var lon = 0L
        fun next(): Long? {
            var result = 0L
            var shift = 0
            while (true) {
                if (index >= encoded.length) return null
                val b = encoded[index++].code - 63
                // A corrupt string never ends its number: past 64 bits the shift wraps round and gives garbage points.
                if (b < 0 || shift >= 64) return null
                result = result or ((b and 0x1f).toLong() shl shift)
                shift += 5
                if (b < 0x20) break
            }
            return if (result and 1L != 0L) (result shr 1).inv() else result shr 1
        }
        while (index < encoded.length) {
            lat += next() ?: break
            lon += next() ?: break
            out += (lat / factor) to (lon / factor)
        }
        return out
    }

    /**
     * The points of [points] as Google's encoded polyline, [precision] decimals
     * of a degree (5 for the drive log, 6 for "polyline6"). The first point is
     * the start, so the linked phone draws the route from where it began.
     */
    fun encode(points: List<Pair<Double, Double>>, precision: Int = 5): String = // MINE: #41
        EncodedPolyline.encode(points, precision)
}
