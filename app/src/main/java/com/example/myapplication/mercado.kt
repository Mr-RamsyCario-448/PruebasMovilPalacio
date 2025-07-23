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
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.*
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import com.example.myapplication.utils.TokenManager
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.Executors
import com.example.myapplication.network.ApiService
import com.example.myapplication.network.ApiService.obtenerNombresNegocios
import java.text.SimpleDateFormat

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

@Serializable
data class NegocioResponse(
    @SerialName("Nombre_Negocio") val nombreNegocio: String,
    @SerialName("Total_Pago") val totalPago: String
)

@Composable
fun MercadoScreen() {
    var tickets by remember { mutableStateOf<List<Ticket>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }
    var ticketSeleccionado by remember { mutableStateOf<Ticket?>(null) }
    var mostrarScanner by remember { mutableStateOf(false) }
    var mostrandoFormulario by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        try {
            val token = TokenManager.obtenerToken(context)
            if (token != null) {
                tickets = ApiService.obtenerTickets(token)
            } else {
                Log.e("API", "No hay token guardado, inicia sesión primero.")
            }
        } catch (e: Exception) {
            Log.e("API", "Error al obtener datos: ${e.message}")
        } finally {
            cargando = false
        }
    }

    when {
        mostrarScanner -> {
            QRScreen { qrResult ->
                Log.d("QR", "Resultado del QR: $qrResult")
                mostrarScanner = false
                ticketSeleccionado = tickets.find { it.N_Ticket == qrResult }
            }
        }

        mostrandoFormulario -> {
            FormularioNuevoTicket(
                onGuardar = { nuevoTicket ->
                    tickets = tickets + nuevoTicket
                    mostrandoFormulario = false
                },
                onCancelar = { mostrandoFormulario = false }
            )
        }

        else -> {
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
                    BotonEstilo("Nuevo") { mostrandoFormulario = true }
                    BotonEstilo("Scan") { mostrarScanner = true }
                    BotonEstilo("Borrar") {
                        ticketSeleccionado?.let {
                            Log.d("BOTÓN", "Borrar ticket: ${it.N_Ticket}")
                        } ?: Log.d("BOTÓN", "Selecciona un ticket primero")
                    }
                    BotonEstilo("Cargar") {
                        cargando = true
                        scope.launch {
                            try {
                                val token = TokenManager.obtenerToken(context)
                                if (token != null) {
                                    tickets = ApiService.obtenerTickets(token)
                                } else {
                                    Log.e("API", "No hay token guardado.")
                                }
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
}

fun generateNumericTicket(length: Int = 12): String {
    return (1..length)
        .map { ('0'..'9').random() }
        .joinToString("")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormularioNuevoTicket(
    onGuardar: suspend (Ticket) -> Unit,
    onCancelar: () -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var nTicket by remember { mutableStateOf(generateNumericTicket()) }
    var nombreNegocio by remember { mutableStateOf("") }
    var totalPago by remember { mutableStateOf("") }

    var fechaPago by remember {
        mutableStateOf(
            SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
        )
    }

    var negocios by remember { mutableStateOf<List<NegocioResponse>>(emptyList()) }
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            val token = TokenManager.obtenerToken(context)
            if (token != null) {
                negocios = ApiService.obtenerNombresNegocios(token)
            } else {
                Log.e("API", "No hay token guardado.")
            }
        } catch (e: Exception) {
            Log.e("API", "Error al obtener negocios: ${e.message}")
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFAFAFA))
                .padding(8.dp)
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White, shape = RoundedCornerShape(24.dp))
                    .padding(24.dp)
                    .shadow(8.dp, shape = RoundedCornerShape(24.dp)),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Agregar Nuevo Ticket",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF80142B)
                )

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = nTicket,
                    onValueChange = {},
                    label = { Text("N° Ticket (autogenerado)") },
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(0.85f)
                )

                Spacer(modifier = Modifier.height(24.dp))

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier.fillMaxWidth(0.85f)
                ) {
                    OutlinedTextField(
                        value = nombreNegocio,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Nombre del Negocio") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier.menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        negocios.forEach { negocio ->
                            DropdownMenuItem(
                                text = { Text(negocio.nombreNegocio) },
                                onClick = {
                                    nombreNegocio = negocio.nombreNegocio
                                    totalPago = negocio.totalPago
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = totalPago,
                    onValueChange = {},
                    label = { Text("Total de Pago") },
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(0.85f)
                )

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = fechaPago,
                    onValueChange = { fechaPago = it },
                    label = { Text("Fecha de Pago") },
                    modifier = Modifier.fillMaxWidth(0.85f)
                )

                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth(0.85f)
                ) {
                    Button(
                        onClick = {
                            val ticket = Ticket(nTicket, nombreNegocio, fechaPago, totalPago)
                            scope.launch {
                                try {
                                    val token = TokenManager.obtenerToken(context)
                                    if (token == null) {
                                        snackbarHostState.showSnackbar("Error: no hay token de usuario.")
                                        return@launch
                                    }

                                    // 🚀 Llamamos a la API
                                    val exito = ApiService.crearTicket(token, ticket)

                                    if (exito) {
                                        snackbarHostState.showSnackbar("Ticket guardado con éxito")
                                        onGuardar(ticket) // ✅ devolvemos el ticket a la pantalla padre
                                    } else {
                                        snackbarHostState.showSnackbar("Error al guardar el ticket en el servidor")
                                    }
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("Error al guardar: ${e.message}")
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Guardar")
                    }


                    Button(
                        onClick = onCancelar,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancelar")
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
                listOf(ticket.N_Ticket, ticket.Nombre_Negocio, ticket.Total_Pago, ticket.Fecha_Pago).forEach {
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
        modifier = Modifier.width(80.dp).height(40.dp),
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
    LaunchedEffect(Unit) { cameraPermission.launchPermissionRequest() }
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

    AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize()) {
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
