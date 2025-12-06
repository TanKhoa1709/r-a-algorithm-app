package cs.monitor

import app.models.VisualizerSnapshot
import io.ktor.server.websocket.*
import io.ktor.websocket.send
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object VisualizerBroadcaster {

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

    suspend fun broadcast(snapshot: VisualizerSnapshot) {
        val text = Json.encodeToString(snapshot)
        val copy: List<WebSocketServerSession>
        mutex.withLock {
            copy = sessions.toList()
        }
        copy.forEach { session ->
            session.send(text)
        }
    }
}
