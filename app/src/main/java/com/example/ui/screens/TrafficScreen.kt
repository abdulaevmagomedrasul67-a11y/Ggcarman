package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.model.TrafficEntry
import com.example.ui.components.DisclaimerTopBanner
import com.example.ui.components.TrafficItemCard
import com.example.ui.theme.CriticalRed
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkCanvas
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DarkSurfaceHighlight
import com.example.ui.theme.ElectricAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TerminalGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.RulesViewModel
import com.example.ui.viewmodel.TrafficViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrafficScreen(
    trafficViewModel: TrafficViewModel,
    rulesViewModel: RulesViewModel,
    onSelectEntry: (TrafficEntry) -> Unit,
    onOpenBreakpointModal: () -> Unit,
    modifier: Modifier = Modifier
) {
    val entries by trafficViewModel.filteredEntries.collectAsStateWithLifecycle()
    val totalCount by trafficViewModel.rawEntries.collectAsStateWithLifecycle()
    val searchQuery by trafficViewModel.searchQuery.collectAsStateWithLifecycle()
    val methodFilter by trafficViewModel.selectedMethodFilter.collectAsStateWithLifecycle()
    val statusFilter by trafficViewModel.selectedStatusFilter.collectAsStateWithLifecycle()
    val isRecording by trafficViewModel.isRecording.collectAsStateWithLifecycle()
    val proxyStats by trafficViewModel.proxyStats.collectAsStateWithLifecycle()
    val activeBreakpoints by rulesViewModel.activeBreakpoints.collectAsStateWithLifecycle()

    var showQuickTestMenu by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkCanvas)
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (proxyStats.isRunning) TerminalGreen else CriticalRed)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "API FLOW",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(DarkSurfaceElevated)
                            .border(1.dp, DarkBorder, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = ":${proxyStats.port}",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = NeonCyan
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = DarkSurface,
                titleContentColor = TextPrimary
            ),
            actions = {
                // Test Request Generator
                IconButton(
                    onClick = { showQuickTestMenu = !showQuickTestMenu },
                    modifier = Modifier.testTag("quick_test_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FlashOn,
                        contentDescription = "Simulate Request",
                        tint = NeonCyan
                    )
                }

                // Record / Pause toggle
                IconButton(
                    onClick = { trafficViewModel.toggleRecording() },
                    modifier = Modifier.testTag("toggle_recording_button")
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Default.FiberManualRecord else Icons.Default.Pause,
                        contentDescription = "Recording Toggle",
                        tint = if (isRecording) CriticalRed else TextMuted
                    )
                }

                // Clear logs
                IconButton(
                    onClick = { trafficViewModel.clearAll() },
                    modifier = Modifier.testTag("clear_traffic_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ClearAll,
                        contentDescription = "Clear Logs",
                        tint = TextSecondary
                    )
                }
            }
        )

        // Subheader Disclaimer
        DisclaimerTopBanner(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))

        // Active Breakpoint Alert Strip
        if (activeBreakpoints.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(ElectricAmber.copy(alpha = 0.15f))
                    .border(1.dp, ElectricAmber, RoundedCornerShape(8.dp))
                    .clickable { onOpenBreakpointModal() }
                    .padding(12.dp)
                    .testTag("active_breakpoint_banner")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = null,
                            tint = ElectricAmber,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${activeBreakpoints.size} Request(s) Paused at Breakpoint",
                            color = ElectricAmber,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Text(
                        text = "Inspect →",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Quick Test Simulation Dropdown / Bar
        if (showQuickTestMenu) {
            Surface(
                color = DarkSurfaceElevated,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, NeonCyan.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "Simulate Traffic for Testing Mock Rules & Breakpoints:",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonCyan
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        Button(
                            onClick = { trafficViewModel.sendTestRequest("auth") },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceHighlight),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("GET /auth/status (Mock)", fontSize = 11.sp, color = TerminalGreen, fontFamily = FontFamily.Monospace)
                        }

                        Button(
                            onClick = { trafficViewModel.sendTestRequest("user") },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceHighlight),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("GET /users/me", fontSize = 11.sp, color = NeonCyan, fontFamily = FontFamily.Monospace)
                        }

                        Button(
                            onClick = { trafficViewModel.sendTestRequest("post") },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceHighlight),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("POST /audit/logs", fontSize = 11.sp, color = ElectricAmber, fontFamily = FontFamily.Monospace)
                        }

                        Button(
                            onClick = { trafficViewModel.sendTestRequest("ssl_sim") },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceHighlight),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Simulate SSL Pinning Failure", fontSize = 11.sp, color = CriticalRed, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }

        // Live Network Metrics Strip
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(DarkSurface)
                .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Column {
                Text(
                    text = "TOTAL LOGGED",
                    color = TextMuted,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "${totalCount.size} requests",
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Column {
                Text(
                    text = "CONNECTIONS",
                    color = TextMuted,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "${proxyStats.activeConnections} active",
                    color = NeonCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Column {
                Text(
                    text = "TRAFFIC (RX / TX)",
                    color = TextMuted,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "${formatBytes(proxyStats.bytesReceived)} / ${formatBytes(proxyStats.bytesSent)}",
                    color = TerminalGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { trafficViewModel.setSearchQuery(it) },
            placeholder = { Text("Filter by URL, host, or body text...", color = TextMuted, fontSize = 12.sp) },
            leadingIcon = {
                Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = TextMuted, modifier = Modifier.size(18.dp))
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            textStyle = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                color = TextPrimary
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonCyan,
                unfocusedBorderColor = DarkBorder,
                focusedContainerColor = DarkSurface,
                unfocusedContainerColor = DarkSurface
            ),
            shape = RoundedCornerShape(8.dp)
        )

        // Method Filter Chips
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 2.dp)
        ) {
            val methods = listOf("ALL", "GET", "POST", "PUT", "DELETE", "PATCH")
            methods.forEach { m ->
                FilterChip(
                    selected = methodFilter == m,
                    onClick = { trafficViewModel.setMethodFilter(m) },
                    label = { Text(m, fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NeonCyan.copy(alpha = 0.2f),
                        selectedLabelColor = NeonCyan,
                        containerColor = DarkSurface,
                        labelColor = TextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = methodFilter == m,
                        borderColor = DarkBorder,
                        selectedBorderColor = NeonCyan
                    )
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            val statuses = listOf("ALL", "2xx", "3xx", "4xx", "5xx", "ERRORS")
            statuses.forEach { s ->
                FilterChip(
                    selected = statusFilter == s,
                    onClick = { trafficViewModel.setStatusFilter(s) },
                    label = { Text(s, fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = TerminalGreen.copy(alpha = 0.2f),
                        selectedLabelColor = TerminalGreen,
                        containerColor = DarkSurface,
                        labelColor = TextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = statusFilter == s,
                        borderColor = DarkBorder,
                        selectedBorderColor = TerminalGreen
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Traffic Items List
        if (entries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "No Traffic",
                        tint = TextMuted,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (totalCount.isEmpty()) "Waiting for network traffic..." else "No matching requests found",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Configure device proxy to 127.0.0.1:8888 or click Flash button to simulate a test request.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { trafficViewModel.sendTestRequest("auth") },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = DarkCanvas),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Simulate Test Request", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(entries, key = { it.id }) { entry ->
                    TrafficItemCard(
                        entry = entry,
                        onClick = { onSelectEntry(entry) }
                    )
                }
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes > 1024 * 1024 -> String.format(Locale.US, "%.1f MB", bytes / (1024f * 1024f))
        bytes > 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024f)
        else -> "$bytes B"
    }
}
