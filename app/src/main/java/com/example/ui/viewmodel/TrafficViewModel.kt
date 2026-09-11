package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ApiFlowApp
import com.example.core.export.TrafficExporter
import com.example.core.model.TrafficEntry
import com.example.core.proxy.ProxyServerStats
import com.example.core.repository.TrafficRepository
import com.example.service.ProxyVpnService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TrafficViewModel(
    private val repository: TrafficRepository = ApiFlowApp.instance.trafficRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedMethodFilter = MutableStateFlow("ALL")
    val selectedMethodFilter: StateFlow<String> = _selectedMethodFilter.asStateFlow()

    private val _selectedStatusFilter = MutableStateFlow("ALL")
    val selectedStatusFilter: StateFlow<String> = _selectedStatusFilter.asStateFlow()

    private val _isRecording = MutableStateFlow(true)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    val proxyStats: StateFlow<ProxyServerStats> = ApiFlowApp.instance.proxyServer.stats
    val isVpnRunning: StateFlow<Boolean> = ProxyVpnService.isVpnRunning

    val rawEntries: StateFlow<List<TrafficEntry>> = repository.allTraffic
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredEntries: StateFlow<List<TrafficEntry>> = combine(
        rawEntries,
        _searchQuery,
        _selectedMethodFilter,
        _selectedStatusFilter
    ) { entries, query, method, status ->
        entries.filter { entry ->
            val matchesMethod = method == "ALL" || entry.method.equals(method, ignoreCase = true)

            val matchesStatus = when (status) {
                "ALL" -> true
                "2xx" -> entry.responseStatus in 200..299
                "3xx" -> entry.responseStatus in 300..399
                "4xx" -> entry.responseStatus in 400..499
                "5xx" -> entry.responseStatus in 500..599
                "ERRORS" -> entry.isSslFailure || entry.responseStatus >= 400
                else -> true
            }

            val matchesQuery = query.isBlank() ||
                    entry.url.contains(query, ignoreCase = true) ||
                    entry.host.contains(query, ignoreCase = true) ||
                    entry.path.contains(query, ignoreCase = true) ||
                    entry.requestBody.contains(query, ignoreCase = true) ||
                    entry.responseBody.contains(query, ignoreCase = true)

            matchesMethod && matchesStatus && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setMethodFilter(method: String) {
        _selectedMethodFilter.value = method
    }

    fun setStatusFilter(status: String) {
        _selectedStatusFilter.value = status
    }

    fun toggleRecording() {
        _isRecording.value = !_isRecording.value
    }

    fun toggleProxy() {
        val server = ApiFlowApp.instance.proxyServer
        if (server.stats.value.isRunning) {
            server.stop()
        } else {
            server.start(8888)
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }

    fun deleteEntry(id: Long) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }

    fun sendTestRequest(type: String) {
        when (type) {
            "auth" -> ApiFlowApp.instance.triggerTestSimulatedRequest(
                method = "GET",
                url = "http://127.0.0.1:8888/api/v1/auth/status"
            )
            "user" -> ApiFlowApp.instance.triggerTestSimulatedRequest(
                method = "GET",
                url = "http://127.0.0.1:8888/api/v1/users/me"
            )
            "post" -> ApiFlowApp.instance.triggerTestSimulatedRequest(
                method = "POST",
                url = "http://127.0.0.1:8888/api/v1/audit/logs",
                body = "{\"action\":\"SECURITY_SCAN\",\"target\":\"internal_gateway\"}"
            )
            "ssl_sim" -> {
                // Simulate an SSL test entry for testing diagnostics
                viewModelScope.launch {
                    repository.insert(
                        TrafficEntry(
                            method = "CONNECT",
                            url = "https://pinned-api.example.com:443",
                            host = "pinned-api.example.com",
                            path = "/",
                            responseStatus = -1,
                            responseStatusText = "SSLHandshakeException (CertPinning)",
                            responseBody = "SSLHandshakeException: Pin verification failed (Certificate pinning active). Refer to SSL Pinning Guide tab for Network Security Config or Frida bypass.",
                            contentType = "text/plain",
                            isHttps = true,
                            sslError = "Trust anchor for certification path not found."
                        )
                    )
                }
            }
        }
    }

    fun exportHar(): String {
        return TrafficExporter.toHar(rawEntries.value)
    }

    fun exportOpenApi(): String {
        return TrafficExporter.toOpenApiSkeleton(rawEntries.value)
    }
}
