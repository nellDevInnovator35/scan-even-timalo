package com.timalo.mobileevent.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.timalo.mobileevent.viewmodel.CreateEventViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private val DISPLAY = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE)

private fun displayLabel(iso: String): String {
    val cal = CreateEventViewModel.parseLocalISO(iso) ?: return "Choisir"
    return DISPLAY.format(cal.time)
}

/**
 * Champ date+heure utilisant les pickers natifs Android.
 * La valeur est gérée en ISO local (yyyy-MM-dd'T'HH:mm:ss) sans offset UTC.
 */
@Composable
fun DateTimeField(
    label: String,
    isoValue: String,
    onIsoChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(modifier = modifier.fillMaxWidth()) {
        Text(label)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    val cal = CreateEventViewModel.parseLocalISO(isoValue)
                        ?: Calendar.getInstance()

                    DatePickerDialog(
                        context,
                        { _, year, month, day ->
                            val updated = (cal.clone() as Calendar).apply {
                                set(Calendar.YEAR, year)
                                set(Calendar.MONTH, month)
                                set(Calendar.DAY_OF_MONTH, day)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            // Ouvre ensuite le TimePicker pour compléter l'heure
                            TimePickerDialog(
                                context,
                                { _, hour, minute ->
                                    val full = (updated.clone() as Calendar).apply {
                                        set(Calendar.HOUR_OF_DAY, hour)
                                        set(Calendar.MINUTE, minute)
                                    }
                                    onIsoChange(CreateEventViewModel.toLocalISO(full))
                                },
                                updated.get(Calendar.HOUR_OF_DAY),
                                updated.get(Calendar.MINUTE),
                                true
                            ).show()
                        },
                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.MONTH),
                        cal.get(Calendar.DAY_OF_MONTH)
                    ).show()
                }
            ) {
                Text(if (isoValue.isBlank()) "Choisir date et heure" else displayLabel(isoValue))
            }
        }
    }
}
