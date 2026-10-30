// MINE: código propio del fork MyDashwheel. Vive en su propio paquete para que
// los merges del upstream no lo pisen ni lo mezclen.
package com.openauto.dash.mine

import android.content.Context
import com.openauto.dash.LocationFeed
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import org.maplibre.navigation.core.location.Location
import org.maplibre.navigation.core.location.engine.LocationEngine
import org.maplibre.navigation.core.location.toLocation

/**
 * MapLibre Navigation's fixes, taken from Dashwheel's own GPS feed.
 *
 * The library's engine registers an `android.location.LocationListener` that was
 * compiled against API 30+, where `onProviderDisabled` became a default method.
 * On an Android 10 unit the method is still abstract, so the platform's
 * `ListenerTransport` dispatches it and the process dies with
 * `AbstractMethodError` the moment a provider goes away (ticket #12).
 *
 * [LocationFeed]'s listener overrides every callback, and the map already reads
 * from it, so the guidance takes the same fixes instead of opening a second GPS
 * subscription that competes with the dashboard's.
 */
internal class FeedLocationEngine(private val app: Context) : LocationEngine {

    override suspend fun getLastLocation(): Location? = LocationFeed.location.value?.toLocation()

    override fun listenToLocation(request: LocationEngine.Request): Flow<Location> = flow {
        if (!LocationFeed.acquire(app)) return@flow
        try {
            LocationFeed.location.filterNotNull().map { it.toLocation() }.collect { emit(it) }
        } finally {
            LocationFeed.release()
        }
    }
}
