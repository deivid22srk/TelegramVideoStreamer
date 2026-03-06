package com.deivid.telegramvideo.ui.setup

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun SetupScreen(onNavigateToLogin: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("telegram_prefs", Context.MODE_PRIVATE) }

    var apiId by remember { mutableStateOf("") }
    var apiHash by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        if (prefs.contains("api_id") && prefs.contains("api_hash")) {
            onNavigateToLogin()
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Configuração Inicial",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            OutlinedTextField(
                value = apiId,
                onValueChange = { apiId = it },
                label = { Text("API ID") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )

            OutlinedTextField(
                value = apiHash,
                onValueChange = { apiHash = it },
                label = { Text("API HASH") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            )

            Button(
                onClick = {
                    if (apiId.isBlank() || apiHash.isBlank()) {
                        Toast.makeText(context, "Preencha todos os campos", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    prefs.edit()
                        .putInt("api_id", apiId.toIntOrNull() ?: 0)
                        .putString("api_hash", apiHash)
                        .apply()

                    Toast.makeText(context, "Configurações salvas!", Toast.LENGTH_SHORT).show()
                    onNavigateToLogin()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Salvar")
            }
        }
    }
}
