package com.openauto.dash.companion.mine

import java.io.IOException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class CompanionReleaseFeedTest {
    private fun release(
        tag: String = "v1.0.910",
        beta: Boolean = false,
        draft: Boolean = false,
        asset: String = CompanionReleaseFeed.APK_ASSET,
        url: String = "https://github.com/ravalosv/MyDashWheel/releases/download/$tag/$asset"
    ): JsonElement = buildJsonObject {
        put("tag_name", tag)
        put("draft", draft)
        put("prerelease", beta)
        put("assets", JsonArray(listOf(buildJsonObject {
            put("name", asset)
            put("browser_download_url", url)
        })))
    }

    @Test fun stableRejectsBetasAndDrafts() {
        val releases = listOf(release("v1.0.912", draft = true), release("v1.0.911", beta = true), release("v1.0.882"))
        assertEquals(882L, CompanionReleaseFeed.select(releases, false)?.build)
    }

    @Test fun betaSelectsHighestBuildRegardlessOfListOrderOrPromotion() {
        val releases = listOf(release("v1.0.9", beta = true), release("v1.0.910"), release("v1.0.100", beta = true))
        assertEquals(910L, CompanionReleaseFeed.select(releases, true)?.build)
    }

    @Test fun betaFallsBackToReleaseWithCorrectCompanionAsset() {
        val releases = listOf(release("v1.0.999", asset = "MyDashwheel-1.0.999.apk"), release())
        assertEquals(910L, CompanionReleaseFeed.select(releases, true)?.build)
        assertNull(CompanionReleaseFeed.select(listOf(release(asset = "dashwheel-companion.apk")), true))
    }

    @Test fun rejectsUnrecognizedOrOverflowingVersions() {
        listOf("branch-9999", "v1.0.910-test", "v2.0.999", "v1.0.0910", "v1.0.9999999999999999999999").forEach {
            assertNull(it, CompanionReleaseFeed.select(listOf(release(it)), true))
        }
    }

    @Test fun rejectsForeignOrInsecureApkUrls() {
        listOf(
            "http://github.com/ravalosv/MyDashWheel/releases/download/v1.0.910/mydashwheel-companion.apk",
            "https://github.com.evil.example/ravalosv/MyDashWheel/releases/download/v1.0.910/mydashwheel-companion.apk",
            "https://github.com/deviloufr-ai/Dashwheel/releases/download/v1.0.910/mydashwheel-companion.apk",
            "https://github.com/ravalosv/MyDashWheel/releases/download/v1.0.911/mydashwheel-companion.apk",
            "https://example.com/mydashwheel-companion.apk"
        ).forEach { url -> assertNull(url, CompanionReleaseFeed.select(listOf(release(url = url)), true)) }
    }

    @Test fun ignoresMalformedEntriesAndMissingFlags() {
        val badEntries = listOf(Json.parseToJsonElement("""{"tag_name":{},"draft":false,"prerelease":false}"""),
            Json.parseToJsonElement("""{"tag_name":"v1.0.999","assets":[]}"""), JsonArray(emptyList()))
        assertEquals(910L, CompanionReleaseFeed.select(badEntries + release(), true)?.build)
    }

    @Test fun stableUsesForkLatestEndpoint() {
        val selected = CompanionReleaseFeed.fetch(false) { url ->
            assertEquals("https://api.github.com/repos/ravalosv/MyDashWheel/releases/latest", url)
            release("v1.0.882").toString()
        }
        assertEquals(882L, selected.build)
        assertEquals("https://github.com/ravalosv/MyDashWheel/releases/download/v1.0.882/mydashwheel-companion.apk", selected.apkUrl)
    }

    @Test fun betaPaginatesAndSelectsHighestAcrossPages() {
        val requests = mutableListOf<String>()
        val selected = CompanionReleaseFeed.fetch(true) { url ->
            requests += url
            when (requests.size) {
                1 -> JsonArray(List(100) { release("v1.0.${it + 1}", beta = true) }).toString()
                2 -> JsonArray(listOf(release())).toString()
                else -> error("Unexpected request")
            }
        }
        assertEquals(910L, selected.build)
        assertEquals(listOf("${CompanionReleaseFeed.BASE_URL}?per_page=100&page=1",
            "${CompanionReleaseFeed.BASE_URL}?per_page=100&page=2"), requests)
    }

    @Test fun networkFailuresAreNotReportedAsNoUpdate() {
        assertThrows(IOException::class.java) {
            CompanionReleaseFeed.fetch(true) { throw IOException("offline") }
        }
        assertThrows(IOException::class.java) {
            CompanionReleaseFeed.fetch(true) { "[]" }
        }
        assertThrows(IllegalArgumentException::class.java) {
            CompanionReleaseFeed.fetch(true) { "invalid json" }
        }
    }

    @Test fun paginationFailureDoesNotOfferAPartialResult() {
        var requests = 0
        assertThrows(IOException::class.java) {
            CompanionReleaseFeed.fetch(true) {
                if (++requests == 1) JsonArray(List(100) { release() }).toString()
                else throw IOException("rate limited")
            }
        }
        assertEquals(2, requests)
    }

    @Test fun stableEndpointCannotOfferABeta() {
        assertThrows(IOException::class.java) {
            CompanionReleaseFeed.fetch(false) { release(beta = true).toString() }
        }
    }
}
