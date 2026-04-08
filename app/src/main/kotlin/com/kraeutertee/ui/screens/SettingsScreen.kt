@file:OptIn(ExperimentalMaterial3Api::class)

package com.kraeutertee.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.*
import com.kraeutertee.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: SettingsViewModel, onBack: () -> Unit) {
    val apiKey by vm.apiKey.collectAsState()
    val notifyHarvest by vm.notifyHarvest.collectAsState()
    val notifyDrying by vm.notifyDrying.collectAsState()
    val daysBefore by vm.daysBefore.collectAsState()

    var apiKeyInput by remember(apiKey) { mutableStateOf(apiKey) }
    var showKey by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Einstellungen") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Zurück") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── API Key ─────────────────────────────────────────────────────────
            item {
                SettingsSection(title = "🤖 KI-Assistent (Google Gemini)") {
                    Text(
                        "Um den KI-Kräuterexperten zu nutzen, benötigst du einen kostenlosen API-Schlüssel von aistudio.google.com.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it; saved = false },
                        label = { Text("Gemini API-Schlüssel") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (showKey) VisualTransformation.None
                        else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { showKey = !showKey }) {
                                Icon(
                                    if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    if (showKey) "Ausblenden" else "Anzeigen"
                                )
                            }
                        },
                        singleLine = true,
                        placeholder = { Text("AIza…") }
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        if (saved) {
                            Text("✓ Gespeichert", color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier
                                    .weight(1f)
                                    .align(Alignment.CenterVertically))
                        } else Spacer(Modifier.weight(1f))
                        Button(onClick = {
                            vm.saveApiKey(apiKeyInput.trim())
                            saved = true
                        }) { Text("Speichern") }
                    }
                }
            }

            // ── Notifications ────────────────────────────────────────────────────
            item {
                SettingsSection(title = "🔔 Benachrichtigungen") {
                    SettingsSwitchRow(
                        label = "Ernte-Erinnerungen",
                        description = "Benachrichtigung wenn Erntezeit naht",
                        checked = notifyHarvest,
                        onCheckedChange = vm::setNotifyHarvest
                    )
                    Spacer(Modifier.height(8.dp))
                    SettingsSwitchRow(
                        label = "Trocknungs-Erinnerungen",
                        description = "Benachrichtigung wenn Trocknung fertig",
                        checked = notifyDrying,
                        onCheckedChange = vm::setNotifyDrying
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("Tage vor Ernte erinnern:", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(1, 3, 5, 7, 14).forEach { days ->
                            FilterChip(
                                selected = daysBefore == days,
                                onClick = { vm.setDaysBefore(days) },
                                label = { Text("$days T") }
                            )
                        }
                    }
                }
            }

            // ── About ────────────────────────────────────────────────────────────
            item {
                SettingsSection(title = "ℹ️ Über die App") {
                    InfoRow("Version", "1.0.0")
                    InfoRow("Kräuter in der Datenbank", "20")
                    InfoRow("Karte", "OpenStreetMap (kein API-Key nötig)")
                    InfoRow("KI", "Google Gemini 1.5 Flash")
                    Spacer(Modifier.height(8.dp))
                    Text("Diese App wurde persönlich für dich als Kräuterliebhaber erstellt. Alle Daten bleiben lokal auf deinem Gerät.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // ── Disclaimer ─────────────────────────────────────────────────────
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(0.3f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("⚠️ Medizinischer Hinweis", style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text("Diese App ersetzt keine medizinische Beratung. Bei gesundheitlichen Beschwerden immer einen Arzt aufsuchen. Kräuterinformationen dienen der allgemeinen Information.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun SettingsSwitchRow(label: String, description: String, checked: Boolean,
                               onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(description, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}
