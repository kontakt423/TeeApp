@file:OptIn(ExperimentalMaterial3Api::class)

package com.kraeutertee.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import com.kraeutertee.data.entities.DryingEntry
import com.kraeutertee.data.entities.HarvestReminder
import com.kraeutertee.util.HerbDatabase
import com.kraeutertee.viewmodel.CalendarViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(vm: CalendarViewModel) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("🗓 Ernte", "☀️ Trocknung")

    val latitude by vm.latitude.collectAsState()
    val reminders by vm.activeReminders.collectAsState()
    val dryingEntries by vm.activeDrying.collectAsState()
    val allDrying by vm.allDrying.collectAsState()

    var showHarvestDialog by remember { mutableStateOf(false) }
    var showDryingDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { vm.refreshLocation(context) }

    Column(modifier = Modifier.fillMaxSize()) {

        // ── Tab row ────────────────────────────────────────────────────────────
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { i, title ->
                Tab(selected = selectedTab == i,
                    onClick = { selectedTab = i },
                    text = { Text(title) })
            }
        }

        when (selectedTab) {

            // ── Harvest calendar ─────────────────────────────────────────────
            0 -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Climate zone info
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("📍", fontSize = 24.sp)
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text("Deine Klimazone",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(0.7f))
                                    Text(vm.climateZoneName(latitude),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    Text(vm.offsetDescription(latitude),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(0.8f))
                                }
                            }
                        }
                    }

                    // Monthly overview
                    item {
                        Text("Erntemonatsübersicht",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        MonthlyHarvestGrid(latitude = latitude)
                    }

                    // Active reminders
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Erinnerungen (${reminders.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold)
                            FilledTonalButton(
                                onClick = { showHarvestDialog = true },
                                modifier = Modifier.height(36.dp)
                            ) {
                                Icon(Icons.Default.Add, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Neu", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }

                    if (reminders.isEmpty()) {
                        item {
                            Text("Keine aktiven Erinnerungen.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    items(reminders, key = { it.id }) { reminder ->
                        HarvestReminderCard(
                            reminder = reminder,
                            onDelete = { vm.deleteReminder(reminder) }
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }

            // ── Drying calendar ───────────────────────────────────────────────
            1 -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Aktive Trocknungen (${dryingEntries.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold)
                            FilledTonalButton(
                                onClick = { showDryingDialog = true },
                                modifier = Modifier.height(36.dp)
                            ) {
                                Icon(Icons.Default.Add, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Neu", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }

                    if (dryingEntries.isEmpty()) {
                        item {
                            DryingTipsCard()
                        }
                    }

                    items(dryingEntries, key = { it.id }) { entry ->
                        DryingEntryCard(
                            entry = entry,
                            onComplete = { vm.completeDrying(entry) },
                            onDelete = { vm.deleteDrying(entry) }
                        )
                    }

                    // Completed history
                    val completed = allDrying.filter { it.isCompleted }
                    if (completed.isNotEmpty()) {
                        item {
                            Spacer(Modifier.height(8.dp))
                            Text("Abgeschlossen",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        items(completed.take(10), key = { "done-${it.id}" }) { entry ->
                            CompletedDryingCard(entry = entry, onDelete = { vm.deleteDrying(entry) })
                        }
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }

    // ── Add harvest reminder dialog ────────────────────────────────────────────
    if (showHarvestDialog) {
        AddHarvestReminderDialog(
            onConfirm = { herbId, herbName, date, notes ->
                vm.addReminder(
                    HarvestReminder(herbId = herbId, herbName = herbName,
                        reminderDate = date, notes = notes)
                )
                showHarvestDialog = false
            },
            onDismiss = { showHarvestDialog = false }
        )
    }

    // ── Add drying entry dialog ────────────────────────────────────────────────
    if (showDryingDialog) {
        AddDryingDialog(
            onConfirm = { entry -> vm.addDrying(entry); showDryingDialog = false },
            onDismiss = { showDryingDialog = false }
        )
    }
}

// ── Monthly harvest grid ───────────────────────────────────────────────────────
@Composable
private fun MonthlyHarvestGrid(latitude: Double) {
    val offset = when {
        latitude > 60 -> 2; latitude > 54 -> 1; latitude > 48 -> 0; latitude > 42 -> -1; else -> -2
    }
    val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        (1..12).chunked(3).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                row.forEach { month ->
                    val herbsThisMonth = HerbDatabase.all.filter { herb ->
                        val adjusted = herb.harvestMonths.map { m ->
                            ((m - 1 + offset + 12) % 12) + 1
                        }
                        month in adjusted
                    }
                    val isCurrent = month == currentMonth
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = when {
                                isCurrent -> MaterialTheme.colorScheme.primary
                                herbsThisMonth.isNotEmpty() -> MaterialTheme.colorScheme.primaryContainer
                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            }
                        ),
                        border = if (isCurrent) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                monthName(month),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isCurrent) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                            if (herbsThisMonth.isNotEmpty()) {
                                Text(
                                    herbsThisMonth.take(3).joinToString("") { it.emoji },
                                    fontSize = 10.sp,
                                    textAlign = TextAlign.Center
                                )
                                if (herbsThisMonth.size > 3)
                                    Text("+${herbsThisMonth.size - 3}",
                                        fontSize = 8.sp,
                                        color = if (isCurrent) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                if (row.size < 3) repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

// ── Harvest reminder card ──────────────────────────────────────────────────────
@Composable
private fun HarvestReminderCard(reminder: HarvestReminder, onDelete: () -> Unit) {
    val sdf = SimpleDateFormat("dd. MMMM yyyy", Locale.GERMAN)
    val herb = HerbDatabase.getById(reminder.herbId)
    val daysLeft = ((reminder.reminderDate - System.currentTimeMillis()) / 86400000).toInt()

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                daysLeft < 0 -> Color(0xFFFFEBEE)
                daysLeft < 3 -> Color(0xFFFFF3E0)
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(herb?.emoji ?: "🌿", fontSize = 28.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(reminder.herbName, fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium)
                Text(sdf.format(Date(reminder.reminderDate)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (reminder.notes.isNotBlank())
                    Text(reminder.notes, style = MaterialTheme.typography.labelSmall)
                val label = when {
                    daysLeft < 0 -> "Überfällig (${-daysLeft}d)"
                    daysLeft == 0 -> "Heute!"
                    daysLeft == 1 -> "Morgen"
                    else -> "In $daysLeft Tagen"
                }
                SuggestionChip(
                    onClick = {},
                    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.height(24.dp)
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, null,
                    tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
            }
        }
    }
}

// ── Drying entry card ──────────────────────────────────────────────────────────
@Composable
private fun DryingEntryCard(
    entry: DryingEntry,
    onComplete: () -> Unit,
    onDelete: () -> Unit
) {
    val herb = HerbDatabase.getById(entry.herbId)
    val elapsed = ((System.currentTimeMillis() - entry.startDate) / 86400000).toInt()
    val progress = (elapsed.toFloat() / entry.expectedDays).coerceIn(0f, 1f)
    val isDone = elapsed >= entry.expectedDays
    val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.GERMAN)
    val endDate = Date(entry.startDate + entry.expectedDays * 86400000L)

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDone) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(herb?.emoji ?: "🌿", fontSize = 26.sp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(entry.herbName, fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium)
                    Text("${entry.dryingMethod} · ${entry.quantity}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (isDone) {
                    FilledTonalButton(
                        onClick = onComplete,
                        modifier = Modifier.height(32.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color(0xFF4CAF50)
                        )
                    ) { Text("Fertig ✓", style = MaterialTheme.typography.labelSmall,
                        color = Color.White) }
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, null,
                        tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = if (isDone) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("$elapsed / ${entry.expectedDays} Tage",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(if (isDone) "Fertig seit ${elapsed - entry.expectedDays}d"
                    else "Fertig am ${sdf.format(endDate)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isDone) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun CompletedDryingCard(entry: DryingEntry, onDelete: () -> Unit) {
    val herb = HerbDatabase.getById(entry.herbId)
    val sdf = SimpleDateFormat("dd.MM.yy", Locale.GERMAN)
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(herb?.emoji ?: "🌿", fontSize = 20.sp)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(entry.herbName, style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium)
                Text("${entry.dryingMethod} · ${entry.expectedDays}d · ${sdf.format(Date(entry.startDate))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("✓", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
            IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Delete, null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f),
                    modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
private fun DryingTipsCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("☀️ Tipps zur Kräutertrocknung",
                style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            listOf(
                "🌬 Luftige, schattige Orte sind ideal",
                "🌡 Max. 40°C – ätherische Öle erhalten",
                "🔄 Täglich wenden für gleichmäßige Trocknung",
                "🫙 Kühl, dunkel und trocken lagern",
                "📅 Datum der Ernte immer notieren"
            ).forEach { tip ->
                Text("• $tip", style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 2.dp))
            }
        }
    }
}

// ── Add harvest reminder dialog ────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddHarvestReminderDialog(
    onConfirm: (herbId: String, herbName: String, date: Long, notes: String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedHerbId by remember { mutableStateOf(HerbDatabase.all.first().id) }
    var notes by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis() + 7 * 86400000L
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("OK") }
            }
        ) { DatePicker(state = datePickerState) }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(20.dp)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("🗓 Ernte-Erinnerung",
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))

                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    val selected = HerbDatabase.getById(selectedHerbId)
                    OutlinedTextField(
                        value = "${selected?.emoji} ${selected?.name}",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Kraut") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        HerbDatabase.all.forEach { h ->
                            DropdownMenuItem(
                                text = { Text("${h.emoji} ${h.name}") },
                                onClick = { selectedHerbId = h.id; expanded = false }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))

                val sdf = SimpleDateFormat("dd. MMMM yyyy", Locale.GERMAN)
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CalendarMonth, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(sdf.format(Date(datePickerState.selectedDateMillis
                        ?: System.currentTimeMillis() + 7 * 86400000L)))
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = notes, onValueChange = { notes = it },
                    label = { Text("Notizen") }, modifier = Modifier.fillMaxWidth(), minLines = 2
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) { Text("Abbrechen") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        val herb = HerbDatabase.getById(selectedHerbId)!!
                        onConfirm(
                            selectedHerbId, herb.name,
                            datePickerState.selectedDateMillis ?: System.currentTimeMillis(),
                            notes
                        )
                    }) { Text("Speichern") }
                }
            }
        }
    }
}

// ── Add drying dialog ──────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddDryingDialog(
    onConfirm: (DryingEntry) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedHerbId by remember { mutableStateOf(HerbDatabase.all.first().id) }
    var method by remember { mutableStateOf("Lufttrocknung") }
    var qty by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var methodExpanded by remember { mutableStateOf(false) }
    val methods = listOf("Lufttrocknung", "Dörrgerät", "Backofen", "Mikrowelle", "Gefriertrocknung")

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(20.dp)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("☀️ Trocknung starten",
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))

                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    val selected = HerbDatabase.getById(selectedHerbId)
                    OutlinedTextField(
                        value = "${selected?.emoji} ${selected?.name}",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Kraut") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        HerbDatabase.all.forEach { h ->
                            DropdownMenuItem(
                                text = { Text("${h.emoji} ${h.name}") },
                                onClick = { selectedHerbId = h.id; expanded = false }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))

                ExposedDropdownMenuBox(expanded = methodExpanded, onExpandedChange = { methodExpanded = it }) {
                    OutlinedTextField(
                        value = method, onValueChange = {}, readOnly = true,
                        label = { Text("Methode") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(methodExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = methodExpanded, onDismissRequest = { methodExpanded = false }) {
                        methods.forEach { m ->
                            DropdownMenuItem(text = { Text(m) }, onClick = { method = m; methodExpanded = false })
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = qty, onValueChange = { qty = it },
                    label = { Text("Menge (z.B. '200g' oder 'großes Bündel')") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = notes, onValueChange = { notes = it },
                    label = { Text("Notizen") }, modifier = Modifier.fillMaxWidth(), minLines = 2
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) { Text("Abbrechen") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        val herb = HerbDatabase.getById(selectedHerbId)!!
                        onConfirm(
                            DryingEntry(
                                herbId = selectedHerbId,
                                herbName = herb.name,
                                expectedDays = herb.dryingDays,
                                dryingMethod = method,
                                quantity = qty,
                                notes = notes
                            )
                        )
                    }) { Text("Starten") }
                }
            }
        }
    }
}
