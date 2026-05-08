package com.stalkerhek.presentation.web

import com.stalkerhek.domain.entity.Profile
import com.stalkerhek.domain.entity.ProfileSettings
import com.stalkerhek.domain.usecase.ManageProfilesUseCase
import com.stalkerhek.domain.usecase.StalkerAuthUseCase
import com.stalkerhek.presentation.common.LogEntry
import com.stalkerhek.presentation.common.LogManager
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.cio.CIO
import io.ktor.server.plugins.callloging.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.util.concurrent.ConcurrentHashMap

@Serializable
data class UserSessionData(val username: String, val role: String, val createdAt: Long = System.currentTimeMillis())

class WebUIService(
    private val profileUseCase: ManageProfilesUseCase,
    private val authUseCase: StalkerAuthUseCase,
    private val logManager: LogManager
) {
    private var server: ApplicationEngine? = null

    suspend fun startWebUI(port: Int) {
        server = embeddedServer(CIO, port = port, host = "0.0.0.0") {
            install(WebSockets)
            install(CallLogging)
            
            routing {
                get("/") { call.respondRedirect("/dashboard") }
                get("/dashboard") { handleDashboard(call) }
                
                get("/profiles/manage") { handleManageProfiles(call) }
                get("/profiles/add") { handleAddProfileForm(call) }
                post("/profiles/add") { handleAddProfileSubmit(call) }
                get("/profiles/edit/{id}") { handleEditProfileForm(call) }
                post("/profiles/edit/{id}") { handleEditProfileSubmit(call) }
                post("/profiles/delete/{id}") { handleDeleteProfile(call) }

                route("/api") {
                    post("/profiles/sync/{id}") {
                        val id = call.parameters["id"] ?: ""
                        val profile = profileUseCase.getProfile(id)
                        if (profile != null) {
                            val result = profileUseCase.syncChannels(profile)
                            val json = buildJsonObject {
                                put("success", result.isSuccess)
                                if (result.isSuccess) {
                                    put("channelsCount", result.getOrNull()?.channels?.size ?: 0)
                                } else {
                                    put("error", result.exceptionOrNull()?.message ?: "Unknown error")
                                }
                            }
                            call.respondText(json.toString(), ContentType.Application.Json)
                        } else {
                            call.respondText("{\"success\":false, \"error\":\"Not found\"}", ContentType.Application.Json)
                        }
                    }
                }

                webSocket("/logs") {
                    logManager.logEntries.collect { entry: LogEntry ->
                        send(Json.encodeToString(entry))
                    }
                }
            }
        }
        server?.start()
    }

    suspend fun stopWebUI() {
        server?.stop(1000, 5000)
    }

    private fun layout(title: String, content: String) = """
        <!DOCTYPE html>
        <html>
        <head>
            <title>$title - Stalkerhek</title>
            <style>
                body { font-family: 'Segoe UI', system-ui, sans-serif; background: #1a1a2e; color: #fff; margin: 0; line-height: 1.6; }
                .navbar { background: #16213e; padding: 1rem 2rem; display: flex; align-items: center; gap: 2rem; border-bottom: 2px solid #e94560; }
                .navbar a { color: #fff; text-decoration: none; font-weight: 500; opacity: 0.8; }
                .navbar .brand { font-size: 1.5rem; font-weight: bold; color: #e94560; margin-right: auto; }
                .container { max-width: 1000px; margin: 2rem auto; padding: 0 1rem; }
                .card { background: #0f3460; border-radius: 12px; padding: 2rem; margin-bottom: 2rem; }
                .btn { background: #e94560; color: white; border: none; padding: 0.6rem 1.2rem; border-radius: 6px; cursor: pointer; text-decoration: none; }
                table { width: 100%; border-collapse: collapse; margin: 1.5rem 0; }
                th, td { text-align: left; padding: 1rem 0.5rem; border-bottom: 1px solid #16213e; }
                input { width: 100%; padding: 0.8rem; margin: 0.5rem 0 1.5rem 0; background: #16213e; border: 1px solid #4e4e6a; border-radius: 6px; color: #fff; }
            </style>
        </head>
        <body>
            <div class="navbar">
                <a href="/" class="brand">stalkerhek</a>
                <a href="/dashboard">Dashboard</a>
                <a href="/profiles/manage">Manage Profiles</a>
            </div>
            <div class="container">$content</div>
        </body>
        </html>
    """.trimIndent()

    private suspend fun handleDashboard(call: ApplicationCall) {
        val dol = "$"
        val content = """
            <div class="card">
                <h1>System Status</h1>
                <p>Application is running and healthy.</p>
            </div>
            <div class="card">
                <h2>Real-time Logs</h2>
                <div id="logs" style="height: 400px; overflow-y: auto; background: #000; padding: 1rem; font-family: monospace; color: #00ff41;"></div>
            </div>
            <script>
                const logsDiv = document.getElementById('logs');
                const ws = new WebSocket('ws://' + window.location.host + '/logs');
                ws.onmessage = function(event) {
                    const log = JSON.parse(event.data);
                    const entry = document.createElement('div');
                    entry.textContent = `[${dol}{log.timestamp}] [${dol}{log.level}] [${dol}{log.source}] ${dol}{log.message}`;
                    logsDiv.appendChild(entry);
                    logsDiv.scrollTop = logsDiv.scrollHeight;
                };
            </script>
        """.trimIndent()
        call.respondText(layout("Dashboard", content), ContentType.Text.Html)
    }

    private suspend fun handleManageProfiles(call: ApplicationCall) {
        val profiles = profileUseCase.getAllProfiles()
        var rows = ""
        profiles.forEach { p ->
            rows += "<tr><td>${p.name}</td><td>${p.portalUrl}</td><td>${p.channels.size}</td><td><a href='/profiles/edit/${p.id}'>Edit</a></td></tr>"
        }
        val content = """
            <div class="card">
                <h1>Manage Profiles</h1>
                <table><thead><tr><th>Name</th><th>URL</th><th>Channels</th><th>Actions</th></tr></thead>
                <tbody>$rows</tbody></table>
                <a href="/profiles/add" class="btn">Add Profile</a>
            </div>
        """.trimIndent()
        call.respondText(layout("Manage Profiles", content), ContentType.Text.Html)
    }

    private suspend fun handleAddProfileForm(call: ApplicationCall) {
        val content = """
            <div class="card">
                <h1>Add Profile</h1>
                <form action="/profiles/add" method="post">
                    <label>Name</label><input name="name" required>
                    <label>Portal URL</label><input name="portalUrl" required>
                    <label>MAC Address</label><input name="mac" value="00:1A:79:00:00:00" required>
                    <button type="submit" class="btn">Save</button>
                </form>
            </div>
        """.trimIndent()
        call.respondText(layout("Add Profile", content), ContentType.Text.Html)
    }

    private suspend fun handleAddProfileSubmit(call: ApplicationCall) {
        val params = call.receiveParameters()
        val profile = Profile(
            name = params["name"] ?: "",
            portalUrl = params["portalUrl"] ?: "",
            settings = ProfileSettings(mac = params["mac"] ?: "", macAddress = params["mac"] ?: "")
        )
        profileUseCase.createProfile(profile)
        call.respondRedirect("/profiles/manage")
    }

    private suspend fun handleEditProfileForm(call: ApplicationCall) {
        val id = call.parameters["id"] ?: ""
        val p = profileUseCase.getProfile(id) ?: return call.respondRedirect("/profiles/manage")
        val content = """<div class="card"><h1>Edit ${p.name}</h1><form action="/profiles/edit/${p.id}" method="post"><label>Name</label><input name="name" value="${p.name}"><button type="submit" class="btn">Update</button></form></div>"""
        call.respondText(layout("Edit Profile", content), ContentType.Text.Html)
    }

    private suspend fun handleEditProfileSubmit(call: ApplicationCall) {
        val id = call.parameters["id"] ?: ""
        val p = profileUseCase.getProfile(id) ?: return call.respondRedirect("/profiles/manage")
        val params = call.receiveParameters()
        profileUseCase.updateProfile(p.copy(name = params["name"] ?: p.name))
        call.respondRedirect("/profiles/manage")
    }

    private suspend fun handleDeleteProfile(call: ApplicationCall) {
        val id = call.parameters["id"] ?: ""
        profileUseCase.deleteProfile(id)
        call.respondRedirect("/profiles/manage")
    }
}
