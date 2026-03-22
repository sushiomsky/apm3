package com.sushiomsky.proxmark.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sushiomsky.proxmark.plugins.Plugin
import com.sushiomsky.proxmark.plugins.PluginCategory
import com.sushiomsky.proxmark.plugins.PluginPermission

private val AccentBlue = Color(0xFF2196F3)
private val AccentGreen = Color(0xFF4CAF50)
private val AccentAmber = Color(0xFFFFC107)
private val AccentRed = Color(0xFFF44336)
private val AccentPurple = Color(0xFF9C27B0)
private val DarkSurface = Color(0xFF1A1A2E)
private val CardDark = Color(0xFF16213E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluginBrowserDialog(
    plugins: List<Plugin>,
    onInstallPlugin: (String) -> Unit,
    onUninstallPlugin: (String) -> Unit,
    onTogglePluginEnabled: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<PluginCategory?>(null) }
    var selectedPlugin by remember { mutableStateOf<Plugin?>(null) }

    val filteredPlugins = plugins.filter { plugin ->
        val matchesSearch = searchQuery.isBlank() ||
            plugin.name.contains(searchQuery, ignoreCase = true) ||
            plugin.description.contains(searchQuery, ignoreCase = true)
        val matchesCategory = selectedCategory == null || plugin.category == selectedCategory
        matchesSearch && matchesCategory
    }

    val installedPlugins = filteredPlugins.filter { it.installed }
    val availablePlugins = filteredPlugins.filter { !it.installed }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(0.dp),
            shape = RoundedCornerShape(0.dp),
            color = DarkSurface
        ) {
            // If a plugin is selected, show detail; otherwise show list
            selectedPlugin?.let { plugin ->
                PluginDetailView(
                    plugin = plugin,
                    onInstall = { onInstallPlugin(plugin.id) },
                    onUninstall = { onUninstallPlugin(plugin.id) },
                    onToggleEnabled = { onTogglePluginEnabled(plugin.id) },
                    onBack = { selectedPlugin = null }
                )
            } ?: run {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Gradient header
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        AccentBlue.copy(alpha = 0.8f),
                                        AccentPurple.copy(alpha = 0.6f),
                                        DarkSurface
                                    )
                                )
                            )
                            .padding(top = 16.dp, bottom = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Plugin Store",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                IconButton(onClick = onDismiss) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Close",
                                        tint = Color.White
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Search bar
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Search plugins…", color = Color.White.copy(alpha = 0.5f)) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Search,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.7f)
                                    )
                                },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(
                                                Icons.Default.Clear,
                                                contentDescription = "Clear",
                                                tint = Color.White.copy(alpha = 0.7f)
                                            )
                                        }
                                    }
                                },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color.White.copy(alpha = 0.5f),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                    cursorColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Category filter chips
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = selectedCategory == null,
                                    onClick = { selectedCategory = null },
                                    label = { Text("All") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = AccentBlue,
                                        selectedLabelColor = Color.White
                                    )
                                )
                                PluginCategory.entries.forEach { category ->
                                    FilterChip(
                                        selected = selectedCategory == category,
                                        onClick = {
                                            selectedCategory = if (selectedCategory == category) null else category
                                        },
                                        label = { Text(category.label) },
                                        leadingIcon = if (selectedCategory == category) {
                                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                        } else null,
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = categoryColor(category),
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Plugin list
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        if (installedPlugins.isNotEmpty()) {
                            item {
                                Text(
                                    "INSTALLED",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = AccentGreen,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                            items(installedPlugins, key = { it.id }) { plugin ->
                                PluginCard(
                                    plugin = plugin,
                                    onTap = { selectedPlugin = plugin },
                                    onInstallToggle = {
                                        if (plugin.installed) onUninstallPlugin(plugin.id)
                                        else onInstallPlugin(plugin.id)
                                    },
                                    onToggleEnabled = { onTogglePluginEnabled(plugin.id) }
                                )
                            }
                        }

                        if (availablePlugins.isNotEmpty()) {
                            item {
                                Text(
                                    "AVAILABLE",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = AccentBlue,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                                )
                            }
                            items(availablePlugins, key = { it.id }) { plugin ->
                                PluginCard(
                                    plugin = plugin,
                                    onTap = { selectedPlugin = plugin },
                                    onInstallToggle = { onInstallPlugin(plugin.id) },
                                    onToggleEnabled = {}
                                )
                            }
                        }

                        if (filteredPlugins.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            Icons.Default.SearchOff,
                                            contentDescription = null,
                                            modifier = Modifier.size(48.dp),
                                            tint = Color.White.copy(alpha = 0.3f)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            "No plugins found",
                                            color = Color.White.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PluginCard(
    plugin: Plugin,
    onTap: () -> Unit,
    onInstallToggle: () -> Unit,
    onToggleEnabled: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onTap),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(categoryColor(plugin.category).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = pluginIcon(plugin.icon),
                        contentDescription = null,
                        tint = categoryColor(plugin.category),
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            plugin.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "v${plugin.version}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.4f)
                        )
                    }
                    Text(
                        plugin.author,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                if (plugin.installed) {
                    Switch(
                        checked = plugin.enabled,
                        onCheckedChange = { onToggleEnabled() },
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = AccentGreen,
                            checkedThumbColor = Color.White
                        ),
                        modifier = Modifier.height(24.dp)
                    )
                } else {
                    OutlinedButton(
                        onClick = onInstallToggle,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier = Modifier.height(32.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Install", fontSize = 12.sp, color = AccentBlue)
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                plugin.description,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (plugin.permissions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    plugin.permissions.forEach { perm ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = permissionColor(perm).copy(alpha = 0.15f),
                            modifier = Modifier.height(20.dp)
                        ) {
                            Text(
                                perm.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = permissionColor(perm),
                                fontSize = 9.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PluginDetailView(
    plugin: Plugin,
    onInstall: () -> Unit,
    onUninstall: () -> Unit,
    onToggleEnabled: () -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Header gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            categoryColor(plugin.category).copy(alpha = 0.7f),
                            DarkSurface
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = categoryColor(plugin.category).copy(alpha = 0.3f)
                    ) {
                        Text(
                            plugin.category.label,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = pluginIcon(plugin.icon),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            plugin.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            "${plugin.author} • v${plugin.version}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Install/Uninstall + Enable
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (plugin.installed) {
                        Button(
                            onClick = onUninstall,
                            colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Uninstall")
                        }
                        Button(
                            onClick = onToggleEnabled,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (plugin.enabled) AccentGreen else Color.Gray
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (plugin.enabled) "Enabled" else "Disabled")
                        }
                    } else {
                        Button(
                            onClick = onInstall,
                            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Install Plugin")
                        }
                    }
                }
            }
        }

        // Content
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Description
            item {
                Text(
                    "About",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    plugin.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }

            // Permissions
            if (plugin.permissions.isNotEmpty()) {
                item {
                    Text(
                        "Permissions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        plugin.permissions.forEach { perm ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        permissionColor(perm).copy(alpha = 0.1f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(10.dp)
                            ) {
                                Icon(
                                    imageVector = permissionIcon(perm),
                                    contentDescription = null,
                                    tint = permissionColor(perm),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        perm.label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                    Text(
                                        perm.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Commands
            if (plugin.commands.isNotEmpty()) {
                item {
                    Text(
                        "Commands Used",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        plugin.commands.forEach { cmd ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = CardDark
                            ) {
                                Text(
                                    cmd,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = AccentGreen
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Helpers

private fun categoryColor(category: PluginCategory): Color = when (category) {
    PluginCategory.ATTACK -> AccentRed
    PluginCategory.ANALYSIS -> AccentBlue
    PluginCategory.CLONING -> AccentAmber
    PluginCategory.AUTOMATION -> AccentPurple
    PluginCategory.UTILITY -> AccentGreen
    PluginCategory.CUSTOM -> Color(0xFF00BCD4)
}

private fun permissionColor(perm: PluginPermission): Color = when (perm) {
    PluginPermission.READ_TAG -> AccentBlue
    PluginPermission.WRITE_TAG -> AccentAmber
    PluginPermission.EXECUTE_COMMANDS -> AccentPurple
    PluginPermission.ACCESS_KEYS -> AccentRed
    PluginPermission.EXPORT_DATA -> AccentGreen
    PluginPermission.MODIFY_SETTINGS -> Color(0xFFFF9800)
}

private fun permissionIcon(perm: PluginPermission): ImageVector = when (perm) {
    PluginPermission.READ_TAG -> Icons.Default.Nfc
    PluginPermission.WRITE_TAG -> Icons.Default.Edit
    PluginPermission.EXECUTE_COMMANDS -> Icons.Default.Terminal
    PluginPermission.ACCESS_KEYS -> Icons.Default.Key
    PluginPermission.EXPORT_DATA -> Icons.Default.FileDownload
    PluginPermission.MODIFY_SETTINGS -> Icons.Default.Settings
}

private fun pluginIcon(iconName: String): ImageVector = when (iconName) {
    "FitnessCenter" -> Icons.Default.FitnessCenter
    "VpnKey" -> Icons.Default.VpnKey
    "ContentCopy" -> Icons.Default.ContentCopy
    "QrCode" -> Icons.Default.QrCode
    "Security" -> Icons.Default.Security
    "FileDownload" -> Icons.Default.FileDownload
    "Analytics" -> Icons.Default.Analytics
    "DynamicFeed" -> Icons.Default.DynamicFeed
    "Nfc" -> Icons.Default.Nfc
    "Badge" -> Icons.Default.Badge
    "Edit" -> Icons.Default.Edit
    "Radar" -> Icons.Default.Radar
    else -> Icons.Default.Extension
}
