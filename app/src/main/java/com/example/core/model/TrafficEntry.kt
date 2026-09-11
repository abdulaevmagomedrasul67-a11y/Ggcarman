package com.example.core.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "traffic_entries")
data class TrafficEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val method: String,
    val url: String,
    val host: String,
    val path: String,
    val queryParams: String = "",
    val requestHeadersJson: String = "{}",
    val requestBody: String = "",
    val responseStatus: Int = 0,
    val responseStatusText: String = "",
    val responseHeadersJson: String = "{}",
    val responseBody: String = "",
    val durationMs: Long = 0,
    val requestSizeBytes: Long = 0,
    val responseSizeBytes: Long = 0,
    val contentType: String = "application/json",
    val isHttps: Boolean = false,
    val sslError: String? = null,
    val clientAppPackage: String? = null,
    val wasModified: Boolean = false,
    val mockRuleApplied: String? = null,
    val isBreakpointHit: Boolean = false
) {
    val isSuccess: Boolean
        get() = responseStatus in 200..299

    val isClientError: Boolean
        get() = responseStatus in 400..499

    val isServerError: Boolean
        get() = responseStatus in 500..599

    val isSslFailure: Boolean
        get() = !sslError.isNullOrBlank() || responseStatus == -1
}
