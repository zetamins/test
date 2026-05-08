package com.stalkerhek

import com.stalkerhek.di.appModule
import com.stalkerhek.presentation.common.LogManager
import com.stalkerhek.presentation.hls.HlsProxyService
import com.stalkerhek.presentation.proxy.StalkerProxyService
import com.stalkerhek.presentation.web.WebUIService
import io.ktor.server.engine.*
import io.ktor.server.cio.*
import kotlinx.coroutines.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin
import java.util.concurrent.atomic.AtomicBoolean

class StalkerhekApplication : KoinComponent {
    private val logManager: LogManager by inject()
    private val webUIService: WebUIService by inject()
    private val stalkerProxyService: StalkerProxyService by inject()
    private val hlsProxyService: HlsProxyService by inject()

    private val running = AtomicBoolean(false)
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    suspend fun start() {
        if (running.getAndSet(true)) {
            logManager.warn("Application", "Application already running")
            return
        }

        logManager.info("Application", "Starting Stalkerhek...")

        try {
            // Start Web UI
            webUIService.startWebUI(4400)
            logManager.info("Application", "Web UI started on port 4400")

            logManager.info("Application", "Stalkerhek started successfully!")
            logManager.info("Application", "Web UI: http://localhost:4400")
            logManager.info("Application", "Press Ctrl+C to stop")

        } catch (e: Exception) {
            logManager.error("Application", "Failed to start: ${e.message}")
            running.set(false)
            throw e
        }
    }

    suspend fun stop() {
        if (!running.getAndSet(false)) {
            return
        }

        logManager.info("Application", "Shutting down Stalkerhek...")

        coroutineScope.cancel()

        webUIService.stopWebUI()

        logManager.info("Application", "Stalkerhek stopped")
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            // Initialize Koin
            startKoin {
                modules(appModule)
            }

            val application = StalkerhekApplication()

            // Handle shutdown signals
            Runtime.getRuntime().addShutdownHook(Thread {
                runBlocking {
                    application.stop()
                }
            })

            // Start application
            runBlocking {
                application.start()

                // Keep running until interrupted
                while (application.isRunning()) {
                    delay(1000)
                }
            }
        }

        private fun isRunning(): Boolean = true
    }

    fun isRunning(): Boolean = running.get()
}

suspend fun main(args: Array<String>) {
    startKoin {
        modules(appModule)
    }

    val application = StalkerhekApplication()

    Runtime.getRuntime().addShutdownHook(Thread {
        runBlocking {
            application.stop()
        }
    })

    runBlocking {
        application.start()

        while (application.isRunning()) {
            delay(1000)
        }
    }
}