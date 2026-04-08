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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kraeutertee.data.entities.Recipe
import com.kraeutertee.util.HerbDatabase
import com.kraeutertee.viewmodel.RecipesViewModel

data class RecipeIngredient(
    val herbId: String,
    val herbName: String,
    val amountGrams: Int,
    val note: String = ""
)

private val gson = Gson()
private fun parseIngredients(json: String): List<RecipeIngredient> = try {
    gson.fromJson(json, object : TypeToken<List<RecipeIngredient>>() {}.type)
} catch (e: Exception) { emptyList() }

private fun parseTags(json: String): List<String> = try {
    gson.fromJson(json, object : TypeToken<List<String>>() {}.type)
} catch (e: Exception) { emptyList() }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipesScreen(vm: RecipesViewModel) {
    val recipes by vm.recipes.collectAsState()
    val searchQuery by vm.searchQuery.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingRecipe by remember { mutableStateOf<Recipe?>(null) }
    var viewingRecipe by remember { mutableStateOf<Recipe?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<Recipe?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {

        // ── Search + header ────────────────────────────────────────────────────
        OutlinedTextField(
            value = searchQuery,
            onValueChange = vm::setSearch,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Rezepte suchen…") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty())
                    IconButton(onClick = { vm.setSearch("") }) { Icon(Icons.Default.Clear, null) }
            },
            singleLine = true,
            shape = RoundedCornerShape(28.dp)
        )

        Box(modifier = Modifier.fillMaxSize()) {
            if (recipes.isEmpty()) {
                EmptyRecipesPlaceholder()
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Text("${recipes.size} Rezept${if (recipes.size != 1) "e" else ""}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    items(recipes, key = { it.id }) { recipe ->
                        RecipeCard(
                            recipe = recipe,
                            onClick = { viewingRecipe = recipe },
                            onEdit = { editingRecipe = recipe },
                            onDelete = { showDeleteConfirm = recipe }
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }

            FloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.primary
            ) { Icon(Icons.Default.Add, "Rezept hinzufügen") }
        }
    }

    // ── Dialogs ────────────────────────────────────────────────────────────────
    if (showAddDialog) {
        RecipeEditDialog(
            recipe = null,
            onSave = { recipe -> vm.addRecipe(recipe); showAddDialog = false },
            onDismiss = { showAddDialog = false }
        )
    }
    editingRecipe?.let { r ->
        RecipeEditDialog(
            recipe = r,
            onSave = { updated -> vm.updateRecipe(updated); editingRecipe = null },
            onDismiss = { editingRecipe = null }
        )
    }
    viewingRecipe?.let { r ->
        RecipeDetailSheet(
            recipe = r,
            onEdit = { editingRecipe = r; viewingRecipe = null },
            onDismiss = { viewingRecipe = null }
        )
    }
    showDeleteConfirm?.let { r ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("Rezept löschen?") },
            text = { Text("\"${r.name}\" wird dauerhaft gelöscht.") },
            confirmButton = {
                TextButton(onClick = { vm.deleteRecipe(r); showDeleteConfirm = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Text("Löschen")
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = null }) { Text("Abbrechen") } }
        )
    }
}

// ── Recipe card ────────────────────────────────────────────────────────────────
@Composable
private fun RecipeCard(
    recipe: Recipe,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val ingredients = parseIngredients(recipe.ingredientsJson)
    val tags = parseTags(recipe.tagsJson)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(recipe.name, style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (recipe.description.isNotBlank())
                        Text(recipe.description, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2,
                            overflow = TextOverflow.Ellipsis)
                }
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, null,
                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, null,
                            tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            // Ingredient emojis preview
            if (ingredients.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    ingredients.take(6).forEach { ing ->
                        val herb = HerbDatabase.getById(ing.herbId)
                        Text(herb?.emoji ?: "🌿", fontSize = 18.sp)
                    }
                    if (ingredients.size > 6) Text("+${ingredients.size - 6}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterVertically))
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(onClick = {}, label = { Text("${recipe.brewingTempC}°C") },
                    leadingIcon = { Text("🌡️", fontSize = 10.sp) },
                    modifier = Modifier.height(24.dp))
                AssistChip(onClick = {}, label = { Text("${recipe.brewingMinutes} Min.") },
                    leadingIcon = { Text("⏱️", fontSize = 10.sp) },
                    modifier = Modifier.height(24.dp))
                AssistChip(onClick = {}, label = { Text("${recipe.servings} Port.") },
                    leadingIcon = { Text("☕", fontSize = 10.sp) },
                    modifier = Modifier.height(24.dp))
                if (recipe.rating > 0f) {
                    Spacer(Modifier.weight(1f))
                    repeat(recipe.rating.toInt()) { Text("⭐", fontSize = 12.sp) }
                }
            }

            if (tags.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    tags.take(3).forEach { tag ->
                        SuggestionChip(onClick = {},
                            label = { Text(tag, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.height(22.dp))
                    }
                }
            }
        }
    }
}

// ── Recipe detail sheet ────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecipeDetailSheet(recipe: Recipe, onEdit: () -> Unit, onDismiss: () -> Unit) {
    val ingredients = parseIngredients(recipe.ingredientsJson)
    val tags = parseTags(recipe.tagsJson)

    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(recipe.name, style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold)
                        if (recipe.description.isNotBlank())
                            Text(recipe.description, style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    FilledTonalIconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, "Bearbeiten")
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = {}, label = { Text("${recipe.brewingTempC}°C") },
                        leadingIcon = { Text("🌡️", fontSize = 12.sp) })
                    AssistChip(onClick = {}, label = { Text("${recipe.brewingMinutes} Min. ziehen") },
                        leadingIcon = { Text("⏱️", fontSize = 12.sp) })
                    AssistChip(onClick = {}, label = { Text("${recipe.servings} Port.") },
                        leadingIcon = { Text("☕", fontSize = 12.sp) })
                }
            }

            if (ingredients.isNotEmpty()) {
                item {
                    Text("🌿 Zutaten", style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Card(shape = RoundedCornerShape(12.dp)) {
                        Column(modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ingredients.forEach { ing ->
                                val herb = HerbDatabase.getById(ing.herbId)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(herb?.emoji ?: "🌿", fontSize = 20.sp,
                                        modifier = Modifier.width(32.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(ing.herbName, style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium)
                                        if (ing.note.isNotBlank())
                                            Text(ing.note, style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text("${ing.amountGrams}g",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }

            if (recipe.instructions.isNotBlank()) {
                item {
                    Text("📋 Zubereitung", style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Card(shape = RoundedCornerShape(12.dp)) {
                        Text(recipe.instructions,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp))
                    }
                }
            }

            if (tags.isNotEmpty()) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        tags.forEach { tag ->
                            SuggestionChip(onClick = {}, label = { Text(tag) })
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(100.dp)) }
        }
    }
}

// ── Recipe edit dialog ─────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecipeEditDialog(
    recipe: Recipe?,
    onSave: (Recipe) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(recipe?.name ?: "") }
    var description by remember { mutableStateOf(recipe?.description ?: "") }
    var instructions by remember { mutableStateOf(recipe?.instructions ?: "") }
    var brewTemp by remember { mutableIntStateOf(recipe?.brewingTempC ?: 90) }
    var brewMin by remember { mutableIntStateOf(recipe?.brewingMinutes ?: 7) }
    var servings by remember { mutableIntStateOf(recipe?.servings ?: 2) }
    var rating by remember { mutableFloatStateOf(recipe?.rating ?: 0f) }
    var tagsInput by remember { mutableStateOf(parseTags(recipe?.tagsJson ?: "[]").joinToString(", ")) }
    var ingredients by remember {
        mutableStateOf(parseIngredients(recipe?.ingredientsJson ?: "[]"))
    }
    var showIngredientPicker by remember { mutableStateOf(false) }
    var ingredientHerbId by remember { mutableStateOf(HerbDatabase.all.first().id) }
    var ingredientAmount by remember { mutableStateOf("2") }
    var ingredientNote by remember { mutableStateOf("") }
    var herbExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(if (recipe == null) "➕ Neues Rezept" else "✏️ Rezept bearbeiten",
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                OutlinedTextField(value = name, onValueChange = { name = it },
                    label = { Text("Name *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

                OutlinedTextField(value = description, onValueChange = { description = it },
                    label = { Text("Kurzbeschreibung") }, modifier = Modifier.fillMaxWidth(), minLines = 2)

                // Brew settings row
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = brewTemp.toString(),
                        onValueChange = { brewTemp = it.toIntOrNull() ?: brewTemp },
                        label = { Text("Temp. °C") },
                        modifier = Modifier.weight(1f), singleLine = true
                    )
                    OutlinedTextField(
                        value = brewMin.toString(),
                        onValueChange = { brewMin = it.toIntOrNull() ?: brewMin },
                        label = { Text("Min.") },
                        modifier = Modifier.weight(1f), singleLine = true
                    )
                    OutlinedTextField(
                        value = servings.toString(),
                        onValueChange = { servings = it.toIntOrNull() ?: servings },
                        label = { Text("Port.") },
                        modifier = Modifier.weight(1f), singleLine = true
                    )
                }

                // Rating
                Text("Bewertung:", style = MaterialTheme.typography.labelMedium)
                Row {
                    (1..5).forEach { star ->
                        IconButton(onClick = { rating = star.toFloat() }, modifier = Modifier.size(32.dp)) {
                            Text(if (star <= rating.toInt()) "⭐" else "☆", fontSize = 20.sp)
                        }
                    }
                    if (rating > 0) TextButton(onClick = { rating = 0f }) { Text("Reset") }
                }

                // Ingredients
                Text("🌿 Zutaten:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                ingredients.forEach { ing ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(0.5f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val herb = HerbDatabase.getById(ing.herbId)
                        Text(herb?.emoji ?: "🌿", fontSize = 18.sp)
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(ing.herbName, style = MaterialTheme.typography.bodySmall)
                            Text("${ing.amountGrams}g", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = {
                            ingredients = ingredients.filter { it !== ing }
                        }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, null, modifier = Modifier.size(14.dp))
                        }
                    }
                }
                OutlinedButton(onClick = { showIngredientPicker = true },
                    modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Add, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Zutat hinzufügen")
                }

                if (showIngredientPicker) {
                    Card(shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ExposedDropdownMenuBox(expanded = herbExpanded, onExpandedChange = { herbExpanded = it }) {
                                val sel = HerbDatabase.getById(ingredientHerbId)
                                OutlinedTextField(
                                    value = "${sel?.emoji} ${sel?.name}", onValueChange = {},
                                    readOnly = true, label = { Text("Kraut") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(herbExpanded) },
                                    modifier = Modifier.menuAnchor().fillMaxWidth()
                                )
                                ExposedDropdownMenu(expanded = herbExpanded, onDismissRequest = { herbExpanded = false }) {
                                    HerbDatabase.all.forEach { h ->
                                        DropdownMenuItem(
                                            text = { Text("${h.emoji} ${h.name}") },
                                            onClick = { ingredientHerbId = h.id; herbExpanded = false }
                                        )
                                    }
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(value = ingredientAmount,
                                    onValueChange = { ingredientAmount = it },
                                    label = { Text("Gramm") }, modifier = Modifier.weight(1f), singleLine = true)
                                OutlinedTextField(value = ingredientNote,
                                    onValueChange = { ingredientNote = it },
                                    label = { Text("Notiz") }, modifier = Modifier.weight(2f), singleLine = true)
                            }
                            Button(onClick = {
                                val herb = HerbDatabase.getById(ingredientHerbId)!!
                                ingredients = ingredients + RecipeIngredient(
                                    herbId = ingredientHerbId, herbName = herb.name,
                                    amountGrams = ingredientAmount.toIntOrNull() ?: 2, note = ingredientNote
                                )
                                ingredientAmount = "2"; ingredientNote = ""; showIngredientPicker = false
                            }, modifier = Modifier.fillMaxWidth()) { Text("Zutat speichern") }
                        }
                    }
                }

                OutlinedTextField(value = instructions, onValueChange = { instructions = it },
                    label = { Text("Zubereitung / Anleitung") },
                    modifier = Modifier.fillMaxWidth(), minLines = 3)

                OutlinedTextField(value = tagsInput, onValueChange = { tagsInput = it },
                    label = { Text("Tags (Komma getrennt, z.B. Erkältung, Abend)") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)

                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) { Text("Abbrechen") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isBlank()) return@Button
                            val tags = tagsInput.split(",").map { it.trim() }.filter { it.isNotBlank() }
                            onSave(
                                (recipe ?: Recipe(name = "")).copy(
                                    name = name, description = description,
                                    instructions = instructions, brewingTempC = brewTemp,
                                    brewingMinutes = brewMin, servings = servings, rating = rating,
                                    ingredientsJson = gson.toJson(ingredients),
                                    tagsJson = gson.toJson(tags),
                                    updatedAt = System.currentTimeMillis()
                                )
                            )
                        },
                        enabled = name.isNotBlank()
                    ) { Text("Speichern") }
                }
            }
        }
    }
}

@Composable
private fun EmptyRecipesPlaceholder() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("📖", fontSize = 64.sp)
        Spacer(Modifier.height(16.dp))
        Text("Noch keine Rezepte", style = MaterialTheme.typography.titleMedium)
        Text("Tippe auf + um dein erstes Teerezept zu erstellen.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
