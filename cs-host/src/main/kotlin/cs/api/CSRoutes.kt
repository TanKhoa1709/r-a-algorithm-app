package cs.api

import cs.CSHost
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * REST API routes for CS Host
 */
fun Application.configureCSRoutes(csHost: CSHost) {
    routing {
        route("/api") {
            // Get current state
            get("/state") {
                call.respond(csHost.getState())
            }
            
            // Request access
            post("/request") {
                val payload = call.receiveValidated<AccessRequest>() ?: return@post
                val granted = csHost.requestAccess(payload.nodeId, payload.requestId)
                call.respond(mapOf("granted" to granted))
            }
            
            // Release access
            post("/release") {
                val payload = call.receiveValidated<ReleaseRequest>() ?: return@post
                csHost.releaseAccess(payload.nodeId, payload.requestId)
                call.respond(mapOf("success" to true))
            }
            
            // Get access history
            get("/history") {
                call.respond(csHost.getAccessHistory())
            }
            
            // Get resources
            get("/resources") {
                val resourceManager = csHost.getResourceManager()
                call.respond(resourceManager.getAllResources().map { it.getState() })
            }
            
            // Access resource
            post("/resources/{resourceId}/access") {
                val resourceId = call.parameters["resourceId"] ?: return@post call.respond(
                    mapOf("error" to "Missing resourceId")
                )
                val payload = call.receiveValidated<AccessRequest>() ?: return@post
                val resourceManager = csHost.getResourceManager()
                val result = resourceManager.accessResource(resourceId, payload.nodeId, payload.requestId)
                call.respond(result)
            }

            // Release resource (optional, symmetry with access)
            post("/resources/{resourceId}/release") {
                val resourceId = call.parameters["resourceId"] ?: return@post call.respond(
                    mapOf("error" to "Missing resourceId")
                )
                val payload = call.receiveValidated<AccessRequest>() ?: return@post
                val resourceManager = csHost.getResourceManager()
                resourceManager.releaseResource(resourceId, payload.nodeId, payload.requestId)
                call.respond(mapOf("success" to true))
            }
        }
    }
}

@Serializable
data class AccessRequest(
    val nodeId: String,
    val requestId: String
)

@Serializable
data class ReleaseRequest(
    val nodeId: String,
    val requestId: String
)

/**
 * Helper to receive and validate JSON payloads with lenient parsing and clear errors.
 */
private suspend inline fun <reified T : Any> ApplicationCall.receiveValidated(): T? {
    val text = kotlin.runCatching { receiveText() }.getOrElse { "" }
    return runCatching {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }.decodeFromString<T>(text.ifBlank { throw SerializationException("Empty body") })
    }.getOrElse { ex ->
        respond(
            HttpStatusCode.BadRequest,
            mapOf(
                "error" to "Invalid payload",
                "details" to (ex.message ?: "deserialization error"),
                "body" to text
            )
        )
        null
    }
}
