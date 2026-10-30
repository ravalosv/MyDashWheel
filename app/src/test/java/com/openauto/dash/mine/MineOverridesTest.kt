// MINE: código propio del fork MyDashwheel.
package com.openauto.dash.mine

import android.os.Build
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MineOverridesTest {

    @Test
    fun ownEngineOnlyWhereTheLibraryListenerCannotWork() {
        // The library's listener does not override the callbacks that are abstract
        // before API 30: there, Dashwheel's feed is the source (ticket #12).
        assertTrue(MineOverrides.usesFeedEngine(Build.VERSION_CODES.Q))
        assertTrue(MineOverrides.usesFeedEngine(Build.VERSION_CODES.R - 1))
    }

    @Test
    fun libraryEngineFromApi30On() {
        // Where LocationListener's callbacks are default methods the library works,
        // and every other unit keeps the upstream behaviour.
        assertFalse(MineOverrides.usesFeedEngine(Build.VERSION_CODES.R))
        assertFalse(MineOverrides.usesFeedEngine(Build.VERSION_CODES.UPSIDE_DOWN_CAKE))
    }
}
