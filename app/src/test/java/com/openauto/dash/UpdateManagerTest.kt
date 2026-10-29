package com.openauto.dash

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Release tag -> build number, and which release asset is the launcher's own APK. */
class UpdateManagerTest {

    @Test
    fun theLauncherApkIsTheOneThatIsNeitherCompanionNorPlay() {
        val assets = listOf(
            "mydashwheel-companion.apk" to "https://github.com/ravalosv/MyDashWheel/releases/download/v1.0.42/mydashwheel-companion.apk",
            "MyDashwheel-1.0.42-play.aab" to "https://github.com/ravalosv/MyDashWheel/releases/download/v1.0.42/MyDashwheel-1.0.42-play.aab",
            "MyDashwheel-1.0.42-PLAY.apk" to "https://github.com/ravalosv/MyDashWheel/releases/download/v1.0.42/MyDashwheel-1.0.42-PLAY.apk",
            "MyDashwheel-1.0.42.apk" to "https://github.com/ravalosv/MyDashWheel/releases/download/v1.0.42/MyDashwheel-1.0.42.apk"
        )
        assertEquals(assets.last().second, UpdateManager.pickLauncherApk(assets))
    }

    @Test
    fun noLauncherApkAmongPlayAndCompanionAssets() {
        val assets = listOf(
            "mydashwheel-companion.apk" to "https://github.com/x/y/releases/download/v1/mydashwheel-companion.apk",
            "MyDashwheel-1.0.42-play.apk" to "https://github.com/x/y/releases/download/v1/MyDashwheel-1.0.42-play.apk",
            "MyDashwheel-1.0.42.aab" to "https://github.com/x/y/releases/download/v1/MyDashwheel-1.0.42.aab"
        )
        assertNull(UpdateManager.pickLauncherApk(assets))
    }

    @Test
    fun onlyGitHubHostsOverHttpsAreTrusted() {
        assertNull(UpdateManager.pickLauncherApk(listOf("MyDashwheel.apk" to "http://github.com/a/b.apk")))
        assertNull(UpdateManager.pickLauncherApk(listOf("MyDashwheel.apk" to "https://evil.example.com/Dashwheel.apk")))
        assertNull(UpdateManager.pickLauncherApk(listOf("MyDashwheel.apk" to "")))
        val ok = "https://objects.githubusercontent.com/some/path/Dashwheel.apk"
        assertEquals(ok, UpdateManager.pickLauncherApk(listOf("MyDashwheel.apk" to ok)))
    }

    @Test
    fun assetNamesTellTheEditionsApart() {
        assertTrue(UpdateManager.isLauncherApk("MyDashwheel-1.0.42.apk"))
        assertTrue(UpdateManager.isLauncherApk("MYDASHWHEEL.APK"))
        assertFalse(UpdateManager.isLauncherApk("MyDashwheel-1.0.42-play.apk"))
        assertFalse(UpdateManager.isLauncherApk("MyDashwheel-1.0.42-Play.apk"))
        assertFalse(UpdateManager.isLauncherApk("MyDashwheel-1.0.42.aab"))
        assertFalse(UpdateManager.isLauncherApk("mydashwheel-companion.apk"))
        assertFalse(UpdateManager.isLauncherApk("MyDashwheel-1.0.42.apk.sha256"))
    }

    @Test
    fun lastNumberInTheTagIsTheBuild() {
        assertEquals(42L, UpdateManager.parseBuildNumber("v1.0.42"))
        assertEquals(119L, UpdateManager.parseBuildNumber("MyDashwheel v1.0.119"))
        assertEquals(7L, UpdateManager.parseBuildNumber("7"))
    }

    @Test
    fun tagWithoutBuildSuffixReadsAsZero() {
        // "v1.0" means build 0, which is older than every CI build, so the
        // updater offers the real release rather than staying silent.
        assertEquals(0L, UpdateManager.parseBuildNumber("v1.0"))
    }

    @Test
    fun noDigitsMeansUnknown() {
        assertNull(UpdateManager.parseBuildNumber("nightly"))
        assertNull(UpdateManager.parseBuildNumber(""))
    }

    @Test
    fun onlyBuildsOfMainAreUpdates() {
        assertTrue(UpdateManager.isMainTag("v1.0.712"))
        assertFalse(UpdateManager.isMainTag("my-branch-v1.0.700"))
        assertFalse(UpdateManager.isMainTag("second-screen-image"))
        assertFalse(UpdateManager.isMainTag("v1.0.712-rc"))
    }
}
