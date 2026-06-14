package com.timalo.mobileevent.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.timalo.mobileevent.data.preferences.AppPreferences
import com.timalo.mobileevent.model.Environment
import com.timalo.mobileevent.ui.components.EnvBadge
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    prefs: AppPreferences,
    onBack: () -> Unit,
    onLoggedOut: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val env by prefs.environmentFlow.collectAsState(initial = Environment.PREPROD)
    val storedKey by prefs.anthropicKeyFlow.collectAsState(initial = null)

    var apiKeyInput by remember(storedKey) { mutableStateOf(storedKey ?: "") }
    var keySaved by remember { mutableStateOf(false) }
    var pendingProdSwitch by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Réglages") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = { EnvBadge(env, modifier = Modifier.padding(end = 12.dp)) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Environnement", fontWeight = FontWeight.Bold)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                Environment.entries.forEachIndexed { index, e ->
                    SegmentedButton(
                        selected = env == e,
                        onClick = {
                            if (e == Environment.PROD && env != Environment.PROD) {
                                pendingProdSwitch = true
                            } else {
                                scope.launch { prefs.saveEnvironment(e) }
                            }
                        },
                        shape = SegmentedButtonDefaults.itemShape(index, Environment.entries.size)
                    ) {
                        Text(e.displayName)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Text("Clé API Anthropic", fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = apiKeyInput,
                onValueChange = {
                    apiKeyInput = it
                    keySaved = false
                },
                label = { Text("sk-ant-…") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    scope.launch {
                        prefs.saveAnthropicKey(apiKeyInput.trim())
                        keySaved = true
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Sauvegarder la clé")
            }
            if (keySaved) {
                Text("Clé enregistrée ✅", color = MaterialTheme.colorScheme.primary)
            }

            Spacer(Modifier.height(24.dp))

            OutlinedButton(
                onClick = {
                    scope.launch {
                        prefs.clearToken(prefs.getEnvironment())
                        onLoggedOut()
                    }
                },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Se déconnecter")
            }
        }
    }

    if (pendingProdSwitch) {
        AlertDialog(
            onDismissRequest = { pendingProdSwitch = false },
            title = { Text("Basculer en PRODUCTION ?") },
            text = {
                Text("Les événements seront créés sur ti-malo.fr (production). Confirmer ?")
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingProdSwitch = false
                    scope.launch { prefs.saveEnvironment(Environment.PROD) }
                }) { Text("Confirmer") }
            },
            dismissButton = {
                TextButton(onClick = { pendingProdSwitch = false }) { Text("Annuler") }
            }
        )
    }
}
