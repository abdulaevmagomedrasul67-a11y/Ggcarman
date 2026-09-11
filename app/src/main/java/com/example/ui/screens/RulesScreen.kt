package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.model.BreakpointRule
import com.example.core.model.RewriteRule
import com.example.core.model.RuleActionType
import com.example.ui.components.DisclaimerTopBanner
import com.example.ui.components.MethodBadge
import com.example.ui.theme.CriticalRed
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkCanvas
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DarkSurfaceHighlight
import com.example.ui.theme.ElectricAmber
import com.example.ui.theme.ElectricYellow
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TerminalGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.RulesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RulesScreen(
    rulesViewModel: RulesViewModel,
    modifier: Modifier = Modifier
) {
    val rewriteRules by rulesViewModel.rewriteRules.collectAsStateWithLifecycle()
    val breakpointRules by rulesViewModel.breakpointRules.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf(0) } // 0: Mocking & Rewrite, 1: Breakpoints
    var editingRewriteRule by remember { mutableStateOf<RewriteRule?>(null) }
    var isCreatingRewriteRule by remember { mutableStateOf(false) }

    var editingBreakpointRule by remember { mutableStateOf<BreakpointRule?>(null) }
    var isCreatingBreakpointRule by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkCanvas)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = {
                    Text(
                        text = "RULE ENGINE & MOCKING",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = TextPrimary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface,
                    titleContentColor = TextPrimary
                )
            )

            DisclaimerTopBanner(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = DarkSurface,
                contentColor = NeonCyan,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = NeonCyan
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            text = "Rewrite & Mocking (${rewriteRules.size})",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            text = "Breakpoints (${breakpointRules.size})",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }

            if (selectedTab == 0) {
                // Rewrite & Mocking Rules Tab
                if (rewriteRules.isEmpty()) {
                    EmptyRulesPlaceholder(
                        title = "No Mocking or Rewrite Rules",
                        subtitle = "Add rules to stub responses, mock API payloads, or modify headers on the fly."
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(rewriteRules, key = { it.id }) { rule ->
                            RewriteRuleCard(
                                rule = rule,
                                onToggle = { rulesViewModel.toggleRewriteRule(rule) },
                                onEdit = { editingRewriteRule = rule },
                                onDelete = { rulesViewModel.deleteRewriteRule(rule) }
                            )
                        }
                    }
                }
            } else {
                // Breakpoints Tab
                if (breakpointRules.isEmpty()) {
                    EmptyRulesPlaceholder(
                        title = "No Breakpoint Rules",
                        subtitle = "Define URL patterns to pause requests in flight and edit parameters before they reach the server."
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(breakpointRules, key = { it.id }) { bp ->
                            BreakpointRuleCard(
                                breakpoint = bp,
                                onToggle = { rulesViewModel.toggleBreakpointRule(bp) },
                                onEdit = { editingBreakpointRule = bp },
                                onDelete = { rulesViewModel.deleteBreakpointRule(bp) }
                            )
                        }
                    }
                }
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = {
                if (selectedTab == 0) {
                    isCreatingRewriteRule = true
                } else {
                    isCreatingBreakpointRule = true
                }
            },
            containerColor = if (selectedTab == 0) TerminalGreen else ElectricAmber,
            contentColor = DarkCanvas,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_rule_fab")
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Rule")
        }
    }

    // Dialog for creating/editing rewrite rules
    if (isCreatingRewriteRule || editingRewriteRule != null) {
        val initialRule = editingRewriteRule ?: RewriteRule(
            name = "New Mock Rule",
            targetHostPattern = "*",
            targetPathPattern = "*/api/*",
            targetMethod = "GET",
            actionType = RuleActionType.MOCK_RESPONSE,
            mockStatusCode = 200,
            mockContentType = "application/json",
            mockResponseBody = "{\n  \"status\": \"success\",\n  \"mocked\": true\n}"
        )

        RewriteRuleDialog(
            initialRule = initialRule,
            onDismiss = {
                isCreatingRewriteRule = false
                editingRewriteRule = null
            },
            onSave = { saved ->
                rulesViewModel.saveRewriteRule(saved)
                isCreatingRewriteRule = false
                editingRewriteRule = null
            }
        )
    }

    // Dialog for creating/editing breakpoint rules
    if (isCreatingBreakpointRule || editingBreakpointRule != null) {
        val initialBp = editingBreakpointRule ?: BreakpointRule(
            name = "Pause /checkout requests",
            urlPattern = ".*/checkout.*",
            method = "POST",
            pauseOnRequest = true,
            pauseOnResponse = false
        )

        BreakpointRuleDialog(
            initial = initialBp,
            onDismiss = {
                isCreatingBreakpointRule = false
                editingBreakpointRule = null
            },
            onSave = { saved ->
                rulesViewModel.saveBreakpointRule(saved)
                isCreatingBreakpointRule = false
                editingBreakpointRule = null
            }
        )
    }
}

@Composable
private fun RewriteRuleCard(
    rule: RewriteRule,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(10.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(if (rule.isEnabled) NeonCyan.copy(alpha = 0.5f) else DarkBorder)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MethodBadge(method = rule.targetMethod)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = rule.name,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Switch(
                    checked = rule.isEnabled,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = DarkCanvas,
                        checkedTrackColor = TerminalGreen,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = DarkSurfaceElevated
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Patterns
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Path: ${rule.targetPathPattern}",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Action details
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (rule.actionType == RuleActionType.MOCK_RESPONSE) ElectricYellow.copy(alpha = 0.15f) else NeonCyan.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = when (rule.actionType) {
                            RuleActionType.MOCK_RESPONSE -> "MOCK ${rule.mockStatusCode} (${rule.mockContentType})"
                            RuleActionType.REPLACE_HEADER -> "HEADER: ${rule.targetHeaderKey}"
                            RuleActionType.ADD_QUERY_PARAM -> "PARAM: ${rule.targetParamKey}"
                            RuleActionType.URL_REDIRECT -> "REDIRECT"
                        },
                        color = if (rule.actionType == RuleActionType.MOCK_RESPONSE) ElectricYellow else NeonCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = TextSecondary, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = CriticalRed, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun BreakpointRuleCard(
    breakpoint: BreakpointRule,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(10.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(if (breakpoint.isEnabled) ElectricAmber.copy(alpha = 0.5f) else DarkBorder)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.BugReport, contentDescription = null, tint = ElectricAmber, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = breakpoint.name,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Switch(
                    checked = breakpoint.isEnabled,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = DarkCanvas,
                        checkedTrackColor = ElectricAmber,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = DarkSurfaceElevated
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Regex: ${breakpoint.urlPattern} (${breakpoint.method})",
                color = TextSecondary,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (breakpoint.pauseOnRequest) "PAUSE ON REQUEST" else "PAUSE ON RESPONSE",
                    color = ElectricAmber,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = TextSecondary, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = CriticalRed, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyRulesPlaceholder(title: String, subtitle: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun RewriteRuleDialog(
    initialRule: RewriteRule,
    onDismiss: () -> Unit,
    onSave: (RewriteRule) -> Unit
) {
    var name by remember { mutableStateOf(initialRule.name) }
    var pathPattern by remember { mutableStateOf(initialRule.targetPathPattern) }
    var hostPattern by remember { mutableStateOf(initialRule.targetHostPattern) }
    var method by remember { mutableStateOf(initialRule.targetMethod) }
    var statusCode by remember { mutableStateOf(initialRule.mockStatusCode.toString()) }
    var responseBody by remember { mutableStateOf(initialRule.mockResponseBody) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Text(
                text = if (initialRule.id == 0L) "NEW MOCK / REWRITE RULE" else "EDIT RULE",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = NeonCyan
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Rule Name", color = TextSecondary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = pathPattern,
                    onValueChange = { pathPattern = it },
                    label = { Text("Path Pattern (e.g. */auth/*)", color = TextSecondary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = method,
                        onValueChange = { method = it.uppercase() },
                        label = { Text("Method", color = TextSecondary) },
                        singleLine = true,
                        modifier = Modifier.weight(0.5f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    OutlinedTextField(
                        value = statusCode,
                        onValueChange = { statusCode = it },
                        label = { Text("Status Code", color = TextSecondary) },
                        singleLine = true,
                        modifier = Modifier.weight(0.5f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text("Mock Response Body (JSON):", color = TextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = responseBody,
                    onValueChange = { responseBody = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        initialRule.copy(
                            name = name,
                            targetPathPattern = pathPattern,
                            targetHostPattern = hostPattern,
                            targetMethod = method,
                            mockStatusCode = statusCode.toIntOrNull() ?: 200,
                            mockResponseBody = responseBody
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = TerminalGreen, contentColor = DarkCanvas),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Save Rule", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun BreakpointRuleDialog(
    initial: BreakpointRule,
    onDismiss: () -> Unit,
    onSave: (BreakpointRule) -> Unit
) {
    var name by remember { mutableStateOf(initial.name) }
    var pattern by remember { mutableStateOf(initial.urlPattern) }
    var method by remember { mutableStateOf(initial.method) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Text(
                text = "BREAKPOINT RULE",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = ElectricAmber
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name", color = TextSecondary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricAmber,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = pattern,
                    onValueChange = { pattern = it },
                    label = { Text("URL Regex Pattern (e.g. .*checkout.*)", color = TextSecondary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricAmber,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = method,
                    onValueChange = { method = it.uppercase() },
                    label = { Text("Method (ALL, POST, GET...)", color = TextSecondary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricAmber,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(initial.copy(name = name, urlPattern = pattern, method = method))
                },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricAmber, contentColor = DarkCanvas),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Save Breakpoint", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(8.dp)) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
