package app.node.controller

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Event types for logging
 */
enum class EventType {
    INFO,
    SUCCESS,
    WARNING,
    ERROR,
    REQUEST_SENT,
    REQUEST_RECEIVED,
    REPLY_SENT,
    REPLY_RECEIVED,
    RELEASE_SENT,
    RELEASE_RECEIVED
}

/**
 * Represents a single event log entry
 */
data class EventLogEntry(
    val timestamp: LocalDateTime,
    val type: EventType,
    val message: String,
    val details: Map<String, String> = emptyMap()
) {
    fun formatTimestamp(): String {
        return timestamp.format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"))
    }
    
    fun getTypeColor(): String {
        return when (type) {
            EventType.SUCCESS -> "success"
            EventType.WARNING -> "warning"
            EventType.ERROR -> "error"
            EventType.REQUEST_SENT, EventType.REQUEST_RECEIVED -> "request"
            EventType.REPLY_SENT, EventType.REPLY_RECEIVED -> "reply"
            EventType.RELEASE_SENT, EventType.RELEASE_RECEIVED -> "release"
            else -> "info"
        }
    }
}

/**
 * Thread-safe event logger with fixed-size buffer
 */
class EventLogger(private val maxEntries: Int = 200) {
    private val events = ConcurrentLinkedQueue<EventLogEntry>()
    
    fun log(type: EventType, message: String, details: Map<String, String> = emptyMap()) {
        val entry = EventLogEntry(LocalDateTime.now(), type, message, details)
        events.offer(entry)
        
        // Remove oldest entries if exceeds max size
        while (events.size > maxEntries) {
            events.poll()
        }
    }
    
    fun info(message: String, details: Map<String, String> = emptyMap()) {
        log(EventType.INFO, message, details)
    }
    
    fun success(message: String, details: Map<String, String> = emptyMap()) {
        log(EventType.SUCCESS, message, details)
    }
    
    fun warning(message: String, details: Map<String, String> = emptyMap()) {
        log(EventType.WARNING, message, details)
    }
    
    fun error(message: String, details: Map<String, String> = emptyMap()) {
        log(EventType.ERROR, message, details)
    }
    
    fun requestSent(message: String, details: Map<String, String> = emptyMap()) {
        log(EventType.REQUEST_SENT, message, details)
    }
    
    fun requestReceived(message: String, details: Map<String, String> = emptyMap()) {
        log(EventType.REQUEST_RECEIVED, message, details)
    }
    
    fun replySent(message: String, details: Map<String, String> = emptyMap()) {
        log(EventType.REPLY_SENT, message, details)
    }
    
    fun replyReceived(message: String, details: Map<String, String> = emptyMap()) {
        log(EventType.REPLY_RECEIVED, message, details)
    }
    
    fun releaseSent(message: String, details: Map<String, String> = emptyMap()) {
        log(EventType.RELEASE_SENT, message, details)
    }
    
    fun releaseReceived(message: String, details: Map<String, String> = emptyMap()) {
        log(EventType.RELEASE_RECEIVED, message, details)
    }
    
    fun getEntries(): List<EventLogEntry> {
        return events.toList()
    }
    
    fun clear() {
        events.clear()
    }
}

