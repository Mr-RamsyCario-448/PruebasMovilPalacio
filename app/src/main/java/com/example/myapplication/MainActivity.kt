package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.ui.LoginScreen
import com.example.myapplication.utils.TokenManager
import com.example.myapplication.network.ApiLoginClient
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApp()
        }
    }
}

@Composable
fun MyApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    var startDestination by remember { mutableStateOf("login") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val token = TokenManager.obtenerToken(context)
        if (!token.isNullOrEmpty() && ApiLoginClient.verificarToken(token)) {
            startDestination = "menu"
        }
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable("login") {
            LoginScreen(onLoginExitoso = {
                navController.navigate("menu") {
                    popUpTo("login") { inclusive = true }
                }
            })
        }
        composable("menu") {
            MenuZonaScreen(navController)
        }
        composable("mercado") {
            MercadoScreen()
        }
    }
}
