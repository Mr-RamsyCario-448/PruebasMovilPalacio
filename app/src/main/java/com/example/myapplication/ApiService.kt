package com.example.myapplication.network

import android.util.Log
import com.example.myapplication.NegocioResponse
import com.example.myapplication.Ticket
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object ApiService {

    // Cliente Ktor configurado con OkHttp y JSON
    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true // Ignora campos desconocidos en la respuesta
                }
            )
        }
    }

    // URL base de tu backend
    private const val BASE_URL = "http://192.168.18.5:3000"

    // Obtener todos los tickets
    suspend fun obtenerTickets(token: String): List<Ticket> {
        return client.post("$BASE_URL/enviadatosMercadoTickets") {
            headers {
                append(HttpHeaders.Authorization, "Bearer $token")
            }
        }.body()
    }

    // Obtener lista de negocios con nombre y total pago
    suspend fun obtenerNombresNegocios(token: String): List<NegocioResponse> {
        return client.post("$BASE_URL/enviadatosMercadoNombresNegocios") {
            headers {
                append(HttpHeaders.Authorization, "Bearer $token")
            }
        }.body()
    }

    // Enviar nuevo ticket al servidor
    suspend fun crearTicket(token: String, ticket: Ticket): Boolean {
        return try {
            val response: HttpResponse = client.post("$BASE_URL/recibeDatosMercadoTickets") {
                headers {
                    append(HttpHeaders.Authorization, "Bearer $token")
                }
                contentType(ContentType.Application.Json)
                setBody(ticket)
            }

            val body = response.bodyAsText()
            Log.d("API", "Respuesta servidor: ${response.status.value} -> $body")

            response.status.value in 200..299
        } catch (e: Exception) {
            Log.e("API", "Error enviando ticket: ${e.message}")
            false
        }
    }

}

// Modelo para la respuesta de negocios
@Serializable
data class NegocioResponse(
    @SerialName("Nombre_Negocio") val nombreNegocio: String,
    @SerialName("Total_Pago") val totalPago: String
)
