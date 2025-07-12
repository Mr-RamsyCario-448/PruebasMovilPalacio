package com.example.myapplication.network

import android.util.Log
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object ApiLoginClient {
    private const val BASE_URL = "http://192.168.18.5:5000" // ← si usas emulador, usa 10.0.2.2

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    @Serializable
    data class LoginResponse(val token: String, val usuario: String)

    suspend fun login(usuario: String, password: String): LoginResponse? {
        return try {
            client.post("$BASE_URL/buscarUsuario") {  // <-- Cambio GET por POST
                contentType(ContentType.Application.Json)
                setBody(mapOf("usuario" to usuario, "password" to password))
            }.body()
        } catch (e: Exception) {
            Log.e("LOGIN", "Error: ${e.message}")
            null
        }
    }

    suspend fun verificarToken(token: String): Boolean {
        return try {
            val response = client.get("$BASE_URL/verificar-token") {
                headers {
                    append(HttpHeaders.Authorization, "Bearer $token")
                }
            }
            response.status == HttpStatusCode.OK
        } catch (e: Exception) {
            false
        }
    }
}
