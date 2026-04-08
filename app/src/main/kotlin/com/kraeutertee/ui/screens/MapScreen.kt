@file:OptIn(ExperimentalMaterial3Api::class)

package com.kraeutertee.ui.screens

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import androidx.preference.PreferenceManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.kraeutertee.data.entities.HerbLocation
import com.kraeutertee.util.HerbDatabase
import com.kraeutertee.viewmodel.MapViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.*

@Composable
fun MapScreen(vm: MapViewModel) {
    val context       = LocalContext.current
    val lifecycle     = LocalLifecycleOwner.current.lifecycle
    val locations     by vm.allLocations.collectAsState()
    val userLat       by vm.latitude.collectAsState()
    val userLng       by vm.longitude.collectAsState()
    val selectedLocation by vm.selectedLocation.collectAsState()

    var showAddDialog   by remember { mutableStateOf(false) }
    var pendingPoint    by remember { mutableStateOf<GeoPoint?>(null) }
    var showDetailSheet by remember { mutableStateOf(false) }
    var showListView    by remember { mutableStateOf(false) }

    // Keep a stable reference to the MapView for FAB actions
    val mapViewRef = remember { mutableStateOf<MapView?>(null) }

    LaunchedEffect(Unit) { vm.refreshLocation(context) }

    Box(modifier = Modifier.fillMaxSize()) {

        // ── OSM Map ─────────────────────────────────────────────────────────────
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                // Proper OSMDroid initialisation – MUST come before MapView()
                Configuration.getInstance().apply {
                    load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))
                    userAgentValue = ctx.packageName
                }
                MapView(ctx).also { mv ->
                    mapViewRef.value = mv
                    mv.setTileSource(TileSourceFactory.MAPNIK)
                    mv.setMultiTouchControls(true)
                    mv.controller.setZoom(13.0)
                    mv.controller.setCenter(GeoPoint(userLat, userLng))

                    // Long-press listener for adding herb locations
                    mv.overlays.add(MapEventsOverlay(object : MapEventsReceiver {
                        override fun singleTapConfirmedHelper(p: GeoPoint?) = false
                        override fun longPressHelper(p: GeoPoint?): Boolean {
                            p?.let { pendingPoint = it; showAddDialog = true }
                            return true
                        }
                    }))
                }
            },
            update = { mv ->
                // Remove only Marker overlays, keep MapEventsOverlay
                mv.overlays.removeAll { it is Marker }

                // Current user position (blue dot)
                Marker(mv).apply {
                    position    = GeoPoint(userLat, userLng)
                    title       = "Mein Standort"
                    icon        = makeColoredMarker(context, android.graphics.Color.BLUE)
                    mv.overlays.add(this)
                }
                // Saved herb locations (green dots)
                locations.forEach { loc ->
                    val herb = HerbDatabase.getById(loc.herbId)
                    Marker(mv).apply {
                        position = GeoPoint(loc.latitude, loc.longitude)
                        title    = loc.herbName
                        snippet  = if (loc.notes.isNotBlank()) loc.notes
                                   else herb?.shortDescription ?: ""
                        icon     = makeColoredMarker(context, android.graphics.Color.GREEN)
                        setOnMarkerClickListener { _, _ ->
                            vm.selectLocation(loc); showDetailSheet = true; true
                        }
                        mv.overlays.add(this)
                    }
                }
                mv.invalidate()
            }
        )

        // ── Lifecycle: resume / pause the map properly ───────────────────────────
        DisposableEffect(lifecycle) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> mapViewRef.value?.onResume()
                    Lifecycle.Event.ON_PAUSE  -> mapViewRef.value?.onPause()
                    else -> Unit
                }
            }
            lifecycle.addObserver(observer)
            onDispose {
                lifecycle.removeObserver(observer)
                mapViewRef.value?.onDetach()
            }
        }

        // ── FABs ──────────────────────────────────────────────────────────────────
        Column(
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SmallFloatingActionButton(
                onClick = { showListView = true },
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ) { Icon(Icons.Default.List, "Liste") }

            SmallFloatingActionButton(
                onClick = {
                    mapViewRef.value?.controller?.apply {
                        setZoom(14.0)
                        setCenter(GeoPoint(userLat, userLng))
                    }
                    vm.refreshLocation(context)
                },
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            ) { Icon(Icons.Default.MyLocation, "Mein Standort") }

            FloatingActionButton(
                onClick = { pendingPoint = GeoPoint(userLat, userLng); showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) { Icon(Icons.Default.AddLocation, "Fundstelle hinzufügen") }
        }

        // ── Location count badge ─────────────────────────────────────────────────
        if (locations.isNotEmpty()) {
            Surface(
                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                shadowElevation = 4.dp
            ) {
                Text(
                    "${locations.size} Fundstellen",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }

    // ── Dialogs / sheets ─────────────────────────────────────────────────────────
    if (showAddDialog && pendingPoint != null) {
        AddLocationDialog(
            point = pendingPoint!!,
            onConfirm = { herbId, herbName, notes, qty ->
                vm.addLocation(HerbLocation(
                    herbId = herbId, herbName = herbName,
                    latitude  = pendingPoint!!.latitude,
                    longitude = pendingPoint!!.longitude,
                    notes = notes, quantity = qty
                ))
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }

    if (showDetailSheet && selectedLocation != null) {
        LocationDetailSheet(
            location = selectedLocation!!,
            onDelete = { vm.deleteLocation(selectedLocation!!); showDetailSheet = false },
            onDismiss = { showDetailSheet = false }
        )
    }

    if (showListView) {
        ModalBottomSheet(onDismissRequest = { showListView = false }) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text("Alle Fundstellen",
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                }
                if (locations.isEmpty()) {
                    item {
                        Text("Noch keine Fundstellen. Lange auf die Karte drücken.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                items(locations) { loc ->
                    LocationListItem(
                        location = loc,
                        onClick  = { vm.selectLocation(loc); showListView = false; showDetailSheet = true },
                        onDelete = { vm.deleteLocation(loc) }
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

// ── Private composables ───────────────────────────────────────────────────────

@Composable
private fun AddLocationDialog(
    point: GeoPoint,
    onConfirm: (herbId: String, herbName: String, notes: String, qty: String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedHerbId by remember { mutableStateOf(HerbDatabase.all.first().id) }
    var notes    by remember { mutableStateOf("") }
    var qty      by remember { mutableStateOf("mittel") }
    var expanded by remember { mutableStateOf(false) }
    val quantities = listOf("sehr viel", "viel", "mittel", "wenig", "einzeln")

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(20.dp)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("📍 Fundstelle hinzufügen",
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("%.5f, %.5f".format(point.latitude, point.longitude),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))

                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    val sel = HerbDatabase.getById(selectedHerbId)
                    OutlinedTextField(
                        value = "${sel?.emoji} ${sel?.name}", onValueChange = {},
                        readOnly = true, label = { Text("Kraut wählen") },
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

                Text("Menge:", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    quantities.forEach { q ->
                        FilterChip(selected = qty == q, onClick = { qty = q },
                            label = { Text(q, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.height(28.dp))
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = notes, onValueChange = { notes = it },
                    label = { Text("Notizen (optional)") },
                    modifier = Modifier.fillMaxWidth(), minLines = 2)
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) { Text("Abbrechen") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        val herb = HerbDatabase.getById(selectedHerbId)!!
                        onConfirm(selectedHerbId, herb.name, notes, qty)
                    }) { Text("Speichern") }
                }
            }
        }
    }
}

@Composable
private fun LocationDetailSheet(
    location: HerbLocation,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val herb = HerbDatabase.getById(location.herbId)
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(herb?.emoji ?: "🌿", fontSize = 36.sp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(location.herbName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("%.5f, %.5f".format(location.latitude, location.longitude),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "Löschen", tint = MaterialTheme.colorScheme.error)
                }
            }
            if (location.quantity.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                AssistChip(onClick = {}, label = { Text("Menge: ${location.quantity}") },
                    leadingIcon = { Text("📦", fontSize = 12.sp) })
            }
            if (location.notes.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(location.notes, style = MaterialTheme.typography.bodyMedium)
            }
            herb?.let {
                Spacer(Modifier.height(12.dp)); HorizontalDivider(); Spacer(Modifier.height(12.dp))
                Text("Beste Erntemonate:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Text(it.harvestMonths.joinToString(", ") { m -> monthName(m) })
                Spacer(Modifier.height(4.dp))
                Text(it.harvestTips, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
private fun LocationListItem(location: HerbLocation, onClick: () -> Unit, onDelete: () -> Unit) {
    val herb = HerbDatabase.getById(location.herbId)
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(herb?.emoji ?: "🌿", fontSize = 28.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(location.herbName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Text("%.4f, %.4f".format(location.latitude, location.longitude),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (location.quantity.isNotBlank())
                    Text("Menge: ${location.quantity}", style = MaterialTheme.typography.labelSmall)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
            }
        }
    }
}

private fun makeColoredMarker(context: Context, color: Int): BitmapDrawable {
    val size = 48
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
    canvas.drawCircle(size / 2f, size / 2f, size / 2f - 4, paint)
    paint.color = android.graphics.Color.WHITE
    canvas.drawCircle(size / 2f, size / 2f, size / 4f, paint)
    return BitmapDrawable(context.resources, bmp)
}
