@file:OptIn(ExperimentalPermissionsApi::class)

package com.example.myapplication

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.*
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import java.util.concurrent.Executors

class MercadoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MercadoScreen()
        }
    }
}

@Serializable
data class Ticket(
    val N_Ticket: String,
    val Nombre_Negocio: String,
    val Fecha_Pago: String,
    val Total_Pago: String
)

object ApiClient {
    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    private const val BASE_URL = "http://192.168.18.5:3000"

    suspend fun obtenerTickets(): List<Ticket> {
        return client.get("$BASE_URL/enviadatosMercadoTickets").body()
    }
}

@Composable
fun MercadoScreen() {
    var tickets by remember { mutableStateOf<List<Ticket>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }
    var ticketSeleccionado by remember { mutableStateOf<Ticket?>(null) }
    var mostrarScanner by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        try {
            tickets = ApiClient.obtenerTickets()
        } catch (e: Exception) {
            Log.e("API", "Error al obtener datos: ${e.message}")
        } finally {
            cargando = false
        }
    }

    if (mostrarScanner) {
        QRScreen { qrResult ->
            Log.d("QR", "Resultado del QR: $qrResult")
            mostrarScanner = false
            ticketSeleccionado = tickets.find { it.N_Ticket == qrResult }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFAFAFA)),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
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
                    TablaTickets(
                        tickets = tickets,
                        seleccionado = ticketSeleccionado,
                        onSeleccionar = { ticketSeleccionado = it }
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.2f)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BotonEstilo("nuevo") {
                    Log.d("BOTÓN", "Agregar nuevo ticket (aún no implementado)")
                }

                BotonEstilo("Scan") {
                    mostrarScanner = true
                }

                BotonEstilo("Borrar") {
                    ticketSeleccionado?.let {
                        Log.d("BOTÓN", "Borrar ticket: ${it.N_Ticket}")
                    } ?: Log.d("BOTÓN", "Selecciona un ticket primero")
                }

                BotonEstilo("cargar") {
                    cargando = true
                    scope.launch {
                        try {
                            tickets = ApiClient.obtenerTickets()
                        } catch (e: Exception) {
                            Log.e("API", "Error al recargar: ${e.message}")
                        } finally {
                            cargando = false
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TablaTickets(
    tickets: List<Ticket>,
    seleccionado: Ticket?,
    onSeleccionar: (Ticket) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        // Encabezado
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF80142B))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("N_Ticket", "Nombre_Negocio", "Total_Pago", "Fecha_Pago").forEach {
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
        tickets.forEach { ticket ->
            val esSeleccionado = ticket == seleccionado

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
                    .background(if (esSeleccionado) Color(0xFFFAE3E8) else Color.Transparent)
                    .clickable { onSeleccionar(ticket) },
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf(
                    ticket.N_Ticket,
                    ticket.Nombre_Negocio,
                    ticket.Total_Pago,
                    ticket.Fecha_Pago
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

@Composable
fun QRScreen(onQrResult: (String) -> Unit) {
    val cameraPermission = rememberPermissionState(android.Manifest.permission.CAMERA)

    LaunchedEffect(Unit) {
        cameraPermission.launchPermissionRequest()
    }

    if (cameraPermission.status.isGranted) {
        QRScannerView(onQrScanned = onQrResult)
    } else {
        Text("Permiso de cámara denegado")
    }
}

@Composable
fun QRScannerView(onQrScanned: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val previewView = remember { PreviewView(context) }
    val scanner = BarcodeScanning.getClient()
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    AndroidView(
        factory = { previewView },
        modifier = Modifier.fillMaxSize()
    ) {
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                val mediaImage = imageProxy.image
                if (mediaImage != null) {
                    val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                    scanner.process(image)
                        .addOnSuccessListener { barcodes ->
                            for (barcode in barcodes) {
                                barcode.rawValue?.let {
                                    onQrScanned(it)
                                    cameraProvider.unbindAll()
                                }
                            }
                        }
                        .addOnFailureListener { Log.e("QR", "Error: ${it.message}") }
                        .addOnCompleteListener { imageProxy.close() }
                } else {
                    imageProxy.close()
                }
            }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis
                )
            } catch (e: Exception) {
                Log.e("QR", "Camera binding failed: ${e.message}")
            }
        }, ContextCompat.getMainExecutor(context))
    }
}
