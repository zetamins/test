package com.stalkerhek.presentation.hls

import com.stalkerhek.domain.entity.Profile
import com.stalkerhek.domain.repository.StalkerRepository
import com.stalkerhek.presentation.common.LogManager
import io.ktor.client.*
import io.ktor.client.engine.cio.CIO as ClientCIO
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.cio.CIO as ServerCIO
import io.ktor.server.plugins.callloging.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import java.util.concurrent.ConcurrentHashMap

class HlsProxyService(
    private val stalkerRepository: StalkerRepository,
    private val logManager: LogManager
) {
    private val runningServers = ConcurrentHashMap<String, ApplicationEngine>()
    private val sessionCache = ConcurrentHashMap<String, SessionCache>()
    private val httpClient = HttpClient(ClientCIO) {
        install(HttpTimeout) {
            requestTimeoutMillis = 60000
            connectTimeoutMillis = 30000
            socketTimeoutMillis = 60000
        }
    }

    data class SessionCache(
        val profileId: String,
        val token: String,
        val originalUrl: String,
        val createdAt: Long = System.currentTimeMillis()
    )

    suspend fun startHlsProxyForProfile(profile: Profile) {
        if (runningServers.containsKey(profile.id)) {
            logManager.warn("HlsProxy", "HLS proxy already running for profile ${profile.name}")
            return
        }

        val hlsPort = profile.settings.hlsPort
        logManager.info("HlsProxy", "Starting HLS proxy on port $hlsPort for profile ${profile.name}")

        val server = embeddedServer(ServerCIO, port = hlsPort, host = "0.0.0.0") {
            install(CallLogging)
            routing {
                get("/") {
                    handleHlsRoot(call, profile)
                }
                get("/{path}") {
                    handleHlsRequest(call, profile)
                }
            }
        }

        server.start()
        runningServers[profile.id] = server
        logManager.info("HlsProxy", "HLS proxy started successfully for profile ${profile.name}")
    }

    suspend fun stopHlsProxyForProfile(profileId: String) {
        val server = runningServers.remove(profileId)
        if (server != null) {
            server.stop()
            logManager.info("HlsProxy", "HLS proxy stopped for profile $profileId")
        }
        sessionCache.remove(profileId)
    }

    private suspend fun handleHlsRoot(call: ApplicationCall, profile: Profile) {
        call.respondText("""
            <!DOCTYPE html>
            <html>
            <head>
                <title>Stalkerhek HLS Proxy</title>
                <style>
                    body { font-family: Arial, sans-serif; padding: 20px; background: #1a1a2e; color: #fff; }
                    h1 { color: #e94560; }
                    p { color: #a0a0a0; }
                </style>
            </head>
            <body>
                <h1>Stalkerhek HLS Proxy</h1>
                <p>Proxy is running for profile: ${profile.name}</p>
                <p>Portal: ${profile.portalUrl}</p>
            </body>
            </html>
        """.trimIndent(), ContentType.Text.Html)
    }

    private suspend fun handleHlsRequest(call: ApplicationCall, profile: Profile) {
        val path = call.parameters["path"] ?: ""

        try {
            when {
                path.endsWith(".m3u8") -> {
                    handlePlaylistRequest(call, profile, path)
                }
                path.endsWith(".ts") || path.endsWith(".m4s") || path.contains(".ts?") -> {
                    handleSegmentRequest(call, profile, path)
                }
                else -> {
                    handleGenericHlsRequest(call, profile, path)
                }
            }
        } catch (e: Exception) {
            logManager.error("HlsProxy", "Error handling HLS request: ${e.message}")
            call.respondText("Error processing request: ${e.message}", ContentType.Text.Plain, HttpStatusCode.InternalServerError)
        }
    }

    private suspend fun handlePlaylistRequest(call: ApplicationCall, profile: Profile, path: String) {
        logManager.debug("HlsProxy", "Playlist request: $path")

        val cache = sessionCache[profile.id] ?: run {
            call.respondText("No active session", ContentType.Text.Plain, HttpStatusCode.BadRequest)
            return
        }

        val originalUrl = cache.originalUrl
        val playlistUrl = buildPlaylistUrl(originalUrl, path)

        try {
            val response = httpClient.get(playlistUrl)
            val playlistContent = response.bodyAsText()

            val modifiedPlaylist = rewritePlaylist(playlistContent, originalUrl, profile.settings.hlsPort)

            call.respondText(modifiedPlaylist, ContentType.parse("application/vnd.apple.mpegurl"))
        } catch (e: Exception) {
            logManager.error("HlsProxy", "Failed to fetch playlist: ${e.message}")
            call.respondText("Failed to fetch playlist: ${e.message}", ContentType.Text.Plain, HttpStatusCode.InternalServerError)
        }
    }

    private suspend fun handleSegmentRequest(call: ApplicationCall, profile: Profile, path: String) {
        logManager.debug("HlsProxy", "Segment request: $path")

        val cache = sessionCache[profile.id] ?: run {
            call.respondText("No active session", ContentType.Text.Plain, HttpStatusCode.BadRequest)
            return
        }

        val originalUrl = cache.originalUrl
        val segmentUrl = buildSegmentUrl(originalUrl, path)

        try {
            val response = httpClient.get(segmentUrl) {
                headers {
                    append("User-Agent", "Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36")
                    append("X-User-Agent", "Model: MAG270; Build: 1.7.0.4; Team: Development;")
                }
            }

            val segmentBytes = response.readBytes()
            call.respondBytes(segmentBytes, ContentType.parse("video/MP2T"))
        } catch (e: Exception) {
            logManager.error("HlsProxy", "Failed to fetch segment: ${e.message}")
            call.respondText("Failed to fetch segment: ${e.message}", ContentType.Text.Plain, HttpStatusCode.InternalServerError)
        }
    }

    private suspend fun handleGenericHlsRequest(call: ApplicationCall, profile: Profile, path: String) {
        val cache = sessionCache[profile.id] ?: run {
            if (path.isEmpty() || path == "live") {
                call.respondText("Waiting for stream...", ContentType.Text.Plain)
                return
            }
            call.respondText("No active session", ContentType.Text.Plain, HttpStatusCode.BadRequest)
            return
        }

        try {
            val response = httpClient.get(cache.originalUrl)
            val contentType = response.contentType().toString()
            val body = response.bodyAsText()

            if (contentType.contains("m3u8")) {
                val modifiedPlaylist = rewritePlaylist(body, cache.originalUrl, profile.settings.hlsPort)
                call.respondText(modifiedPlaylist, ContentType.parse("application/vnd.apple.mpegurl"))
            } else {
                call.respondText(body, ContentType.parse(contentType))
            }
        } catch (e: Exception) {
            logManager.error("HlsProxy", "Failed to handle generic request: ${e.message}")
            call.respondText("Error: ${e.message}", ContentType.Text.Plain, HttpStatusCode.InternalServerError)
        }
    }

    fun setStreamUrl(profileId: String, originalUrl: String, token: String) {
        sessionCache[profileId] = SessionCache(
            profileId = profileId,
            token = token,
            originalUrl = originalUrl
        )
        logManager.info("HlsProxy", "Stream URL set for profile $profileId: $originalUrl")
    }

    private fun buildPlaylistUrl(baseUrl: String, path: String): String {
        return if (path.startsWith("http")) {
            path
        } else if (path.startsWith("/")) {
            val baseUri = java.net.URI(baseUrl)
            "${baseUri.scheme}://${baseUri.host}:${baseUri.port}$path"
        } else {
            val lastSlash = baseUrl.lastIndexOf("/")
            if (lastSlash > 0) baseUrl.substring(0, lastSlash + 1) + path else baseUrl + "/" + path
        }
    }

    private fun buildSegmentUrl(baseUrl: String, segmentPath: String): String {
        return buildPlaylistUrl(baseUrl, segmentPath)
    }

    private fun rewritePlaylist(playlist: String, originalUrl: String, proxyPort: Int): String {
        val proxyHost = "127.0.0.1:$proxyPort"

        val lines = playlist.lines()
        val rewrittenLines = lines.map { line ->
            when {
                line.startsWith("#") -> line
                line.startsWith("http") -> {
                    val segmentPath = extractRelativePath(line, originalUrl)
                    "http://$proxyHost/$segmentPath"
                }
                line.startsWith("/") -> {
                    val segmentPath = line.substring(1)
                    "http://$proxyHost/$segmentPath"
                }
                line.isNotEmpty() -> {
                    "http://$proxyHost/$line"
                }
                else -> line
            }
        }

        var modified = rewrittenLines.joinToString("\n")

        val bandwidthRegex = Regex("#EXT-X-BANDWIDTH:(\\d+)")
        modified = modified.replace(bandwidthRegex) { "#EXT-X-BANDWIDTH:${it.groupValues[1].toInt() / 2}" }

        return modified
    }

    private fun extractRelativePath(url: String, baseUrl: String): String {
        return try {
            val urlUri = java.net.URI(url)
            val baseUri = java.net.URI(baseUrl)

            val path = urlUri.path ?: ""
            val query = urlUri.query?.let { "?$it" } ?: ""

            if (urlUri.host == baseUri.host) {
                path.substringAfter(baseUri.path.substringBeforeLast("/", missingDelimiterValue = "/"))
            } else {
                path
            }.let { resultPath ->
                if (query.isNotEmpty() && !resultPath.contains("?")) "$resultPath$query" else resultPath
            }
        } catch (e: Exception) {
            url.substringAfterLast("/", url)
        }
    }

    fun getRunningProxies(): Map<String, Int> {
        return runningServers.mapValues { (_, server) ->
            server.environment?.connectors?.firstOrNull()?.port ?: 0
        }
    }

    fun clearCache(profileId: String) {
        sessionCache.remove(profileId)
        logManager.debug("HlsProxy", "Cache cleared for profile $profileId")
    }
}
