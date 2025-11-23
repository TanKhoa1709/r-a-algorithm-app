package cs.api

import cs.CSHost
import cs.resources.ResourceManager
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

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
                val request = call.receive<AccessRequest>()
                val granted = csHost.requestAccess(request.nodeId, request.requestId)
                call.respond(mapOf("granted" to granted))
            }
            
            // Release access
            post("/release") {
                val request = call.receive<ReleaseRequest>()
                csHost.releaseAccess(request.nodeId, request.requestId)
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
                val request = call.receive<AccessRequest>()
                val resourceManager = csHost.getResourceManager()
                val result = resourceManager.accessResource(resourceId, request.nodeId, request.requestId)
                call.respond(result)
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

