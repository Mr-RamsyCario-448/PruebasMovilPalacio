package com.example.myapplication.ui

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.myapplication.network.ApiLoginClient
import com.example.myapplication.utils.TokenManager
import kotlinx.coroutines.launch
import com.example.myapplication.R // Asegúrate de importar R para recursos

@Composable
fun LoginScreen(onLoginExitoso: () -> Unit) {
    val context = LocalContext.current
    var usuario by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var cargando by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Contenedor general centrado verticalmente
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Imagen arriba
        Image(
            painter = painterResource(id = R.drawable.palacio2),
            contentDescription = "Logo Palacio",
            modifier = Modifier
                .size(150.dp)
                .padding(bottom = 32.dp)
        )

        Text("Iniciar Sesión", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = usuario,
            onValueChange = { usuario = it },
            label = { Text("Usuario") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                cargando = true
                scope.launch {
                    val resultado = ApiLoginClient.login(usuario, password)
                    if (resultado != null) {
                        TokenManager.guardarToken(context, resultado.token)
                        Toast.makeText(context, "¡Login exitoso!", Toast.LENGTH_SHORT).show()
                        onLoginExitoso()
                    } else {
                        Toast.makeText(context, "Usuario o contraseña incorrectos", Toast.LENGTH_SHORT).show()
                    }
                    cargando = false
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = !cargando,
            shape = RoundedCornerShape(12.dp),
        ) {
            Text(if (cargando) "Cargando..." else "Ingresar")
        }
    }
}
