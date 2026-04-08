@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.kraeutertee.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kraeutertee.data.entities.CustomHerbEntity
import com.kraeutertee.data.entities.HerbNote
import com.kraeutertee.util.HerbDatabase
import com.kraeutertee.util.HerbInfo
import com.kraeutertee.viewmodel.CustomHerbsViewModel
import com.kraeutertee.viewmodel.HerbsViewModel
import com.kraeutertee.viewmodel.toHerbInfo

// ── Overview screen ────────────────────────────────────────────────────────────
@Composable
fun HerbsScreen(
    vm: HerbsViewModel,
    customVm: CustomHerbsViewModel,
    onNavigateToDetail: (String) -> Unit
) {
    val searchQuery      by vm.searchQuery.collectAsState()
    val selectedCategory by vm.selectedCategory.collectAsState()
    val filteredHerbs    by vm.filteredHerbs.collectAsState()
    val gardenFilter     by vm.gardenFilter.collectAsState()
    val allCategories    by vm.allCategories.collectAsState()
    val isGenerating     by customVm.isGenerating.collectAsState()

    var showAddCustomDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Search bar ──────────────────────────────────────────────────────
            OutlinedTextField(
                value = searchQuery, onValueChange = vm::setSearch,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Kräuter suchen…") },
                leadingIcon  = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty())
                        IconButton(onClick = { vm.setSearch("") }) { Icon(Icons.Default.Clear, null) }
                },
                singleLine = true, shape = RoundedCornerShape(28.dp)
            )

            // ── Category chips ──────────────────────────────────────────────────
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedCategory == null && !gardenFilter,
                        onClick  = { vm.setCategory(null); vm.setGardenFilter(false) },
                        label    = { Text("Alle") },
                        leadingIcon = if (selectedCategory == null && !gardenFilter)
                            {{ Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }} else null
                    )
                }
                item {
                    FilterChip(
                        selected = gardenFilter,
                        onClick  = { vm.setGardenFilter(!gardenFilter) },
                        label    = { Text("🌱 Mein Garten") },
                        leadingIcon = if (gardenFilter)
                            {{ Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }} else null
                    )
                }
                items(allCategories) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick  = { vm.setCategory(if (selectedCategory == cat) null else cat) },
                        label    = { Text(cat) },
                        leadingIcon = if (selectedCategory == cat)
                            {{ Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }} else null
                    )
                }
            }

            // ── Herb grid ───────────────────────────────────────────────────────
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement   = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredHerbs, key = { it.id }) { herb ->
                    val note by vm.getNoteFor(herb.id).collectAsState(initial = null)
                    HerbCard(
                        herb            = herb,
                        note            = note,
                        isCustom        = vm.isCustomHerb(herb.id),
                        onClick         = { onNavigateToDetail(herb.id) },
                        onToggleFavorite = { vm.toggleFavorite(herb.id, note) },
                        onToggleGarden   = { vm.toggleGarden(herb.id, note) },
                        onDelete         = if (vm.isCustomHerb(herb.id)) {
                            { vm.deleteCustomHerb(herb.id) }
                        } else null
                    )
                }
            }
        }

        // ── FAB: add new herb ───────────────────────────────────────────────────
        FloatingActionButton(
            onClick = { showAddCustomDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .zIndex(10f),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            if (isGenerating)
                CircularProgressIndicator(modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
            else {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Add, null)
                    Text("Kraut hinzufügen", style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }

    // ── Add custom herb dialog ──────────────────────────────────────────────────
    if (showAddCustomDialog) {
        AddCustomHerbDialog(
            customVm = customVm,
            onDismiss = { showAddCustomDialog = false },
            onCreated = { herbId ->
                showAddCustomDialog = false
                onNavigateToDetail(herbId)
            }
        )
    }
}

// ── Herb card ──────────────────────────────────────────────────────────────────
@Composable
private fun HerbCard(
    herb: HerbInfo,
    note: HerbNote?,
    isCustom: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleGarden: () -> Unit,
    onDelete: (() -> Unit)?
) {
    val isFav    = note?.isFavorite == true
    val isGarden = note?.isInGarden == true

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isGarden -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                isCustom -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                else     -> MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Text(herb.emoji, fontSize = 32.sp, modifier = Modifier.weight(1f))
                if (isCustom) {
                    // Show delete for custom herbs
                    if (onDelete != null) {
                        IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.DeleteOutline, "Löschen",
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp))
                        }
                    }
                }
                IconButton(onClick = onToggleFavorite, modifier = Modifier.size(28.dp)) {
                    Icon(
                        if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        "Favorit",
                        tint = if (isFav) Color(0xFFE53935) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (isCustom) {
                Text("✨ Eigenes Kraut", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(bottom = 2.dp))
            }

            Text(herb.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(herb.latinName, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(6.dp))
            Text(herb.shortDescription, style = MaterialTheme.typography.bodySmall,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                SuggestionChip(onClick = {},
                    label = { Text(herb.category, style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.height(24.dp))
                IconButton(onClick = onToggleGarden, modifier = Modifier.size(28.dp)) {
                    Text(if (isGarden) "🌱" else "➕", fontSize = 14.sp)
                }
            }
        }
    }
}

// ── Detail Screen ─────────────────────────────────────────────────────────────
@Composable
fun HerbDetailScreen(
    herbId: String,
    vm: HerbsViewModel,
    customVm: CustomHerbsViewModel,
    onBack: () -> Unit
) {
    var herb by remember { mutableStateOf(HerbDatabase.getById(herbId)) }
    val isCustom = vm.isCustomHerb(herbId)

    // Load custom herb if it's not in the built-in DB
    LaunchedEffect(herbId) {
        if (herb == null && isCustom) {
            val entity = customVm.getCustomHerbById(herbId)
            herb = entity?.let {
                it.toHerbInfo()
            }
        }
    }

    val currentHerb = herb
    if (currentHerb == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val note          by vm.getNoteFor(herbId).collectAsState(initial = null)
    val aiExplanation by vm.aiExplanation.collectAsState()
    val isLoading     by vm.isLoading.collectAsState()

    var showEditDialog    by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(herbId) { vm.loadAiExplanation(currentHerb) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${currentHerb.emoji} ${currentHerb.name}") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Zurück") }
                },
                actions = {
                    if (isCustom) {
                        IconButton(onClick = { showEditDialog = true }) {
                            Icon(Icons.Default.Edit, "Bearbeiten")
                        }
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Default.Delete, "Löschen",
                                tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    IconButton(onClick = { vm.toggleFavorite(herbId, note) }) {
                        Icon(
                            if (note?.isFavorite == true) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            "Favorit",
                            tint = if (note?.isFavorite == true) Color(0xFFE53935)
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { vm.toggleGarden(herbId, note) }) {
                        Text(if (note?.isInGarden == true) "🌱" else "➕", fontSize = 18.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isCustom) {
                item {
                    SuggestionChip(onClick = {},
                        label = { Text("✨ Eigenes Kraut") },
                        icon  = { Icon(Icons.Default.Star, null, Modifier.size(14.dp)) })
                }
            }

            item { InfoChipRow(currentHerb) }

            item {
                DetailCard("🌿 Ernte", Icons.Default.ContentCut) {
                    LabeledRow("Teil:", currentHerb.harvestPart)
                    LabeledRow("Monate:", currentHerb.harvestMonths.joinToString(", ") { monthName(it) })
                    if (currentHerb.harvestTips.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(currentHerb.harvestTips, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            item {
                DetailCard("☀️ Trocknung", Icons.Default.WbSunny) {
                    LabeledRow("Dauer:", "${currentHerb.dryingDays} Tage")
                    LabeledRow("Max. Temp:", "${currentHerb.dryingTempMax}°C")
                    LabeledRow("Lagerung:", "${currentHerb.storageMonths} Monate")
                    if (currentHerb.dryingMethod.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(currentHerb.dryingMethod, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            item {
                DetailCard("☕ Teezubereitung", Icons.Default.LocalCafe) {
                    LabeledRow("Temperatur:", "${currentHerb.brewingTempC}°C")
                    LabeledRow("Ziehzeit:", "${currentHerb.brewingMinutes} Min.")
                    if (currentHerb.teaInfo.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(currentHerb.teaInfo, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            if (currentHerb.effects.isNotEmpty()) {
                item {
                    DetailCard("✨ Wirkungen", Icons.Default.AutoAwesome) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            currentHerb.effects.forEach { effect ->
                                SuggestionChip(onClick = {}, label = { Text(effect) })
                            }
                        }
                    }
                }
            }

            if (currentHerb.warningsDE.isNotBlank()) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                        shape = RoundedCornerShape(12.dp)) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                            Icon(Icons.Default.Warning, null, tint = Color(0xFFF57C00),
                                modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text("Hinweis", fontWeight = FontWeight.Bold, color = Color(0xFF7F4800))
                                Text(currentHerb.warningsDE, style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF7F4800))
                            }
                        }
                    }
                }
            }

            // KI explanation
            item {
                DetailCard("🤖 KI-Erklärung", Icons.Default.SmartToy) {
                    if (isLoading) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Gemini erklärt…", style = MaterialTheme.typography.bodySmall)
                        }
                    } else if (aiExplanation.isNotBlank()) {
                        Text(aiExplanation, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = { vm.loadAiExplanation(currentHerb) }) {
                            Icon(Icons.Default.Refresh, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp)); Text("Neu laden")
                        }
                    } else {
                        OutlinedButton(onClick = { vm.loadAiExplanation(currentHerb) }) {
                            Icon(Icons.Default.SmartToy, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp)); Text("KI-Erklärung laden")
                        }
                    }
                }
            }

            // Personal notes
            item {
                val currentNotes = note?.personalNotes ?: ""
                var editNotes by remember { mutableStateOf(currentNotes) }
                var editing   by remember { mutableStateOf(false) }

                DetailCard("📝 Persönliche Notizen", Icons.Default.Edit) {
                    if (editing) {
                        OutlinedTextField(value = editNotes, onValueChange = { editNotes = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Deine Beobachtungen, Fundorte, Erfahrungen…") },
                            minLines = 3)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                            TextButton(onClick = { editing = false }) { Text("Abbrechen") }
                            Spacer(Modifier.width(8.dp))
                            Button(onClick = { vm.saveNotes(herbId, editNotes, note); editing = false }) {
                                Text("Speichern")
                            }
                        }
                    } else {
                        if (currentNotes.isNotBlank()) {
                            Text(currentNotes, style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(8.dp))
                        } else {
                            Text("Noch keine Notizen.", color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(4.dp))
                        }
                        TextButton(onClick = { editNotes = currentNotes; editing = true }) {
                            Icon(Icons.Default.Edit, null, Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp)); Text("Notiz bearbeiten")
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    // Edit dialog for custom herbs
    if (showEditDialog && isCustom) {
        EditCustomHerbDialog(
            herbId   = herbId,
            customVm = customVm,
            onDismiss = { showEditDialog = false },
            onSaved   = { showEditDialog = false }
        )
    }

    // Delete confirm
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Kraut löschen?") },
            text  = { Text("\"${currentHerb.name}\" wird dauerhaft gelöscht.") },
            confirmButton = {
                TextButton(
                    onClick = { vm.deleteCustomHerb(herbId); showDeleteConfirm = false; onBack() },
                    colors  = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Löschen") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Abbrechen") } }
        )
    }
}

// ── Add custom herb dialog (KI-powered) ───────────────────────────────────────
@Composable
private fun AddCustomHerbDialog(
    customVm: CustomHerbsViewModel,
    onDismiss: () -> Unit,
    onCreated: (String) -> Unit
) {
    var mode by remember { mutableStateOf("ki") }   // "ki" or "manual"
    var herbName by remember { mutableStateOf("") }
    val isGenerating by customVm.isGenerating.collectAsState()
    val error by customVm.generationError.collectAsState()
    var manualEntity by remember { mutableStateOf<CustomHerbEntity?>(null) }

    Dialog(onDismissRequest = { if (!isGenerating) onDismiss() }) {
        Card(shape = RoundedCornerShape(20.dp)) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("🌿 Neues Kraut hinzufügen",
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                // Mode switcher
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = mode == "ki",     onClick = { mode = "ki" },
                        label = { Text("✨ Mit Gemini KI") })
                    FilterChip(selected = mode == "manual", onClick = { mode = "manual" },
                        label = { Text("📝 Manuell") })
                }

                if (mode == "ki") {
                    // KI mode
                    Text("Gib den Kräuternamen ein und Gemini erstellt automatisch alle Details.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)

                    OutlinedTextField(
                        value = herbName, onValueChange = { herbName = it },
                        label = { Text("Kräutername *") },
                        placeholder = { Text("z.B. Bärlauch, Eisenkraut, Weißdorn…") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !isGenerating
                    )

                    if (error.isNotBlank()) {
                        Card(colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer)) {
                            Text(error, modifier = Modifier.padding(10.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = onDismiss, enabled = !isGenerating) { Text("Abbrechen") }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                customVm.generateAndSave(
                                    herbName = herbName.trim(),
                                    onSuccess = onCreated,
                                    onError   = { /* error shown via StateFlow */ }
                                )
                            },
                            enabled = herbName.isNotBlank() && !isGenerating
                        ) {
                            if (isGenerating) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                                Spacer(Modifier.width(8.dp))
                                Text("Gemini generiert…")
                            } else {
                                Icon(Icons.Default.AutoAwesome, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Generieren & speichern")
                            }
                        }
                    }
                } else {
                    // Manual mode – minimal fields
                    ManualHerbForm(
                        onSave = { entity ->
                            customVm.saveCustomHerb(entity)
                            onCreated(entity.id)
                        },
                        onDismiss = onDismiss
                    )
                }
            }
        }
    }
}

// ── Manual herb form (used in both Add and Edit) ───────────────────────────────
@Composable
private fun ManualHerbForm(
    initial: CustomHerbEntity? = null,
    onSave: (CustomHerbEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var name         by remember { mutableStateOf(initial?.name ?: "") }
    var latinName    by remember { mutableStateOf(initial?.latinName ?: "") }
    var emoji        by remember { mutableStateOf(initial?.emoji ?: "🌿") }
    var description  by remember { mutableStateOf(initial?.shortDescription ?: "") }
    var harvestPart  by remember { mutableStateOf(initial?.harvestPart ?: "Blätter") }
    var harvestTips  by remember { mutableStateOf(initial?.harvestTips ?: "") }
    var effects      by remember { mutableStateOf(
        if (initial != null) {
            try { Gson().fromJson<List<String>>(
                initial.effectsJson,
                object : TypeToken<List<String>>() {}.type
            ).joinToString(", ") } catch (_: Exception) { "" }
        } else ""
    ) }
    var teaInfo      by remember { mutableStateOf(initial?.teaInfo ?: "") }
    var category     by remember { mutableStateOf(initial?.category ?: "Sonstige") }
    var warningsDE   by remember { mutableStateOf(initial?.warningsDE ?: "") }
    var catExpanded  by remember { mutableStateOf(false) }
    val categories = listOf("Beruhigung","Erkältung","Verdauung","Schlaf",
        "Immunsystem","Entgiftung","Kreislauf","Stimmung","Sonstige")

    val parts = listOf("Blätter","Blüten","Wurzel","Samen","Früchte","Rinde","Triebe")
    var partExpanded by remember { mutableStateOf(false) }

    OutlinedTextField(value = name, onValueChange = { name = it },
        label = { Text("Name *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value = emoji, onValueChange = { if (it.length <= 2) emoji = it },
            label = { Text("Emoji") }, modifier = Modifier.width(80.dp), singleLine = true)
        OutlinedTextField(value = latinName, onValueChange = { latinName = it },
            label = { Text("Lateinisch") }, modifier = Modifier.weight(1f), singleLine = true)
    }
    OutlinedTextField(value = description, onValueChange = { description = it },
        label = { Text("Kurzbeschreibung") }, modifier = Modifier.fillMaxWidth(), minLines = 2)

    ExposedDropdownMenuBox(expanded = catExpanded, onExpandedChange = { catExpanded = it }) {
        OutlinedTextField(value = category, onValueChange = {}, readOnly = true,
            label = { Text("Kategorie") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(catExpanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth())
        ExposedDropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
            categories.forEach { c ->
                DropdownMenuItem(text = { Text(c) }, onClick = { category = c; catExpanded = false })
            }
        }
    }

    ExposedDropdownMenuBox(expanded = partExpanded, onExpandedChange = { partExpanded = it }) {
        OutlinedTextField(value = harvestPart, onValueChange = {}, readOnly = true,
            label = { Text("Ernte-Teil") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(partExpanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth())
        ExposedDropdownMenu(expanded = partExpanded, onDismissRequest = { partExpanded = false }) {
            parts.forEach { p ->
                DropdownMenuItem(text = { Text(p) }, onClick = { harvestPart = p; partExpanded = false })
            }
        }
    }

    OutlinedTextField(value = effects, onValueChange = { effects = it },
        label = { Text("Wirkungen (Komma getrennt)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
    OutlinedTextField(value = teaInfo, onValueChange = { teaInfo = it },
        label = { Text("Zubereitung") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
    OutlinedTextField(value = harvestTips, onValueChange = { harvestTips = it },
        label = { Text("Ernte-Tipps") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
    OutlinedTextField(value = warningsDE, onValueChange = { warningsDE = it },
        label = { Text("Hinweise / Warnungen") }, modifier = Modifier.fillMaxWidth(), minLines = 2)

    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
        TextButton(onClick = onDismiss) { Text("Abbrechen") }
        Spacer(Modifier.width(8.dp))
        Button(
            onClick = {
                val effectsList = effects.split(",").map { it.trim() }.filter { it.isNotBlank() }
                val gson = Gson()
                val id = initial?.id ?: "custom_${System.currentTimeMillis()}"
                onSave(
                    (initial ?: CustomHerbEntity(id = id, name = "")).copy(
                        id               = id,
                        name             = name.trim(),
                        latinName        = latinName.trim(),
                        emoji            = emoji.ifBlank { "🌿" },
                        shortDescription = description.trim(),
                        harvestPart      = harvestPart,
                        harvestTips      = harvestTips.trim(),
                        effectsJson      = gson.toJson(effectsList),
                        teaInfo          = teaInfo.trim(),
                        category         = category,
                        warningsDE       = warningsDE.trim()
                    )
                )
            },
            enabled = name.isNotBlank()
        ) { Text("Speichern") }
    }
}

// ── Edit dialog for custom herbs ──────────────────────────────────────────────
@Composable
private fun EditCustomHerbDialog(
    herbId: String,
    customVm: CustomHerbsViewModel,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    var entity by remember { mutableStateOf<CustomHerbEntity?>(null) }
    LaunchedEffect(herbId) { entity = customVm.getCustomHerbById(herbId) }

    val current = entity ?: return

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(20.dp)) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("✏️ Kraut bearbeiten",
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                ManualHerbForm(
                    initial   = current,
                    onSave    = { updated -> customVm.updateCustomHerb(updated); onSaved() },
                    onDismiss = onDismiss
                )
            }
        }
    }
}

// ── Helper composables ────────────────────────────────────────────────────────

@Composable
private fun InfoChipRow(herb: HerbInfo) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        item { AssistChip(onClick = {}, label = { Text(herb.harvestPart) },
            leadingIcon = { Text("✂️", fontSize = 12.sp) }) }
        item { AssistChip(onClick = {}, label = { Text("${herb.brewingTempC}°C") },
            leadingIcon = { Text("🌡️", fontSize = 12.sp) }) }
        item { AssistChip(onClick = {}, label = { Text("${herb.brewingMinutes} Min.") },
            leadingIcon = { Text("⏱️", fontSize = 12.sp) }) }
        item { AssistChip(onClick = {}, label = { Text("${herb.storageMonths}M Lager") },
            leadingIcon = { Text("🫙", fontSize = 12.sp) }) }
    }
}

@Composable
private fun DetailCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun LabeledRow(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(label, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(90.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun FlowRow(horizontalArrangement: Arrangement.Horizontal, content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = horizontalArrangement,
        verticalArrangement   = Arrangement.spacedBy(4.dp),
        content = { content() }
    )
}

fun monthName(month: Int): String = when (month) {
    1 -> "Jan"; 2 -> "Feb"; 3 -> "Mär"; 4 -> "Apr"
    5 -> "Mai"; 6 -> "Jun"; 7 -> "Jul"; 8 -> "Aug"
    9 -> "Sep"; 10 -> "Okt"; 11 -> "Nov"; 12 -> "Dez"
    else -> "?"
}
