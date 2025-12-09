package cs.monitor

import app.models.CSState
import io.ktor.server.websocket.*
import io.ktor.websocket.send
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object NodeBroadcaster {
    private val sessions = mutableSetOf<WebSocketServerSession>()
    private val mutex = Mutex()

    suspend fun register(session: WebSocketServerSession) {
        mutex.withLock {
            sessions += session
        }
    }

    suspend fun unregister(session: WebSocketServerSession) {
        mutex.withLock {
            sessions -= session
        }
    }

    suspend fun broadcast(state: CSState) {
        val text = Json.encodeToString(state)
        val copy: List<WebSocketServerSession>
        mutex.withLock {
            copy = sessions.toList()
        }
        copy.forEach { session ->
            try {
                session.send(text)
            } catch (e: Exception) {
                // Connection lost, will be cleaned up
                mutex.withLock {
                    sessions -= session
                }
            }
        }
    }
}

