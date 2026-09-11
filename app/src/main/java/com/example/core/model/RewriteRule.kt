package com.example.core.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class RuleActionType {
    MOCK_RESPONSE,
    REPLACE_HEADER,
    ADD_QUERY_PARAM,
    URL_REDIRECT
}

@Entity(tableName = "rewrite_rules")
data class RewriteRule(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val isEnabled: Boolean = true,
    val targetHostPattern: String = "*",
    val targetPathPattern: String = "*",
    val targetMethod: String = "ALL", // ALL, GET, POST, etc.
    val actionType: RuleActionType = RuleActionType.MOCK_RESPONSE,
    val targetHeaderKey: String? = null,
    val targetHeaderValue: String? = null,
    val targetParamKey: String? = null,
    val targetParamValue: String? = null,
    val mockStatusCode: Int = 200,
    val mockContentType: String = "application/json",
    val mockResponseBody: String = "{\n  \"status\": \"mocked_by_api_flow\",\n  \"success\": true\n}"
)

@Entity(tableName = "breakpoint_rules")
data class BreakpointRule(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val isEnabled: Boolean = true,
    val urlPattern: String = ".*",
    val method: String = "ALL",
    val pauseOnRequest: Boolean = true,
    val pauseOnResponse: Boolean = false
)
