// MINE: punto único donde el fork MyDashwheel sustituye un comportamiento del
// upstream. Todo lo que se enganche al código heredado pasa por aquí.
package com.openauto.dash.mine

import android.content.Context
import android.os.Build
import android.os.Looper
import org.maplibre.navigation.core.location.engine.LocationEngine
import org.maplibre.navigation.core.location.engine.MapLibreLocationEngine

object MineOverrides {

    /**
     * Whether the guidance takes its fixes from Dashwheel's feed instead of the
     * library's own engine.
     *
     * Only where the library's listener cannot work: `LocationListener`'s
     * callbacks are `default` from API 30 on, so below that a listener that does
     * not override them dies with `AbstractMethodError` ([FeedLocationEngine]).
     * From API 30 on the library engine works, and every other unit keeps
     * behaving exactly as upstream does.
     */
    internal fun usesFeedEngine(sdk: Int): Boolean = sdk < Build.VERSION_CODES.R

    /** The location source for in-app guidance; [sdk] is injectable so the choice can be tested off-device. */
    fun navLocationEngine(context: Context, sdk: Int = Build.VERSION.SDK_INT): LocationEngine =
        if (usesFeedEngine(sdk)) FeedLocationEngine(context)
        else MapLibreLocationEngine(context, Looper.getMainLooper())
}
