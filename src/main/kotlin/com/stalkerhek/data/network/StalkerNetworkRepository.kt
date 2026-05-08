package com.stalkerhek.data.network

import com.stalkerhek.domain.entity.*
import com.stalkerhek.domain.repository.Category
import com.stalkerhek.domain.repository.StalkerRepository
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.*
import java.net.URLEncoder

class StalkerNetworkRepository : StalkerRepository {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30000
        }
    }

    private val json = Json { ignoreUnknownKeys = true }

    private fun parsePortalResponse(body: String): JsonElement {
        val trimmed = body.trim()
        if (trimmed.startsWith("<!DOCTYPE html", ignoreCase = true) || trimmed.startsWith("<html", ignoreCase = true)) {
            throw Exception("Received HTML response instead of JSON. The portal URL might be incorrect or requires authentication.")
        }
        return try {
            json.parseToJsonElement(body)
        } catch (e: Exception) {
            throw Exception("Failed to parse JSON response: ${e.message}. Body starts with: ${body.take(100)}")
        }
    }

    private fun getCommonHeaders(portalUrl: String, settings: ProfileSettings, token: String? = null): Map<String, String> {
        val headers = mutableMapOf(
            "User-Agent" to "Mozilla/5.0 (QtEmbedded; U; Linux; C) AppleWebKit/533.3 (KHTML, like Gecko) MAG200 stbapp ver: 4 rev: 2116 Mobile Safari/533.3",
            "X-User-Agent" to "Model: " + settings.model + "; Link: Ethernet",
            "Accept" to "*/*",
            "Accept-Language" to "en-US,en;q=0.9",
            "Cache-Control" to "no-cache",
            "Pragma" to "no-cache",
            "Referer" to portalUrl,
            "Origin" to portalUrl
        )
        token?.let { headers["Authorization"] = "Bearer $it" }
        return headers
    }

    private fun getCookieString(settings: ProfileSettings): String {
        return "sn=" + settings.serialNumber + "; mac=" + settings.mac + "; stb_lang=en; timezone=" + URLEncoder.encode(settings.timezone, "UTF-8")
    }

    override suspend fun handshake(portalUrl: String, settings: ProfileSettings): Result<String> = runCatching {
        val url = portalUrl + "?type=stb&action=handshake&JsHttpRequest=1-xml"
        val response = client.get(url) {
            getCommonHeaders(portalUrl, settings).forEach { (k, v) -> header(k, v) }
            header("Cookie", getCookieString(settings))
        }
        val body = response.bodyAsText()
        val jsonElement = parsePortalResponse(body)
        jsonElement.jsonObject["js"]?.jsonObject?.get("token")?.jsonPrimitive?.content ?: ""
    }

    override suspend fun authenticate(
        portalUrl: String,
        login: String,
        password: String,
        settings: ProfileSettings
    ): Result<StalkerSession> = runCatching {
        val url = portalUrl
        val tokenResult = handshake(portalUrl, settings)
        val token = tokenResult.getOrThrow()
        
        val response = client.submitForm(
            url = url,
            formParameters = parameters {
                append("type", "stb")
                append("action", "do_auth")
                append("login", login)
                append("password", password)
                append("JsHttpRequest", "1-xml")
            }
        ) {
            getCommonHeaders(portalUrl, settings, token).forEach { (k, v) -> header(k, v) }
            header("Cookie", getCookieString(settings))
        }
        
        val body = response.bodyAsText()
        val jsonElement = parsePortalResponse(body)
        val success = jsonElement.jsonObject["js"]?.jsonPrimitive?.booleanOrNull ?: false
        if (success) {
            StalkerSession(
                token = token,
                expiresAt = System.currentTimeMillis() + 3600000,
                mac = settings.macAddress,
                deviceId = settings.deviceId,
                deviceId2 = settings.deviceId2,
                signature = settings.signature,
                userLogin = login,
                userPassword = password
            )
        } else {
            throw Exception("Authentication failed")
        }
    }

    override suspend fun getCategories(portalUrl: String, session: StalkerSession): Result<List<Category>> = Result.success(emptyList())

    override suspend fun getChannels(portalUrl: String, session: StalkerSession, categoryId: Int?): Result<List<Channel>> = Result.success(emptyList())

    override suspend fun createLink(portalUrl: String, session: StalkerSession, channelId: Int, cmd: String): Result<String> = Result.failure(Exception("Not implemented"))

    override suspend fun keepAlive(portalUrl: String, session: StalkerSession): Result<Boolean> = Result.success(true)
}
