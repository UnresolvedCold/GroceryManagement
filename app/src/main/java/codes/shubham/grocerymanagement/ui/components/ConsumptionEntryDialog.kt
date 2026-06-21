package codes.shubham.grocerymanagement.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import codes.shubham.grocerymanagement.domain.model.Transaction
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsumptionEntryDialog(
    title: String,
    unit: String,
    entry: Transaction? = null,
    onConfirm: (Double, String?, LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    var quantity by remember(entry?.id) { mutableStateOf(entry?.quantity?.format().orEmpty()) }
    var notes by remember(entry?.id) { mutableStateOf(entry?.notes.orEmpty()) }
    var date by remember(entry?.id) { mutableStateOf(entry?.date ?: LocalDate.now()) }
    var error by remember(entry?.id) { mutableStateOf<String?>(null) }
    var showDatePicker by remember(entry?.id) { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.toEpochDay() * 86_400_000
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                Button(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        date = LocalDate.ofEpochDay(it / 86_400_000)
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it; error = null },
                    label = { Text("Quantity ($unit)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = error != null,
                    supportingText = error?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = date.format(DateTimeFormatter.ofPattern("d MMM yyyy")),
                    onValueChange = {},
                    label = { Text("Date") },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth()
                )
                TextButton(onClick = { showDatePicker = true }) { Text("Change date") }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val parsedQuantity = quantity.toDoubleOrNull()
                if (parsedQuantity == null || parsedQuantity <= 0) {
                    error = "Enter a valid quantity"
                    return@Button
                }
                onConfirm(parsedQuantity, notes.trim().takeIf { it.isNotEmpty() }, date)
            }) { Text(if (entry == null) "Add" else "Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun Double.format(): String =
    if (this == toLong().toDouble()) toLong().toString() else "%.2f".format(this)
