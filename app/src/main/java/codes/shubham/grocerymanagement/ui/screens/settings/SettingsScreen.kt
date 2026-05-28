package codes.shubham.grocerymanagement.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "Back") }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SettingsSectionTitle("Alerts & Thresholds")
            }

            item {
                Card(shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = state.lowStockThreshold,
                            onValueChange = viewModel::onLowStockThresholdChange,
                            label = { Text("Low Stock Threshold") },
                            leadingIcon = { Icon(Icons.Default.WarningAmber, null, tint = MaterialTheme.colorScheme.tertiary) },
                            supportingText = { Text("Show alert when quantity is at or below this value") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = state.expiryWarningDays,
                            onValueChange = viewModel::onExpiryDaysChange,
                            label = { Text("Expiry Warning Days") },
                            leadingIcon = { Icon(Icons.Default.Schedule, null, tint = MaterialTheme.colorScheme.error) },
                            supportingText = { Text("Show expiry warning this many days before the expiry date") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            item {
                SettingsSectionTitle("Regressive Consumption")
            }

            item {
                Card(shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.AutoGraph, null, tint = MaterialTheme.colorScheme.primary)
                            Column(Modifier.weight(1f)) {
                                Text("Daily suggestions", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "Suggest inventory consumed today from recent consume history",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = state.regressiveConsumptionEnabled,
                                onCheckedChange = viewModel::onRegressiveConsumptionEnabledChange
                            )
                        }

                        OutlinedTextField(
                            value = state.regressiveConsumptionLookbackDays,
                            onValueChange = viewModel::onRegressiveLookbackDaysChange,
                            label = { Text("Lookback Days") },
                            leadingIcon = { Icon(Icons.Default.History, null) },
                            supportingText = { Text("Use the last this many days of consume entries") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            enabled = state.regressiveConsumptionEnabled,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = state.regressiveConsumptionReminderHour,
                                onValueChange = viewModel::onRegressiveReminderHourChange,
                                label = { Text("Hour") },
                                leadingIcon = { Icon(Icons.Default.Schedule, null) },
                                supportingText = { Text("0-23") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                enabled = state.regressiveConsumptionEnabled,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = state.regressiveConsumptionReminderMinute,
                                onValueChange = viewModel::onRegressiveReminderMinuteChange,
                                label = { Text("Minute") },
                                supportingText = { Text("0-59") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                enabled = state.regressiveConsumptionEnabled,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            item {
                SettingsSectionTitle("AI Photo Analysis")
            }

            item {
                Card(shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Text("Gemini AI", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        }
                        Text(
                            "Enter a Gemini API key to enable automatic product detail extraction from photos. " +
                            "Get a free key at ai.google.dev",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = state.geminiApiKey,
                            onValueChange = viewModel::onApiKeyChange,
                            label = { Text("Gemini API Key") },
                            leadingIcon = { Icon(Icons.Default.Key, null) },
                            trailingIcon = {
                                IconButton(onClick = viewModel::toggleApiKeyVisibility) {
                                    Icon(
                                        imageVector = if (state.showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (state.showApiKey) "Hide key" else "Show key"
                                    )
                                }
                            },
                            visualTransformation = if (state.showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            item {
                Button(
                    onClick = viewModel::save,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Save Settings")
                }
            }

            if (state.savedFeedback) {
                item {
                    Card(
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                            Text("Settings saved successfully", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
            }

            item {
                SettingsSectionTitle("About")
            }

            item {
                Card(shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SettingsInfoRow(icon = Icons.Default.Storage, label = "Storage", value = "Local only — no cloud sync")
                        SettingsInfoRow(icon = Icons.Default.Security, label = "Privacy", value = "No account required")
                        SettingsInfoRow(icon = Icons.Default.Info, label = "Version", value = "1.0.0")
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun SettingsInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
