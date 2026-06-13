package com.timalo.mobileevent.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.timalo.mobileevent.model.EventOccurrence
import com.timalo.mobileevent.ui.components.DateTimeField

/**
 * Revue des occurrences d'un événement multi-jours.
 * Chaque jour est éditable individuellement (date + heure de début et de fin).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiDayReviewScreen(
    occurrences: List<EventOccurrence>,
    submitting: Boolean,
    onUpdateOccurrence: (id: Long, startIso: String, endIso: String) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Événement multi-jours") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
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
        ) {
            Text(
                "${occurrences.size} occurrences détectées (une par jour). " +
                    "Vous pouvez ajuster chaque journée.",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(occurrences, key = { it.id }) { occ ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "Jour ${occ.id + 1}",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            DateTimeField(
                                label = "Début",
                                isoValue = occ.startDate,
                                onIsoChange = { newStart ->
                                    onUpdateOccurrence(occ.id, newStart, occ.endDate)
                                }
                            )
                            DateTimeField(
                                label = "Fin",
                                isoValue = occ.endDate,
                                onIsoChange = { newEnd ->
                                    onUpdateOccurrence(occ.id, occ.startDate, newEnd)
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    enabled = !submitting,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Annuler")
                }
                Button(
                    onClick = onConfirm,
                    enabled = !submitting,
                    modifier = Modifier.weight(1f)
                ) {
                    if (submitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Créer ${occurrences.size} événements")
                    }
                }
            }
        }
    }
}
