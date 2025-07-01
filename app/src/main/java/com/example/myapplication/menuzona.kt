package com.example.myapplication  // ← Ajusta este package si es diferente

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MenuZonaScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Imagen arriba
        Image(
            painter = painterResource(id = R.drawable.palacio2),
            contentDescription = "Logo Apatzingán",
            modifier = Modifier
                .padding(top = 24.dp, bottom = 16.dp)
                .width(200.dp)
        )

        // Título
        Text(
            text = "Opciones de áreas de trabajo",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF80142B),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        )

        // Botones
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BotonZona("Área mercado")
            BotonZona("Área de Vías Públicas")
            BotonZona("Área de Alcoholes")
            BotonZona("Área de alcoholes")
            BotonZona("Área de tesorería")
        }
    }
}

@Composable
fun BotonZona(texto: String) {
    Button(
        onClick = { /* acción futura */ },
        modifier = Modifier
            .fillMaxWidth(0.8f),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF80142B))
    ) {
        Text(text = texto, color = Color.White)
    }
}
