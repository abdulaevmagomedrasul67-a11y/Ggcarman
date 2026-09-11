package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.export.TrafficExporter
import com.example.core.model.RewriteRule
import com.example.core.model.RuleActionType
import com.example.core.model.TrafficEntry
import com.example.core.security.CertificateManager
import com.example.ui.components.MethodBadge
import com.example.ui.components.StatusBadge
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
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrafficDetailScreen(
    entry: TrafficEntry,
    onBack: () -> Unit,
    rulesViewModel: RulesViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Request", "Response", "Overview", "Export / cURL", "SSL Diagnostics")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkCanvas)
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MethodBadge(method = entry.method)
                    Spacer(modifier = Modifier.width(8.dp))
                    StatusBadge(statusCode = entry.responseStatus, isSslError = entry.isSslFailure)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = entry.path.ifBlank { "/" },
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        maxLines = 1,
                        fontFamily = FontFamily.Monospace
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
            },
            actions = {
                IconButton(onClick = {
                    val curl = TrafficExporter.toCurl(entry)
                    copyToClipboard(context, "cURL Command", curl)
                }) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy cURL",
                        tint = NeonCyan
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = DarkSurface,
                titleContentColor = TextPrimary
            )
        )

        // URL Header banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurfaceElevated)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (entry.isHttps) Icons.Default.Lock else Icons.Default.LockOpen,
                    contentDescription = null,
                    tint = if (entry.isHttps) NeonCyan else TextMuted,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = entry.url,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 2
                )
            }
        }

        // Tabs
        ScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = DarkSurface,
            contentColor = NeonCyan,
            edgePadding = 12.dp,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                    color = NeonCyan
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = {
                        Text(
                            text = title,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        // Tab Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            when (selectedTabIndex) {
                0 -> RequestTab(entry, context)
                1 -> ResponseTab(entry, context, rulesViewModel)
                2 -> OverviewTab(entry, context)
                3 -> ExportTab(entry, context)
                4 -> SslDiagnosticsTab(entry, context)
            }
        }
    }
}

@Composable
private fun RequestTab(entry: TrafficEntry, context: Context) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Headers Section
        SectionHeader("Request Headers")
        val headersMap = remember(entry.requestHeadersJson) {
            parseHeaders(entry.requestHeadersJson)
        }
        HeadersTable(headersMap)

        Spacer(modifier = Modifier.height(16.dp))

        // Query Parameters Section
        if (entry.queryParams.isNotBlank()) {
            SectionHeader("Query Parameters")
            val paramsMap = remember(entry.queryParams) {
                entry.queryParams.split("&").mapNotNull {
                    val parts = it.split("=")
                    if (parts.size >= 2) parts[0] to parts[1] else null
                }
            }
            KeyValueTable(paramsMap)
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Body Section
        SectionHeader("Request Body (${entry.requestSizeBytes} bytes)")
        if (entry.requestBody.isNotBlank()) {
            CodeViewer(content = formatIfJson(entry.requestBody), context = context)
        } else {
            Text(
                text = "No Request Body",
                color = TextMuted,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun ResponseTab(entry: TrafficEntry, context: Context, rulesViewModel: RulesViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            SectionHeader("Status: ${entry.responseStatus} ${entry.responseStatusText}")

            Button(
                onClick = {
                    rulesViewModel.saveRewriteRule(
                        RewriteRule(
                            name = "Mock ${entry.path}",
                            targetHostPattern = entry.host,
                            targetPathPattern = entry.path,
                            targetMethod = entry.method,
                            actionType = RuleActionType.MOCK_RESPONSE,
                            mockStatusCode = if (entry.responseStatus > 0) entry.responseStatus else 200,
                            mockContentType = entry.contentType,
                            mockResponseBody = entry.responseBody
                        )
                    )
                    Toast.makeText(context, "Saved as Mocking Rule!", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceHighlight),
                shape = RoundedCornerShape(6.dp)
            ) {
                Icon(imageVector = Icons.Default.AutoFixHigh, contentDescription = null, tint = TerminalGreen, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Mock This API", fontSize = 11.sp, color = TerminalGreen, fontFamily = FontFamily.Monospace)
            }
        }

        val headersMap = remember(entry.responseHeadersJson) {
            parseHeaders(entry.responseHeadersJson)
        }
        HeadersTable(headersMap)

        Spacer(modifier = Modifier.height(16.dp))

        SectionHeader("Response Body (${entry.responseSizeBytes} bytes)")
        if (entry.responseBody.isNotBlank()) {
            CodeViewer(content = formatIfJson(entry.responseBody), context = context)
        } else {
            Text(
                text = if (entry.isSslFailure) "Connection failed before receiving response headers" else "Empty Response Body",
                color = TextMuted,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun OverviewTab(entry: TrafficEntry, context: Context) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        SectionHeader("Transaction Details")

        val details = listOf(
            "Method" to entry.method,
            "URL" to entry.url,
            "Host" to entry.host,
            "Path" to entry.path,
            "Status" to "${entry.responseStatus} (${entry.responseStatusText})",
            "Duration" to "${entry.durationMs} ms",
            "Protocol" to if (entry.isHttps) "HTTPS / TLS" else "HTTP / Plaintext",
            "Content-Type" to entry.contentType,
            "Request Size" to "${entry.requestSizeBytes} bytes",
            "Response Size" to "${entry.responseSizeBytes} bytes",
            "Was Modified" to if (entry.wasModified) "Yes (Rewrite Rule / Breakpoint)" else "No",
            "Mock Rule" to (entry.mockRuleApplied ?: "None")
        )

        KeyValueTable(details)
    }
}

@Composable
private fun ExportTab(entry: TrafficEntry, context: Context) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        SectionHeader("cURL Command")
        val curl = remember(entry.id) { TrafficExporter.toCurl(entry) }
        CodeViewer(content = curl, context = context)

        Spacer(modifier = Modifier.height(16.dp))

        SectionHeader("RAW HTTP Request")
        val rawReq = remember(entry.id) { TrafficExporter.toRawHttpRequest(entry) }
        CodeViewer(content = rawReq, context = context)

        Spacer(modifier = Modifier.height(16.dp))

        SectionHeader("RAW HTTP Response")
        val rawResp = remember(entry.id) { TrafficExporter.toRawHttpResponse(entry) }
        CodeViewer(content = rawResp, context = context)

        Spacer(modifier = Modifier.height(16.dp))

        SectionHeader("OpenAPI 3.0 Operation Skeleton")
        val openApi = remember(entry.id) { TrafficExporter.toOpenApiSkeleton(listOf(entry)) }
        CodeViewer(content = openApi, context = context)
    }
}

@Composable
private fun SslDiagnosticsTab(entry: TrafficEntry, context: Context) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        if (entry.isSslFailure) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(CriticalRed.copy(alpha = 0.15f))
                    .border(1.dp, CriticalRed, RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = CriticalRed)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SSLHandshakeException Encountered",
                            color = CriticalRed,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Error: ${entry.sslError ?: "Certificate untrusted or SSL Pinning active"}",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        SectionHeader("1. Trust User CA in Debug Builds (network_security_config.xml)")
        Text(
            text = "By default on Android 7+, apps do not trust user-installed CA certificates unless configured in network security config:",
            color = TextSecondary,
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        CodeViewer(content = CertificateManager.getNetworkSecurityConfigSample(), context = context)

        Spacer(modifier = Modifier.height(16.dp))

        SectionHeader("2. AndroidManifest.xml Configuration")
        CodeViewer(content = CertificateManager.getAndroidManifestConfigSnippet(), context = context)

        Spacer(modifier = Modifier.height(16.dp))

        SectionHeader("3. Frida Dynamic SSL Pinning Bypass (For authorized audit)")
        CodeViewer(content = CertificateManager.getFridaBypassSnippet(), context = context)
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = NeonCyan,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

@Composable
private fun HeadersTable(headers: List<Pair<String, String>>) {
    if (headers.isEmpty()) {
        Text("No Headers", color = TextMuted, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurface)
            .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
    ) {
        headers.forEachIndexed { idx, (key, value) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (idx % 2 == 0) DarkSurface else DarkSurfaceElevated)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = key,
                    color = NeonCyan,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(0.35f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = value,
                    color = TextPrimary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(0.65f)
                )
            }
        }
    }
}

@Composable
private fun KeyValueTable(pairs: List<Pair<String, String>>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurface)
            .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
    ) {
        pairs.forEachIndexed { idx, (key, value) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (idx % 2 == 0) DarkSurface else DarkSurfaceElevated)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = key,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(0.35f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = value,
                    color = TextPrimary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(0.65f)
                )
            }
        }
    }
}

@Composable
private fun CodeViewer(content: String, context: Context) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurface)
            .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Column {
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = { copyToClipboard(context, "Code", content) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        tint = NeonCyan,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Text(
                text = content,
                color = TextPrimary,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 17.sp
            )
        }
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "$label copied to clipboard", Toast.LENGTH_SHORT).show()
}

private fun parseHeaders(json: String): List<Pair<String, String>> {
    return try {
        val obj = JSONObject(json)
        val list = mutableListOf<Pair<String, String>>()
        val keys = obj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            list.add(key to obj.getString(key))
        }
        list
    } catch (_: Exception) {
        emptyList()
    }
}

private fun formatIfJson(text: String): String {
    return try {
        if (text.trim().startsWith("{")) {
            JSONObject(text).toString(2)
        } else if (text.trim().startsWith("[")) {
            org.json.JSONArray(text).toString(2)
        } else {
            text
        }
    } catch (_: Exception) {
        text
    }
}
