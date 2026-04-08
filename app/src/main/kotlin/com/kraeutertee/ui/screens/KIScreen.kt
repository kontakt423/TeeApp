@file:OptIn(ExperimentalMaterial3Api::class)

package com.kraeutertee.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.kraeutertee.viewmodel.KIViewModel

data class ChatMessage(
    val role: String,  // "user" | "assistant"
    val content: String,
    val isError: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KIScreen(vm: KIViewModel) {
    val messages by vm.messages.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val hasApiKey by vm.hasApiKey.collectAsState()
    val blendSuggestions by vm.blendSuggestions.collectAsState()
    val gardenHerbs by vm.gardenHerbNames.collectAsState()
    var inputText by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty())
            listState.animateScrollToItem(messages.size - 1)
    }

    Column(modifier = Modifier.fillMaxSize()) {

        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 },
                text = { Text("💬 Chat") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 },
                text = { Text("🍵 Mischungen") })
        }

        when (selectedTab) {

            // ── Chat tab ─────────────────────────────────────────────────────
            0 -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    if (!hasApiKey) {
                        ApiKeyBanner()
                    }

                    if (messages.isEmpty()) {
                        WelcomeChatContent(
                            onQuickQuestion = { q -> vm.sendMessage(q); inputText = "" }
                        )
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(messages) { msg ->
                                ChatBubble(message = msg)
                            }
                            if (isLoading) {
                                item { LoadingBubble() }
                            }
                            item { Spacer(Modifier.height(8.dp)) }
                        }
                    }

                    // ── Input bar ────────────────────────────────────────────
                    Surface(shadowElevation = 8.dp) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            OutlinedTextField(
                                value = inputText,
                                onValueChange = { inputText = it },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("Frage den Kräuterexperten…") },
                                shape = RoundedCornerShape(24.dp),
                                minLines = 1,
                                maxLines = 4,
                                enabled = !isLoading && hasApiKey
                            )
                            Spacer(Modifier.width(8.dp))
                            FilledIconButton(
                                onClick = {
                                    if (inputText.isNotBlank()) {
                                        vm.sendMessage(inputText)
                                        inputText = ""
                                    }
                                },
                                enabled = inputText.isNotBlank() && !isLoading && hasApiKey,
                                modifier = Modifier.size(48.dp)
                            ) {
                                if (isLoading)
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                                else
                                    Icon(Icons.Default.Send, "Senden")
                            }
                        }
                    }
                }
            }

            // ── Blend suggestions tab ─────────────────────────────────────────
            1 -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (!hasApiKey) {
                        item { ApiKeyBanner() }
                    }

                    // Garden herbs summary
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("🌱 Deine Kräutersammlung",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(4.dp))
                                if (gardenHerbs.isEmpty()) {
                                    Text("Markiere Kräuter im Kräuter-Tab mit 🌱 um sie zu deiner Sammlung hinzuzufügen.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(0.8f))
                                } else {
                                    Text(gardenHerbs.joinToString(" · "),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    Spacer(Modifier.height(12.dp))
                                    Button(
                                        onClick = { vm.loadBlendSuggestions() },
                                        enabled = !isLoading,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        if (isLoading) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                                            Spacer(Modifier.width(8.dp))
                                        } else {
                                            Icon(Icons.Default.AutoAwesome, null, Modifier.size(16.dp))
                                            Spacer(Modifier.width(8.dp))
                                        }
                                        Text("KI-Mischungsvorschläge generieren")
                                    }
                                }
                            }
                        }
                    }

                    // Blend suggestions result
                    if (blendSuggestions.isNotBlank()) {
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(2.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.SmartToy, null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("KI-Mischungsvorschläge",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(Modifier.height(10.dp))
                                    Text(blendSuggestions, style = MaterialTheme.typography.bodyMedium)
                                    Spacer(Modifier.height(8.dp))
                                    OutlinedButton(onClick = { vm.loadBlendSuggestions() },
                                        modifier = Modifier.fillMaxWidth()) {
                                        Icon(Icons.Default.Refresh, null, Modifier.size(14.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Neue Vorschläge")
                                    }
                                }
                            }
                        }
                    }

                    // Quick herb harmony cards
                    item {
                        Text("Schnelle Kombinationsideen",
                            style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    item {
                        QuickBlendGrid()
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

// ── Chat bubble ────────────────────────────────────────────────────────────────
@Composable
private fun ChatBubble(message: ChatMessage) {
    val isUser = message.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) { Text("🌿", fontSize = 16.sp) }
            Spacer(Modifier.width(8.dp))
        }

        Surface(
            shape = RoundedCornerShape(
                topStart = if (isUser) 20.dp else 4.dp,
                topEnd = if (isUser) 4.dp else 20.dp,
                bottomStart = 20.dp, bottomEnd = 20.dp
            ),
            color = when {
                message.isError -> Color(0xFFFFEBEE)
                isUser -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.surfaceVariant
            },
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Text(
                message.content,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = when {
                    message.isError -> Color(0xFFB71C1C)
                    isUser -> MaterialTheme.colorScheme.onPrimary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }

        if (isUser) {
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.Person, null, modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer) }
        }
    }
}

@Composable
private fun LoadingBubble() {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) { Text("🌿", fontSize = 16.sp) }
        Spacer(Modifier.width(8.dp))
        Surface(
            shape = RoundedCornerShape(4.dp, 20.dp, 20.dp, 20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Each dot is its own composable – avoids calling @Composable inside repeat {}
                TypingDot(delayMillis = 0)
                TypingDot(delayMillis = 200)
                TypingDot(delayMillis = 400)
            }
        }
    }
}

@Composable
private fun TypingDot(delayMillis: Int) {
    val transition = rememberInfiniteTransition(label = "typing_dot_$delayMillis")
    val alpha by transition.animateFloat(
        initialValue = 0.25f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(
                durationMillis = 600,
                delayMillis    = delayMillis
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_alpha_$delayMillis"
    )
    Box(
        modifier = Modifier
            .size(8.dp)
            .background(
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                CircleShape
            )
    )
}

@Composable
private fun WelcomeChatContent(onQuickQuestion: (String) -> Unit) {
    val quickQuestions = listOf(
        "Welche Kräuter helfen gegen Stress?",
        "Was hilft bei Erkältung?",
        "Welche Kräuter fördern den Schlaf?",
        "Wie erkenne ich die Erntezeit?",
        "Was ist beim Trocknen zu beachten?",
        "Welche Kräuter harmonieren mit Kamille?"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🌿", fontSize = 64.sp)
        Spacer(Modifier.height(16.dp))
        Text("Kräuterexperte", style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text("Frag mich alles über Kräuter, Tee, Ernte und Naturheilkunde!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        Text("Schnellfragen:", style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        quickQuestions.forEach { q ->
            SuggestionChip(
                onClick = { onQuickQuestion(q) },
                label = { Text(q) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
            )
        }
    }
}

@Composable
private fun ApiKeyBanner() {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Warning, null, tint = Color(0xFFF57C00))
            Spacer(Modifier.width(8.dp))
            Text("Kein API-Schlüssel. Bitte in den Einstellungen (⚙️) eingeben.",
                style = MaterialTheme.typography.bodySmall, color = Color(0xFF7F4800))
        }
    }
}

@Composable
private fun QuickBlendGrid() {
    val blends = listOf(
        Triple("Schlaf-Tee 🌙", "Baldrian · Melisse · Lavendel", "Beruhigend & schlaffördernd"),
        Triple("Erkältungs-Tee 🤧", "Holunderblüte · Thymian · Ingwer", "Schweißtreibend & wärmend"),
        Triple("Verdauungs-Tee 🌿", "Kamille · Pfefferminze · Fenchel", "Entspannend & krampflösend"),
        Triple("Immun-Tee 🛡", "Hagebutte · Echinacea · Ingwer", "Stärkend & antiviral"),
        Triple("Frühlings-Detox 🌱", "Brennnessel · Löwenzahn · Birke", "Entschlackend & mineralreich"),
        Triple("Stimmungs-Tee ☀️", "Johanniskraut · Melisse · Lavendel", "Aufhellend & ausgleichend")
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        blends.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (name, herbs, effect) ->
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(0.6f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(name, style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text(herbs, style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Spacer(Modifier.height(2.dp))
                            Text(effect, style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(0.7f))
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}
