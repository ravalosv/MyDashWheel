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
    fun decode(encoded: String, precision: Int = 5): List<Pair<Double, Double>> = // MINE: #41
        EncodedPolyline.decode(encoded, precision)

    /**
     * The points of [points] as Google's encoded polyline, [precision] decimals
     * of a degree (5 for the drive log, 6 for "polyline6"). The first point is
     * the start, so the linked phone draws the route from where it began.
     */
    fun encode(points: List<Pair<Double, Double>>, precision: Int = 5): String = // MINE: #41
        EncodedPolyline.encode(points, precision)
}
