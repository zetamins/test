package com.stalkerhek.presentation.common

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class LogManager {
    private val _logEntries = MutableSharedFlow<LogEntry>(replay = 100, extraBufferCapacity = 500)
    val logEntries: SharedFlow<LogEntry> = _logEntries.asSharedFlow()

    private val timestampFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")

    fun log(level: LogLevel, source: String, message: String) {
        val timestamp = LocalDateTime.now().format(timestampFormatter)
        val entry = LogEntry(
            timestamp = timestamp,
            level = level,
            source = source,
            message = message
        )
        _logEntries.tryEmit(entry)

        val color = when (level) {
            LogLevel.DEBUG -> "\u001B[36m" // Cyan
            LogLevel.INFO -> "\u001B[32m"  // Green
            LogLevel.WARN -> "\u001B[33m"  // Yellow
            LogLevel.ERROR -> "\u001B[31m" // Red
        }
        val reset = "\u001B[0m"
        println("$color[$timestamp] [$level] [$source]: $message$reset")
    }

    fun debug(source: String, message: String) = log(LogLevel.DEBUG, source, message)
    fun info(source: String, message: String) = log(LogLevel.INFO, source, message)
    fun warn(source: String, message: String) = log(LogLevel.WARN, source, message)
    fun error(source: String, message: String) = log(LogLevel.ERROR, source, message)
}

@Serializable
data class LogEntry(
    val timestamp: String,
    val level: LogLevel,
    val source: String,
    val message: String
)

@Serializable
enum class LogLevel {
    DEBUG, INFO, WARN, ERROR
}
