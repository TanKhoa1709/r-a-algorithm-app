package app.net.http

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * HTTP client for REST API communication
 */
class HttpClient {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
            })
        }
    }

    suspend fun get(url: String): String {
        return client.get(url).bodyAsText()
    }

    suspend fun post(url: String, body: Any): String {
        return client.post(url) {
            contentType(ContentType.Application.Json)
            setBody(body)
        }.bodyAsText()
    }

    suspend fun put(url: String, body: Any): String {
        return client.put(url) {
            contentType(ContentType.Application.Json)
            setBody(body)
        }.bodyAsText()
    }

    suspend fun delete(url: String): String {
        return client.delete(url).bodyAsText()
    }

    fun close() {
        client.close()
    }
}

