package com.timalo.mobileevent.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.timalo.mobileevent.ui.components.DateTimeField
import com.timalo.mobileevent.ui.components.EnvBadge
import com.timalo.mobileevent.ui.components.TypeSelector
import com.timalo.mobileevent.viewmodel.CreateEventViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun CreateEventScreen(
    viewModel: CreateEventViewModel,
    onOpenSettings: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val form = state.form
    val context = LocalContext.current

    LaunchedEffect(Unit) { viewModel.refreshEnv() }

    // URI temporaire pour la photo caméra
    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let(viewModel::onImageChange) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) cameraUri?.let(viewModel::onImageChange)
    }

    val cameraPermission = rememberPermissionState(android.Manifest.permission.CAMERA)

    fun launchCamera() {
        val photoFile = File(
            context.cacheDir.also { File(it, "images").mkdirs() },
            "images/cam_${System.currentTimeMillis()}.jpg"
        )
        val uri = FileProvider.getUriForFile(
            context,
            "com.timalo.mobileevent.fileprovider",
            photoFile
        )
        cameraUri = uri
        cameraLauncher.launch(uri)
    }

    // Bascule vers l'écran de revue multi-jours
    if (state.showMultiDayReview) {
        MultiDayReviewScreen(
            occurrences = state.occurrences,
            submitting = state.submitting,
            onUpdateOccurrence = viewModel::updateOccurrence,
            onConfirm = viewModel::confirmMultiDay,
            onCancel = viewModel::dismissMultiDayReview
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Nouvel événement")
                        Spacer(Modifier.height(0.dp))
                    }
                },
                actions = {
                    EnvBadge(state.env, modifier = Modifier.padding(end = 8.dp))
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Réglages")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = form.title,
                onValueChange = viewModel::onTitleChange,
                label = { Text("Titre *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Bouton IA
            OutlinedButton(
                onClick = viewModel::enrichWithAi,
                enabled = form.title.isNotBlank() && !state.aiLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.aiLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(18.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.height(0.dp))
                    Text("  Enrichissement…")
                } else {
                    Text("✨ Enrichir avec l'IA")
                }
            }

            OutlinedTextField(
                value = form.description,
                onValueChange = viewModel::onDescriptionChange,
                label = { Text("Description") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            )

            OutlinedTextField(
                value = form.location,
                onValueChange = viewModel::onLocationChange,
                label = { Text("Lieu") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            DateTimeField(
                label = "Date de début *",
                isoValue = form.startDate,
                onIsoChange = viewModel::onStartDateChange
            )

            DateTimeField(
                label = "Date de fin *",
                isoValue = form.endDate,
                onIsoChange = viewModel::onEndDateChange
            )

            OutlinedTextField(
                value = form.organizerName,
                onValueChange = viewModel::onOrganizerNameChange,
                label = { Text("Organisateur *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = form.organizerEmail,
                onValueChange = viewModel::onOrganizerEmailChange,
                label = { Text("Email organisateur *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = form.price,
                onValueChange = viewModel::onPriceChange,
                label = { Text("Prix") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = form.maxAttendees,
                onValueChange = viewModel::onMaxAttendeesChange,
                label = { Text("Places max") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = form.sourceUrl,
                onValueChange = viewModel::onSourceUrlChange,
                label = { Text("URL source") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            TypeSelector(selected = form.types, onToggle = viewModel::toggleType)

            // Image
            Text("Image", fontWeight = FontWeight.Medium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { galleryLauncher.launch("image/*") }) {
                    Text("Galerie")
                }
                OutlinedButton(onClick = {
                    if (cameraPermission.status.isGranted) {
                        launchCamera()
                    } else {
                        cameraPermission.launchPermissionRequest()
                    }
                }) {
                    Text("Caméra")
                }
            }
            // Lance la caméra dès que la permission vient d'être accordée
            LaunchedEffect(cameraPermission.status.isGranted) {
                // no-op : on attend l'action explicite de l'utilisateur
            }

            form.imageUri?.let { uri ->
                Box(modifier = Modifier.fillMaxWidth()) {
                    AsyncImage(
                        model = uri,
                        contentDescription = "Aperçu image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )
                }
                OutlinedButton(onClick = { viewModel.onImageChange(null) }) {
                    Text("Retirer l'image")
                }
            }

            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }
            state.successMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            state.multiDayResult?.let {
                Text(it, color = MaterialTheme.colorScheme.primary)
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = viewModel::submit,
                enabled = !state.submitting,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.submitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Créer l'événement")
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
