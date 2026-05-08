package com.stalkerhek.presentation.proxy

import com.stalkerhek.domain.entity.Profile
import com.stalkerhek.domain.repository.StalkerRepository
import com.stalkerhek.presentation.common.LogManager
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.cio.CIO
import io.ktor.server.plugins.callloging.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap

class StalkerProxyService(
    private val stalkerRepository: StalkerRepository,
    private val logManager: LogManager
) {
    private val runningServers = ConcurrentHashMap<String, ApplicationEngine>()

    suspend fun startProxyForProfile(profile: Profile) {
        if (runningServers.containsKey(profile.id)) {
            logManager.warn("StalkerProxy", "Proxy already running for profile ${profile.name}")
            return
        }

        val proxyPort = profile.settings.httpPort
        logManager.info("StalkerProxy", "Starting Stalker proxy on port $proxyPort for profile ${profile.name}")

        val server = embeddedServer(CIO, port = proxyPort, host = "0.0.0.0") {
            install(CallLogging)
            routing {
                get("/portal.php") {
                    handlePortalRequest(call, profile)
                }
                post("/portal.php") {
                    handlePortalRequest(call, profile)
                }
            }
        }

        server.start()
        runningServers[profile.id] = server
        logManager.info("StalkerProxy", "Stalker proxy started successfully for profile ${profile.name}")
    }

    suspend fun stopProxyForProfile(profileId: String) {
        val server = runningServers.remove(profileId)
        if (server != null) {
            server.stop()
            logManager.info("StalkerProxy", "Stalker proxy stopped for profile $profileId")
        }
    }

    private suspend fun handlePortalRequest(call: ApplicationCall, profile: Profile) {
        val action = call.request.queryParameters["action"] ?: ""
        logManager.debug("StalkerProxy", "Request action: $action for profile ${profile.name}")

        // Simple proxy implementation - in a real scenario this would mimic Stalker portal responses
        when (action) {
            "handshake" -> {
                call.respondText("{\"js\":{\"token\":\"fake-token-123\"}}")
            }
            "get_profile" -> {
                call.respondText("{\"js\":{\"id\":\"1\",\"fname\":\"User\"}}")
            }
            else -> {
                call.respondText("{\"js\":[]}")
            }
        }
    }
}
