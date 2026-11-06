package com.openauto.dash.companion.mine

import android.content.Context
import android.util.Log
import com.openauto.dash.companion.CompanionUpdate
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull

internal data class CompanionRelease(val build: Long, val name: String, val apkUrl: String)

internal object CompanionReleaseFeed {
    const val BASE_URL = "https://api.github.com/repos/ravalosv/MyDashWheel/releases"
    const val APK_ASSET = "mydashwheel-companion.apk"
    private val version = Regex("^v1\\.0\\.(0|[1-9][0-9]*)$")

    fun select(releases: List<JsonElement>, includeBeta: Boolean): CompanionRelease? =
        releases.mapNotNull { parse(it, includeBeta) }.maxByOrNull { it.build }

    private fun parse(element: JsonElement, includeBeta: Boolean): CompanionRelease? {
        val release = element as? JsonObject ?: return null
        if ((release["draft"] as? JsonPrimitive)?.booleanOrNull != false) return null
        val prerelease = (release["prerelease"] as? JsonPrimitive)?.booleanOrNull ?: return null
        if (prerelease && !includeBeta) return null
        val tag = (release["tag_name"] as? JsonPrimitive)?.contentOrNull ?: return null
        val build = version.matchEntire(tag)?.groupValues?.get(1)?.toLongOrNull() ?: return null
        val assets = release["assets"] as? JsonArray ?: return null
        val expectedUrl = "https://github.com/ravalosv/MyDashWheel/releases/download/$tag/$APK_ASSET"
        if (assets.filterIsInstance<JsonObject>().none {
            (it["name"] as? JsonPrimitive)?.contentOrNull == APK_ASSET &&
                (it["browser_download_url"] as? JsonPrimitive)?.contentOrNull == expectedUrl
        }) return null
        return CompanionRelease(build, tag, expectedUrl)
    }

    fun fetch(includeBeta: Boolean, request: (String) -> String = ::read): CompanionRelease {
        if (!includeBeta) {
            return select(listOf(Json.parseToJsonElement(request("$BASE_URL/latest"))), false)
                ?: throw IOException("Stable release has no valid Companion APK")
        }
        val releases = mutableListOf<JsonElement>()
        var page = 1
        do {
            val batch = Json.parseToJsonElement(request("$BASE_URL?per_page=100&page=$page")) as? JsonArray
                ?: throw IOException("Expected a GitHub release list")
            releases.addAll(batch)
            page++
        } while (batch.size == 100)
        return select(releases, true) ?: throw IOException("No valid Companion release found")
    }

    private fun read(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("User-Agent", "MyDashwheel-Companion-Updater")
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("GitHub release check returned HTTP ${connection.responseCode}")
            }
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }
}

internal object CompanionUpdateChannel {
    fun beta(context: Context): Boolean =
        context.getSharedPreferences("updates", Context.MODE_PRIVATE).getBoolean("beta", false)

    fun setBeta(context: Context, enabled: Boolean) {
        context.getSharedPreferences("updates", Context.MODE_PRIVATE).edit().putBoolean("beta", enabled).apply()
    }

    fun fetch(context: Context): CompanionUpdate.Release? = try {
        CompanionReleaseFeed.fetch(beta(context)).let { CompanionUpdate.Release(it.build, it.name, it.apkUrl) }
    } catch (e: IOException) {
        Log.e("CompanionUpdate", "Failed to fetch fork releases", e)
        null
    } catch (e: IllegalArgumentException) {
        Log.e("CompanionUpdate", "Invalid fork release response", e)
        null
    }
}
