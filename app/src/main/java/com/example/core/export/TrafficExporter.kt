package com.example.core.export

import com.example.core.model.TrafficEntry
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object TrafficExporter {

    fun toCurl(entry: TrafficEntry): String {
        val sb = StringBuilder()
        sb.append("curl -X ").append(entry.method).append(" '").append(entry.url).append("'")

        try {
            val headersObj = JSONObject(entry.requestHeadersJson)
            val keys = headersObj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val value = headersObj.getString(key)
                // Filter out pseudo-headers
                if (!key.startsWith(":")) {
                    sb.append(" \\\n  -H '").append(key).append(": ").append(value.replace("'", "\\'")).append("'")
                }
            }
        } catch (_: Exception) {}

        if (entry.requestBody.isNotBlank()) {
            val escapedBody = entry.requestBody.replace("'", "'\\''")
            sb.append(" \\\n  --data-raw '").append(escapedBody).append("'")
        }

        return sb.toString()
    }

    fun toRawHttpRequest(entry: TrafficEntry): String {
        val sb = StringBuilder()
        val pathWithQuery = if (entry.queryParams.isNotBlank()) "${entry.path}?${entry.queryParams}" else entry.path
        sb.append("${entry.method} $pathWithQuery HTTP/1.1\r\n")
        sb.append("Host: ${entry.host}\r\n")

        try {
            val headersObj = JSONObject(entry.requestHeadersJson)
            val keys = headersObj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                if (!key.equals("Host", ignoreCase = true) && !key.startsWith(":")) {
                    sb.append("$key: ${headersObj.getString(key)}\r\n")
                }
            }
        } catch (_: Exception) {}

        sb.append("\r\n")
        if (entry.requestBody.isNotBlank()) {
            sb.append(entry.requestBody)
        }
        return sb.toString()
    }

    fun toRawHttpResponse(entry: TrafficEntry): String {
        val sb = StringBuilder()
        val statusText = if (entry.responseStatusText.isNotBlank()) entry.responseStatusText else "OK"
        sb.append("HTTP/1.1 ${entry.responseStatus} $statusText\r\n")

        try {
            val headersObj = JSONObject(entry.responseHeadersJson)
            val keys = headersObj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                if (!key.startsWith(":")) {
                    sb.append("$key: ${headersObj.getString(key)}\r\n")
                }
            }
        } catch (_: Exception) {}

        sb.append("\r\n")
        if (entry.responseBody.isNotBlank()) {
            sb.append(entry.responseBody)
        }
        return sb.toString()
    }

    fun toHar(entries: List<TrafficEntry>): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        val logObj = JSONObject()
        logObj.put("version", "1.2")

        val creator = JSONObject().apply {
            put("name", "API Flow Inspector")
            put("version", "1.0.0")
        }
        logObj.put("creator", creator)

        val harEntries = JSONArray()
        for (item in entries) {
            val entryObj = JSONObject()
            entryObj.put("startedDateTime", dateFormat.format(Date(item.timestamp)))
            entryObj.put("time", item.durationMs)

            // Request
            val reqObj = JSONObject().apply {
                put("method", item.method)
                put("url", item.url)
                put("httpVersion", "HTTP/1.1")

                val headersArr = JSONArray()
                try {
                    val hObj = JSONObject(item.requestHeadersJson)
                    val k = hObj.keys()
                    while (k.hasNext()) {
                        val key = k.next()
                        headersArr.put(JSONObject().apply {
                            put("name", key)
                            put("value", hObj.getString(key))
                        })
                    }
                } catch (_: Exception) {}
                put("headers", headersArr)

                val postData = JSONObject().apply {
                    put("mimeType", "application/json")
                    put("text", item.requestBody)
                }
                put("postData", postData)
                put("headersSize", item.requestHeadersJson.length)
                put("bodySize", item.requestSizeBytes)
            }
            entryObj.put("request", reqObj)

            // Response
            val resObj = JSONObject().apply {
                put("status", item.responseStatus)
                put("statusText", item.responseStatusText)
                put("httpVersion", "HTTP/1.1")

                val headersArr = JSONArray()
                try {
                    val hObj = JSONObject(item.responseHeadersJson)
                    val k = hObj.keys()
                    while (k.hasNext()) {
                        val key = k.next()
                        headersArr.put(JSONObject().apply {
                            put("name", key)
                            put("value", hObj.getString(key))
                        })
                    }
                } catch (_: Exception) {}
                put("headers", headersArr)

                val contentObj = JSONObject().apply {
                    put("size", item.responseSizeBytes)
                    put("mimeType", item.contentType)
                    put("text", item.responseBody)
                }
                put("content", contentObj)
                put("headersSize", item.responseHeadersJson.length)
                put("bodySize", item.responseSizeBytes)
            }
            entryObj.put("response", resObj)

            val timings = JSONObject().apply {
                put("send", 1)
                put("wait", (item.durationMs - 2).coerceAtLeast(0))
                put("receive", 1)
            }
            entryObj.put("timings", timings)

            harEntries.put(entryObj)
        }

        logObj.put("entries", harEntries)

        val root = JSONObject()
        root.put("log", logObj)
        return root.toString(2)
    }

    fun toOpenApiSkeleton(entries: List<TrafficEntry>): String {
        val root = JSONObject()
        root.put("openapi", "3.0.3")

        val info = JSONObject().apply {
            put("title", "Exported API Flow Specification")
            put("version", "1.0.0")
            put("description", "Generated dynamically from inspected network traffic sessions")
        }
        root.put("info", info)

        val paths = JSONObject()
        for (item in entries) {
            val pathKey = item.path.ifBlank { "/" }
            val pathObj = if (paths.has(pathKey)) paths.getJSONObject(pathKey) else JSONObject()
            val methodKey = item.method.lowercase()

            val opObj = JSONObject().apply {
                put("summary", "${item.method} $pathKey")
                put("operationId", "${item.method.lowercase()}_${item.path.replace("/", "_").trim('_')}")

                val responses = JSONObject()
                val respStatusObj = JSONObject().apply {
                    put("description", "Response status ${item.responseStatus}")
                    val contentObj = JSONObject()
                    val mediaObj = JSONObject()
                    if (item.responseBody.isNotBlank()) {
                        try {
                            // Validate if it's JSON
                            JSONObject(item.responseBody)
                            mediaObj.put("example", JSONObject(item.responseBody))
                        } catch (_: Exception) {
                            mediaObj.put("example", item.responseBody)
                        }
                    }
                    contentObj.put(item.contentType.ifBlank { "application/json" }, mediaObj)
                    put("content", contentObj)
                }
                responses.put(if (item.responseStatus > 0) item.responseStatus.toString() else "default", respStatusObj)
                put("responses", responses)
            }

            pathObj.put(methodKey, opObj)
            paths.put(pathKey, pathObj)
        }
        root.put("paths", paths)

        return root.toString(2)
    }
}
