package app.node.controller

import app.models.CSState
import app.models.TransactionRequest
import app.models.TransactionResult
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
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
        return client.get("$csHostUrl/api/state").body()
    }

    suspend fun requestAccess(nodeId: String, requestId: String): Boolean {
        val result: Map<String, Boolean> = client.post("$csHostUrl/api/request") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("nodeId" to nodeId, "requestId" to requestId))
        }.body()
        return result["granted"] ?: false
    }

    suspend fun releaseAccess(nodeId: String, requestId: String) {
        client.post("$csHostUrl/api/release") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("nodeId" to nodeId, "requestId" to requestId))
        }
    }

    suspend fun accessResource(resourceId: String, nodeId: String, requestId: String) {
        client.post("$csHostUrl/api/resources/$resourceId/access") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("nodeId" to nodeId, "requestId" to requestId))
        }
    }

    suspend fun releaseResource(resourceId: String, nodeId: String, requestId: String) {
        client.post("$csHostUrl/api/resources/$resourceId/release") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("nodeId" to nodeId, "requestId" to requestId))
        }
    }
    
    /**
     * Withdraw money from bank account
     */
    suspend fun withdraw(nodeId: String, requestId: String, amount: Long): TransactionResult {
        return client.post("$csHostUrl/api/bank/withdraw") {
            contentType(ContentType.Application.Json)
            setBody(TransactionRequest(nodeId, requestId, amount))
        }.body()
    }
    
    /**
     * Deposit money to bank account
     */
    suspend fun deposit(nodeId: String, requestId: String, amount: Long): TransactionResult {
        return client.post("$csHostUrl/api/bank/deposit") {
            contentType(ContentType.Application.Json)
            setBody(TransactionRequest(nodeId, requestId, amount))
        }.body()
    }
    
    /**
     * Get current bank balance
     */
    suspend fun getBalance(): Long {
        val result: Map<String, Long> = client.get("$csHostUrl/api/bank/balance").body()
        return result["balance"] ?: 0L
    }

    fun close() {
        client.close()
    }
}

