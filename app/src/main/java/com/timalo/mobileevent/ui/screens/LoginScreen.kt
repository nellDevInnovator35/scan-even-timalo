package com.timalo.mobileevent.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.timalo.mobileevent.model.Environment
import com.timalo.mobileevent.ui.components.EnvBadge
import com.timalo.mobileevent.viewmodel.LoginViewModel

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onLoggedIn: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var pendingProdSwitch by remember { mutableStateOf(false) }

    LaunchedEffect(state.loggedIn) {
        if (state.loggedIn) onLoggedIn()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Ti-Malo Event",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            EnvBadge(state.env)
        }

        Spacer(Modifier.height(24.dp))

        // Sélecteur d'environnement
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            Environment.entries.forEachIndexed { index, env ->
                SegmentedButton(
                    selected = state.env == env,
                    onClick = {
                        if (env == Environment.PROD && state.env != Environment.PROD) {
                            pendingProdSwitch = true
                        } else {
                            viewModel.onEnvChange(env)
                        }
                    },
                    shape = SegmentedButtonDefaults.itemShape(index, Environment.entries.size)
                ) {
                    Text(env.displayName)
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = state.email,
            onValueChange = viewModel::onEmailChange,
            label = { Text("Email") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = state.password,
            onValueChange = viewModel::onPasswordChange,
            label = { Text("Mot de passe") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        state.error?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = viewModel::login,
            enabled = !state.loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.loading) {
                CircularProgressIndicator(
                    modifier = Modifier.height(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Se connecter")
            }
        }
    }

    if (pendingProdSwitch) {
        AlertDialog(
            onDismissRequest = { pendingProdSwitch = false },
            title = { Text("Basculer en PRODUCTION ?") },
            text = {
                Text(
                    "Vous allez créer des événements sur le site de PRODUCTION " +
                        "(ti-malo.fr). Confirmez-vous ?"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingProdSwitch = false
                    viewModel.onEnvChange(Environment.PROD)
                }) { Text("Confirmer") }
            },
            dismissButton = {
                TextButton(onClick = { pendingProdSwitch = false }) { Text("Annuler") }
            }
        )
    }
}
