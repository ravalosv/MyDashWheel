package com.openauto.dash.link.mine

/** Google's encoded polyline format used by navigation routes and drive history. */
object EncodedPolyline {

    /** Decodes latitude/longitude pairs; a malformed suffix leaves the valid prefix intact. */
    fun decode(encoded: String, precision: Int = 5): List<Pair<Double, Double>> {
        require(precision in 0..10)
        val factor = Math.pow(10.0, precision.toDouble())
        val points = ArrayList<Pair<Double, Double>>()
        var index = 0
        var latitude = 0L
        var longitude = 0L

        fun next(): Long? {
            var result = 0L
            var shift = 0
            while (true) {
                if (index >= encoded.length || shift >= 60) return null
                val value = encoded[index++].code - 63
                if (value !in 0..63) return null
                result = result or ((value and 0x1f).toLong() shl shift)
                shift += 5
                if (value < 0x20) break
            }
            return if (result and 1L != 0L) (result shr 1).inv() else result shr 1
        }

        while (index < encoded.length) {
            val latDelta = next() ?: break
            val lonDelta = next() ?: break
            latitude += latDelta
            longitude += lonDelta
            points += latitude / factor to longitude / factor
        }
        return points
    }

    /** Encodes latitude/longitude pairs with [precision] decimal places. */
    fun encode(points: List<Pair<Double, Double>>, precision: Int = 5): String {
        require(precision in 0..10)
        if (points.isEmpty()) return ""
        val factor = Math.pow(10.0, precision.toDouble())
        var latitude = 0L
        var longitude = 0L
        return buildString {
            for ((lat, lon) in points) {
                val nextLatitude = Math.round(lat * factor)
                val nextLongitude = Math.round(lon * factor)
                append(encodeDelta(nextLatitude - latitude))
                append(encodeDelta(nextLongitude - longitude))
                latitude = nextLatitude
                longitude = nextLongitude
            }
        }
    }

    private fun encodeDelta(delta: Long): String {
        var value = if (delta < 0) (delta.inv() shl 1) + 1 else delta shl 1
        return buildString {
            while (value >= 0x20) {
                append(((0x20 or (value and 0x1f).toInt()) + 63).toChar())
                value = value shr 5
            }
            append((value + 63).toInt().toChar())
        }
    }
}
