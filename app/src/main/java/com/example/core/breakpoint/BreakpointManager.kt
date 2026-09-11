package com.example.core.breakpoint

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

enum class BreakpointStage {
    REQUEST,
    RESPONSE
}

sealed class BreakpointDecision {
    data class Forward(
        val modifiedHeaders: Map<String, String>,
        val modifiedBody: String
    ) : BreakpointDecision()

    data class Mock(
        val statusCode: Int,
        val statusText: String,
        val headers: Map<String, String>,
        val body: String
    ) : BreakpointDecision()

    object Drop : BreakpointDecision()
}

data class ActiveBreakpoint(
    val id: String = UUID.randomUUID().toString(),
    val stage: BreakpointStage,
    val method: String,
    val url: String,
    val host: String,
    val path: String,
    val headers: Map<String, String>,
    val body: String,
    val statusCode: Int = 200,
    val timestamp: Long = System.currentTimeMillis(),
    val deferred: CompletableDeferred<BreakpointDecision>
)

object BreakpointManager {
    private val _activeBreakpoints = MutableStateFlow<List<ActiveBreakpoint>>(emptyList())
    val activeBreakpoints: StateFlow<List<ActiveBreakpoint>> = _activeBreakpoints.asStateFlow()

    suspend fun pauseRequest(
        method: String,
        url: String,
        host: String,
        path: String,
        headers: Map<String, String>,
        body: String
    ): BreakpointDecision {
        val deferred = CompletableDeferred<BreakpointDecision>()
        val breakpoint = ActiveBreakpoint(
            stage = BreakpointStage.REQUEST,
            method = method,
            url = url,
            host = host,
            path = path,
            headers = headers,
            body = body,
            deferred = deferred
        )

        _activeBreakpoints.value = _activeBreakpoints.value + breakpoint

        return try {
            deferred.await()
        } finally {
            _activeBreakpoints.value = _activeBreakpoints.value.filterNot { it.id == breakpoint.id }
        }
    }

    suspend fun pauseResponse(
        method: String,
        url: String,
        host: String,
        path: String,
        statusCode: Int,
        headers: Map<String, String>,
        body: String
    ): BreakpointDecision {
        val deferred = CompletableDeferred<BreakpointDecision>()
        val breakpoint = ActiveBreakpoint(
            stage = BreakpointStage.RESPONSE,
            method = method,
            url = url,
            host = host,
            path = path,
            statusCode = statusCode,
            headers = headers,
            body = body,
            deferred = deferred
        )

        _activeBreakpoints.value = _activeBreakpoints.value + breakpoint

        return try {
            deferred.await()
        } finally {
            _activeBreakpoints.value = _activeBreakpoints.value.filterNot { it.id == breakpoint.id }
        }
    }

    fun resume(breakpointId: String, decision: BreakpointDecision) {
        val bp = _activeBreakpoints.value.find { it.id == breakpointId }
        bp?.deferred?.complete(decision)
    }

    fun dropAll() {
        val list = _activeBreakpoints.value
        list.forEach { it.deferred.complete(BreakpointDecision.Drop) }
        _activeBreakpoints.value = emptyList()
    }
}
