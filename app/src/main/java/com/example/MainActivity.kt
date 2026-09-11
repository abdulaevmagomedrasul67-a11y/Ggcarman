package com.example

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Stream
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.model.TrafficEntry
import com.example.ui.components.ActiveBreakpointModal
import com.example.ui.components.DisclaimerBlockingDialog
import com.example.ui.screens.CaCertScreen
import com.example.ui.screens.RulesScreen
import com.example.ui.screens.TrafficDetailScreen
import com.example.ui.screens.TrafficScreen
import com.example.ui.screens.VpnControlScreen
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkCanvas
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.ElectricAmber
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TerminalGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.RulesViewModel
import com.example.ui.viewmodel.TrafficViewModel

class MainActivity : ComponentActivity() {

    private val trafficViewModel: TrafficViewModel by viewModels()
    private val rulesViewModel: RulesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val prefs = getSharedPreferences("api_flow_prefs", Context.MODE_PRIVATE)
        val hasAcceptedDisclaimer = prefs.getBoolean("has_accepted_disclaimer_v1", false)

        setContent {
            MyApplicationTheme {
                MainAppContainer(
                    trafficViewModel = trafficViewModel,
                    rulesViewModel = rulesViewModel,
                    initialDisclaimerAccepted = hasAcceptedDisclaimer,
                    onAcceptDisclaimer = {
                        prefs.edit().putBoolean("has_accepted_disclaimer_v1", true).apply()
                    },
                    onDeclineDisclaimer = {
                        finish()
                    }
                )
            }
        }
    }
}

enum class NavigationDestination(val title: String) {
    TRAFFIC("Traffic"),
    RULES("Rules & Mocks"),
    CA_CERT("CA & SSL"),
    TUNNEL("Tunnel & Export")
}

@Composable
fun MainAppContainer(
    trafficViewModel: TrafficViewModel,
    rulesViewModel: RulesViewModel,
    initialDisclaimerAccepted: Boolean,
    onAcceptDisclaimer: () -> Unit,
    onDeclineDisclaimer: () -> Unit
) {
    var currentDestination by remember { mutableStateOf(NavigationDestination.TRAFFIC) }
    var selectedTrafficEntry by remember { mutableStateOf<TrafficEntry?>(null) }
    var showDisclaimerDialog by remember { mutableStateOf(!initialDisclaimerAccepted) }
    var showBreakpointModal by remember { mutableStateOf(false) }

    val activeBreakpoints by rulesViewModel.activeBreakpoints.collectAsStateWithLifecycle()
    val trafficEntries by trafficViewModel.rawEntries.collectAsStateWithLifecycle()

    // Show breakpoint modal if any breakpoint occurs
    val currentBreakpoint = activeBreakpoints.firstOrNull()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (selectedTrafficEntry == null) {
                NavigationBar(
                    containerColor = DarkSurface,
                    contentColor = TextPrimary,
                    tonalElevation = 8.dp
                ) {
                    // 1. Traffic Tab
                    NavigationBarItem(
                        selected = currentDestination == NavigationDestination.TRAFFIC,
                        onClick = { currentDestination = NavigationDestination.TRAFFIC },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (trafficEntries.isNotEmpty()) {
                                        Badge(containerColor = NeonCyan, contentColor = DarkCanvas) {
                                            Text(
                                                text = if (trafficEntries.size > 99) "99+" else trafficEntries.size.toString(),
                                                fontSize = 9.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                }
                            ) {
                                Icon(imageVector = Icons.Default.Stream, contentDescription = "Traffic")
                            }
                        },
                        label = { Text("Traffic", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NeonCyan,
                            selectedTextColor = NeonCyan,
                            indicatorColor = DarkBorder,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted
                        ),
                        modifier = Modifier.testTag("nav_traffic")
                    )

                    // 2. Rules Tab
                    NavigationBarItem(
                        selected = currentDestination == NavigationDestination.RULES,
                        onClick = { currentDestination = NavigationDestination.RULES },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (activeBreakpoints.isNotEmpty()) {
                                        Badge(containerColor = ElectricAmber, contentColor = DarkCanvas) {
                                            Text(activeBreakpoints.size.toString(), fontSize = 9.sp)
                                        }
                                    }
                                }
                            ) {
                                Icon(imageVector = Icons.Default.AltRoute, contentDescription = "Rules")
                            }
                        },
                        label = { Text("Rules", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = TerminalGreen,
                            selectedTextColor = TerminalGreen,
                            indicatorColor = DarkBorder,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted
                        ),
                        modifier = Modifier.testTag("nav_rules")
                    )

                    // 3. CA Cert Tab
                    NavigationBarItem(
                        selected = currentDestination == NavigationDestination.CA_CERT,
                        onClick = { currentDestination = NavigationDestination.CA_CERT },
                        icon = {
                            Icon(imageVector = Icons.Default.Key, contentDescription = "CA & SSL")
                        },
                        label = { Text("CA / SSL", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NeonCyan,
                            selectedTextColor = NeonCyan,
                            indicatorColor = DarkBorder,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted
                        ),
                        modifier = Modifier.testTag("nav_cacert")
                    )

                    // 4. Tunnel & Export Tab
                    NavigationBarItem(
                        selected = currentDestination == NavigationDestination.TUNNEL,
                        onClick = { currentDestination = NavigationDestination.TUNNEL },
                        icon = {
                            Icon(imageVector = Icons.Default.Router, contentDescription = "Tunnel")
                        },
                        label = { Text("Tunnel", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = TerminalGreen,
                            selectedTextColor = TerminalGreen,
                            indicatorColor = DarkBorder,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted
                        ),
                        modifier = Modifier.testTag("nav_tunnel")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            if (selectedTrafficEntry != null) {
                TrafficDetailScreen(
                    entry = selectedTrafficEntry!!,
                    onBack = { selectedTrafficEntry = null },
                    rulesViewModel = rulesViewModel
                )
            } else {
                when (currentDestination) {
                    NavigationDestination.TRAFFIC -> TrafficScreen(
                        trafficViewModel = trafficViewModel,
                        rulesViewModel = rulesViewModel,
                        onSelectEntry = { selectedTrafficEntry = it },
                        onOpenBreakpointModal = { showBreakpointModal = true }
                    )
                    NavigationDestination.RULES -> RulesScreen(
                        rulesViewModel = rulesViewModel
                    )
                    NavigationDestination.CA_CERT -> CaCertScreen()
                    NavigationDestination.TUNNEL -> VpnControlScreen(
                        trafficViewModel = trafficViewModel
                    )
                }
            }
        }
    }

    // Active Breakpoint Dialog
    if (showBreakpointModal || currentBreakpoint != null) {
        ActiveBreakpointModal(
            breakpoint = currentBreakpoint,
            onResume = { id, decision ->
                rulesViewModel.resumeBreakpoint(id, decision)
                showBreakpointModal = false
            }
        )
    }

    // Mandatory Disclaimer Dialog
    if (showDisclaimerDialog) {
        DisclaimerBlockingDialog(
            isOpen = showDisclaimerDialog,
            onAccept = {
                showDisclaimerDialog = false
                onAcceptDisclaimer()
            },
            onDecline = {
                onDeclineDisclaimer()
            }
        )
    }
}
