package com.example.myapplication

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class MercadoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MercadoScreen()
        }
    }
}

@Serializable
data class Negocio(
    val ID_Negocio: String,
    val Nombre_Negocio: String,
    val Num_Local: String,
    val Tipo_Pago: String,
    val Total_Pago: String,
    val Zona: String
)

object ApiClient {
    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    private const val BASE_URL = "http://10.0.2.2:3000" // Cambia si es un celular real

    suspend fun obtenerNegocios(): List<Negocio> {
        return client.get("$BASE_URL/enviadatosMercadoNegocios").body()
    }
}

@Composable
fun MercadoScreen() {
    var negocios by remember { mutableStateOf<List<Negocio>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            negocios = ApiClient.obtenerNegocios()
        } catch (e: Exception) {
            Log.e("API", "Error al obtener datos: ${e.message}")
        } finally {
            cargando = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA)),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // ZONA DE TABLA
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.8f)
                .padding(16.dp)
                .background(Color.White, shape = RoundedCornerShape(12.dp))
                .shadow(4.dp, RoundedCornerShape(12.dp))
        ) {
            if (cargando) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                TablaZona(negocios)
            }
        }

        // BOTONERA
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.2f)
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BotonEstilo("nuevo")
            BotonEstilo("Editar")
            BotonEstilo("Borrar")
            BotonEstilo("cargar")
        }
    }
}

@Composable
fun TablaZona(negocios: List<Negocio>) {
    Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        // Encabezados
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF80142B))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("ID", "Nombre", "Local", "Pago", "Total", "Zona").forEach {
                Text(
                    text = it,
                    color = Color.White,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }

        // Filas de datos
        negocios.forEach { negocio ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf(
                    negocio.ID_Negocio,
                    negocio.Nombre_Negocio,
                    negocio.Num_Local,
                    negocio.Tipo_Pago,
                    negocio.Total_Pago,
                    negocio.Zona
                ).forEach {
                    Text(
                        text = it,
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun BotonEstilo(texto: String, onClick: () -> Unit = {}) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .width(80.dp)
            .height(40.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFF80142B)),
        shape = RoundedCornerShape(8.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
    ) {
        Text(text = texto, color = Color(0xFF80142B), fontSize = 12.sp)
    }
}
