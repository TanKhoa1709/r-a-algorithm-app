package app.node.controller

import app.models.CSState
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

/**
 * Controller for interacting with CS Host
 */
class CSInteractionController(private val csHostUrl: String) {
    private val client = HttpClient(CIO) {
        install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
            })
        }
    }

    suspend fun getState(): CSState {
        val response = client.get("$csHostUrl/api/state")
        return Json.decodeFromString<CSState>(response.bodyAsText())
    }

    suspend fun requestAccess(nodeId: String, requestId: String): Boolean {
        val response = client.post("$csHostUrl/api/request") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("nodeId" to nodeId, "requestId" to requestId))
        }
        val result = Json.decodeFromString<Map<String, Boolean>>(response.bodyAsText())
        return result["granted"] ?: false
    }

    suspend fun releaseAccess(nodeId: String, requestId: String) {
        client.post("$csHostUrl/api/release") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("nodeId" to nodeId, "requestId" to requestId))
        }
    }

    fun close() {
        client.close()
    }
}

