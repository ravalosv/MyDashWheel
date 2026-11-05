package com.openauto.dash.mine

import com.openauto.dash.link.mine.EncodedPolyline
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

internal class DriveRouteRecorder {
    private val routes = mutableMapOf<Long, MutableList<Pair<Double, Double>>>()

    fun record(startedAt: Long, previous: Pair<Double, Double>, current: Pair<Double, Double>) {
        val points = routes.getOrPut(startedAt) { mutableListOf() }
        if (points.isEmpty()) points += previous
        if (distanceMeters(points.last(), current) >= MIN_POINT_DISTANCE_METERS) points += current
        if (points.size > MAX_POINTS) {
            routes[startedAt] = points.filterIndexed { index, _ -> index % 2 == 0 || index == points.lastIndex }.toMutableList()
        }
    }

    fun encode(startedAt: Long): String =
        routes[startedAt]?.let(EncodedPolyline::encode).orEmpty()

    fun restore(startedAt: Long, encoded: String?) {
        val points = encoded?.takeIf(String::isNotBlank)?.let(EncodedPolyline::decode).orEmpty()
        if (points.isEmpty()) routes.remove(startedAt) else routes[startedAt] = points.toMutableList()
    }

    fun take(startedAt: Long): List<Pair<Double, Double>> = routes.remove(startedAt).orEmpty()

    private fun distanceMeters(a: Pair<Double, Double>, b: Pair<Double, Double>): Double {
        val lat1 = Math.toRadians(a.first)
        val lat2 = Math.toRadians(b.first)
        val dLat = lat2 - lat1
        val dLon = Math.toRadians(b.second - a.second)
        val haversine = sin(dLat / 2) * sin(dLat / 2) +
            cos(lat1) * cos(lat2) * sin(dLon / 2) * sin(dLon / 2)
        return 2 * EARTH_RADIUS_METERS * atan2(sqrt(haversine), sqrt(1 - haversine))
    }

    private companion object {
        const val MIN_POINT_DISTANCE_METERS = 8.0
        const val MAX_POINTS = 10_000
        const val EARTH_RADIUS_METERS = 6_371_000.0
    }
}
