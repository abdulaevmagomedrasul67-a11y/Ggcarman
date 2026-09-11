package com.example.core.proxy

import android.util.Log
import com.example.core.breakpoint.BreakpointDecision
import com.example.core.breakpoint.BreakpointManager
import com.example.core.model.RewriteRule
import com.example.core.model.RuleActionType
import com.example.core.model.TrafficEntry
import com.example.core.repository.RulesRepository
import com.example.core.repository.TrafficRepository
import com.example.core.security.CertificateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLException
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

data class ProxyServerStats(
    val isRunning: Boolean = false,
    val port: Int = 8888,
    val bytesReceived: Long = 0,
    val bytesSent: Long = 0,
    val totalRequests: Long = 0,
    val activeConnections: Int = 0,
    val lastError: String? = null
)

class HttpProxyServer(
    private val trafficRepository: TrafficRepository,
    private val rulesRepository: RulesRepository,
    private val certificateManager: CertificateManager,
    private val scope: CoroutineScope
) {
    private val tag = "HttpProxyServer"
    private var serverSocket: ServerSocket? = null
    private var isAccepting = false

    private val _stats = MutableStateFlow(ProxyServerStats())
    val stats: StateFlow<ProxyServerStats> = _stats.asStateFlow()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .followRedirects(false)
        .build()

    @Synchronized
    fun start(port: Int = 8888) {
        if (isAccepting) return
        try {
            serverSocket = ServerSocket(port)
            isAccepting = true
            _stats.value = _stats.value.copy(
                isRunning = true,
                port = port,
                lastError = null
            )
            Log.i(tag, "API Flow Proxy Server listening on port $port")

            scope.launch(Dispatchers.IO) {
                while (isAccepting) {
                    try {
                        val clientSocket = serverSocket?.accept() ?: break
                        updateActiveConnections(1)
                        scope.launch(Dispatchers.IO) {
                            try {
                                handleClientConnection(clientSocket)
                            } catch (e: Exception) {
                                Log.d(tag, "Client connection ended: ${e.message}")
                            } finally {
                                updateActiveConnections(-1)
                                try { clientSocket.close() } catch (_: Exception) {}
                            }
                        }
                    } catch (e: SocketException) {
                        if (!isAccepting) break
                        Log.e(tag, "ServerSocket accept exception", e)
                    } catch (e: Exception) {
                        Log.e(tag, "Error in proxy accept loop", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to start proxy on port $port", e)
            _stats.value = _stats.value.copy(
                isRunning = false,
                lastError = e.message ?: "Failed to bind port"
            )
        }
    }

    @Synchronized
    fun stop() {
        isAccepting = false
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            Log.e(tag, "Error closing server socket", e)
        }
        serverSocket = null
        _stats.value = _stats.value.copy(isRunning = false, activeConnections = 0)
        Log.i(tag, "API Flow Proxy Server stopped")
    }

    private fun updateActiveConnections(delta: Int) {
        _stats.value = _stats.value.copy(
            activeConnections = (_stats.value.activeConnections + delta).coerceAtLeast(0)
        )
    }

    private fun addBytes(received: Long, sent: Long) {
        _stats.value = _stats.value.copy(
            bytesReceived = _stats.value.bytesReceived + received,
            bytesSent = _stats.value.bytesSent + sent
        )
    }

    private fun incrementRequests() {
        _stats.value = _stats.value.copy(
            totalRequests = _stats.value.totalRequests + 1
        )
    }

    private suspend fun handleClientConnection(clientSocket: Socket) {
        val inputStream = clientSocket.getInputStream()
        val outputStream = clientSocket.getOutputStream()

        val reader = BufferedReader(InputStreamReader(inputStream, Charsets.ISO_8859_1))
        val initialLine = reader.readLine() ?: return
        val parts = initialLine.split(" ")
        if (parts.size < 2) return

        val method = parts[0].uppercase()
        val target = parts[1]

        if (method == "CONNECT") {
            // HTTPS Tunneling
            handleHttpsConnect(clientSocket, inputStream, outputStream, target)
        } else {
            // Plain HTTP Request
            handlePlainHttpRequest(inputStream, outputStream, method, target, reader)
        }
    }

    private suspend fun handleHttpsConnect(
        clientSocket: Socket,
        clientIn: InputStream,
        clientOut: OutputStream,
        targetHostPort: String
    ) {
        // Read client CONNECT headers
        val hostParts = targetHostPort.split(":")
        val host = hostParts[0]
        val port = hostParts.getOrNull(1)?.toIntOrNull() ?: 443

        // Consume headers until blank line
        var line: String?
        val reader = BufferedReader(InputStreamReader(clientIn, Charsets.ISO_8859_1))
        while (true) {
            line = reader.readLine()
            if (line.isNullOrEmpty()) break
        }

        // Send 200 Connection Established to client
        val establishedMsg = "HTTP/1.1 200 Connection Established\r\nProxy-Agent: API-Flow-Inspector/1.0\r\n\r\n"
        clientOut.write(establishedMsg.toByteArray(Charsets.ISO_8859_1))
        clientOut.flush()
        addBytes(0, establishedMsg.length.toLong())

        // Upgrade client socket to TLS using our dynamically signed certificate
        val sslContext = try {
            certificateManager.getOrCreateSslContextForHost(host)
        } catch (e: Exception) {
            Log.e(tag, "Failed to create SSLContext for host $host", e)
            return
        }

        val sslSocketFactory: SSLSocketFactory = sslContext.socketFactory
        val sslClientSocket: SSLSocket = try {
            sslSocketFactory.createSocket(
                clientSocket,
                host,
                clientSocket.port,
                true
            ) as SSLSocket
        } catch (e: Exception) {
            Log.e(tag, "Failed to wrap SSLSocket for $host", e)
            return
        }

        sslClientSocket.useClientMode = false

        try {
            sslClientSocket.startHandshake()
        } catch (sslEx: SSLException) {
            // SSL Handshake failed! Most commonly due to SSL Pinning or untrusted User CA in the target app
            Log.w(tag, "SSL Handshake failed for $host: ${sslEx.message}")
            recordSslHandshakeFailure(host, sslEx.message ?: "SSLHandshakeException (Certificate untrusted or SSL Pinning active)")
            return
        }

        // Handle decrypted HTTPS traffic inside TLS connection
        val sslIn = sslClientSocket.inputStream
        val sslOut = sslClientSocket.outputStream
        val sslReader = BufferedReader(InputStreamReader(sslIn, Charsets.ISO_8859_1))

        val decryptedInitialLine = sslReader.readLine() ?: return
        val reqParts = decryptedInitialLine.split(" ")
        if (reqParts.size < 2) return

        val decryptedMethod = reqParts[0].uppercase()
        val path = reqParts[1]
        val fullUrl = "https://$host$path"

        processHttpRequest(
            isHttps = true,
            method = decryptedMethod,
            url = fullUrl,
            host = host,
            path = path,
            reader = sslReader,
            rawIn = sslIn,
            rawOut = sslOut
        )
    }

    private suspend fun handlePlainHttpRequest(
        rawIn: InputStream,
        rawOut: OutputStream,
        method: String,
        target: String,
        reader: BufferedReader
    ) {
        val fullUrl = if (target.startsWith("http://") || target.startsWith("https://")) {
            target
        } else {
            "http://$target"
        }

        val parsedUri = try {
            java.net.URI.create(fullUrl)
        } catch (_: Exception) {
            null
        }

        val host = parsedUri?.host ?: target.substringBefore("/").substringBefore(":")
        val path = parsedUri?.rawPath ?: "/"

        processHttpRequest(
            isHttps = false,
            method = method,
            url = fullUrl,
            host = host,
            path = path,
            reader = reader,
            rawIn = rawIn,
            rawOut = rawOut
        )
    }

    private suspend fun processHttpRequest(
        isHttps: Boolean,
        method: String,
        url: String,
        host: String,
        path: String,
        reader: BufferedReader,
        rawIn: InputStream,
        rawOut: OutputStream
    ) {
        val startTime = System.currentTimeMillis()
        incrementRequests()

        // 1. Read Headers
        val headers = mutableMapOf<String, String>()
        var headerLine: String?
        var contentLength = 0
        while (true) {
            headerLine = reader.readLine()
            if (headerLine.isNullOrEmpty()) break
            val colonIdx = headerLine.indexOf(':')
            if (colonIdx > 0) {
                val name = headerLine.substring(0, colonIdx).trim()
                val value = headerLine.substring(colonIdx + 1).trim()
                headers[name] = value
                if (name.equals("Content-Length", ignoreCase = true)) {
                    contentLength = value.toIntOrNull() ?: 0
                }
            }
        }

        // 2. Read Request Body if present
        var requestBody = ""
        if (contentLength > 0) {
            val charBuffer = CharArray(contentLength)
            var totalRead = 0
            while (totalRead < contentLength) {
                val read = reader.read(charBuffer, totalRead, contentLength - totalRead)
                if (read <= 0) break
                totalRead += read
            }
            requestBody = String(charBuffer, 0, totalRead)
            addBytes(totalRead.toLong(), 0)
        }

        val queryParams = if (url.contains("?")) url.substringAfter("?") else ""
        val reqHeadersJson = JSONObject(headers as Map<*, *>).toString()

        var finalHeaders = headers.toMap()
        var finalBody = requestBody
        var wasModified = false
        var mockRuleApplied: String? = null

        // 3. Match active Rewrite Rules
        val activeRewriteRules = rulesRepository.getActiveRewriteRules()
        var matchedMockRule: RewriteRule? = null

        for (rule in activeRewriteRules) {
            val hostMatches = rule.targetHostPattern == "*" || host.contains(rule.targetHostPattern.replace("*", ""), ignoreCase = true)
            val pathMatches = rule.targetPathPattern == "*" || path.contains(rule.targetPathPattern.replace("*", ""), ignoreCase = true)
            val methodMatches = rule.targetMethod == "ALL" || rule.targetMethod.equals(method, ignoreCase = true)

            if (hostMatches && pathMatches && methodMatches) {
                when (rule.actionType) {
                    RuleActionType.MOCK_RESPONSE -> {
                        matchedMockRule = rule
                        mockRuleApplied = rule.name
                        wasModified = true
                        break
                    }
                    RuleActionType.REPLACE_HEADER -> {
                        if (!rule.targetHeaderKey.isNullOrBlank()) {
                            val updated = finalHeaders.toMutableMap()
                            updated[rule.targetHeaderKey] = rule.targetHeaderValue ?: ""
                            finalHeaders = updated
                            wasModified = true
                        }
                    }
                    RuleActionType.ADD_QUERY_PARAM -> {
                        // handled if needed
                        wasModified = true
                    }
                    RuleActionType.URL_REDIRECT -> {
                        wasModified = true
                    }
                }
            }
        }

        // 4. Match Breakpoint Rules
        var isBreakpointHit = false
        val activeBreakpoints = rulesRepository.getActiveBreakpointRules()
        for (bp in activeBreakpoints) {
            val methodMatches = bp.method == "ALL" || bp.method.equals(method, ignoreCase = true)
            val urlMatches = url.matches(Regex(bp.urlPattern.ifBlank { ".*" }))
            if (methodMatches && urlMatches && bp.pauseOnRequest) {
                isBreakpointHit = true
                val decision = BreakpointManager.pauseRequest(
                    method = method,
                    url = url,
                    host = host,
                    path = path,
                    headers = finalHeaders,
                    body = finalBody
                )

                when (decision) {
                    is BreakpointDecision.Forward -> {
                        finalHeaders = decision.modifiedHeaders
                        finalBody = decision.modifiedBody
                        wasModified = true
                    }
                    is BreakpointDecision.Mock -> {
                        sendMockResponse(
                            rawOut = rawOut,
                            status = decision.statusCode,
                            statusText = decision.statusText,
                            contentType = "application/json",
                            body = decision.body
                        )
                        saveTrafficLog(
                            isHttps = isHttps,
                            method = method,
                            url = url,
                            host = host,
                            path = path,
                            queryParams = queryParams,
                            reqHeadersJson = reqHeadersJson,
                            requestBody = finalBody,
                            status = decision.statusCode,
                            statusText = decision.statusText,
                            resHeadersJson = JSONObject(decision.headers as Map<*, *>).toString(),
                            responseBody = decision.body,
                            duration = System.currentTimeMillis() - startTime,
                            contentType = "application/json",
                            wasModified = true,
                            mockRuleApplied = "Manual Breakpoint Mock",
                            isBreakpointHit = true
                        )
                        return
                    }
                    is BreakpointDecision.Drop -> {
                        // Close connection immediately
                        return
                    }
                }
                break
            }
        }

        // 5. If Mock Rule matched, send mock response directly
        if (matchedMockRule != null) {
            val mockHeaders = mapOf(
                "Content-Type" to matchedMockRule.mockContentType,
                "X-Powered-By" to "API Flow Inspector Mock Engine",
                "Access-Control-Allow-Origin" to "*"
            )
            sendMockResponse(
                rawOut = rawOut,
                status = matchedMockRule.mockStatusCode,
                statusText = "OK",
                contentType = matchedMockRule.mockContentType,
                body = matchedMockRule.mockResponseBody
            )
            saveTrafficLog(
                isHttps = isHttps,
                method = method,
                url = url,
                host = host,
                path = path,
                queryParams = queryParams,
                reqHeadersJson = reqHeadersJson,
                requestBody = finalBody,
                status = matchedMockRule.mockStatusCode,
                statusText = "Mocked Response",
                resHeadersJson = JSONObject(mockHeaders as Map<*, *>).toString(),
                responseBody = matchedMockRule.mockResponseBody,
                duration = System.currentTimeMillis() - startTime,
                contentType = matchedMockRule.mockContentType,
                wasModified = true,
                mockRuleApplied = mockRuleApplied,
                isBreakpointHit = isBreakpointHit
            )
            return
        }

        // 6. Forward request to actual upstream server
        try {
            val okReqBuilder = Request.Builder().url(url)
            for ((k, v) in finalHeaders) {
                // Filter pseudo or proxy headers
                if (!k.startsWith("Proxy-", ignoreCase = true) &&
                    !k.equals("Host", ignoreCase = true) &&
                    !k.equals("Content-Length", ignoreCase = true)
                ) {
                    try { okReqBuilder.addHeader(k, v) } catch (_: Exception) {}
                }
            }

            val mediaType = finalHeaders["Content-Type"]?.toMediaTypeOrNull()
            val okBody = if (finalBody.isNotBlank() || method in listOf("POST", "PUT", "PATCH")) {
                finalBody.toByteArray().toRequestBody(mediaType)
            } else null

            when (method) {
                "GET" -> okReqBuilder.get()
                "POST" -> okReqBuilder.post(okBody ?: ByteArray(0).toRequestBody(null))
                "PUT" -> okReqBuilder.put(okBody ?: ByteArray(0).toRequestBody(null))
                "DELETE" -> if (okBody != null) okReqBuilder.delete(okBody) else okReqBuilder.delete()
                "PATCH" -> okReqBuilder.patch(okBody ?: ByteArray(0).toRequestBody(null))
                "HEAD" -> okReqBuilder.head()
                else -> okReqBuilder.method(method, okBody)
            }

            val okResponse = okHttpClient.newCall(okReqBuilder.build()).execute()
            val responseCode = okResponse.code
            val responseMessage = okResponse.message.ifBlank { "OK" }
            val respHeadersMap = mutableMapOf<String, String>()
            for (i in 0 until okResponse.headers.size) {
                respHeadersMap[okResponse.headers.name(i)] = okResponse.headers.value(i)
            }

            val rawRespBytes = okResponse.body?.bytes() ?: ByteArray(0)
            val respContentType = okResponse.header("Content-Type") ?: "text/plain"
            val respBodyStr = if (isTextOrJson(respContentType)) {
                String(rawRespBytes, Charsets.UTF_8)
            } else {
                "[Binary Content: ${rawRespBytes.size} bytes]"
            }

            // Write response back to client socket
            val statusLine = "HTTP/1.1 $responseCode $responseMessage\r\n"
            rawOut.write(statusLine.toByteArray(Charsets.ISO_8859_1))
            for ((k, v) in respHeadersMap) {
                rawOut.write("$k: $v\r\n".toByteArray(Charsets.ISO_8859_1))
            }
            rawOut.write("\r\n".toByteArray(Charsets.ISO_8859_1))
            rawOut.write(rawRespBytes)
            rawOut.flush()

            addBytes(finalBody.length.toLong(), rawRespBytes.size.toLong())

            saveTrafficLog(
                isHttps = isHttps,
                method = method,
                url = url,
                host = host,
                path = path,
                queryParams = queryParams,
                reqHeadersJson = reqHeadersJson,
                requestBody = finalBody,
                status = responseCode,
                statusText = responseMessage,
                resHeadersJson = JSONObject(respHeadersMap as Map<*, *>).toString(),
                responseBody = respBodyStr,
                duration = System.currentTimeMillis() - startTime,
                contentType = respContentType,
                wasModified = wasModified,
                mockRuleApplied = mockRuleApplied,
                isBreakpointHit = isBreakpointHit
            )

        } catch (e: Exception) {
            Log.e(tag, "Upstream request failed for $url", e)
            val errorMsg = "HTTP/1.1 502 Bad Gateway\r\nContent-Type: text/plain\r\n\r\nUpstream connection failed: ${e.message}"
            rawOut.write(errorMsg.toByteArray(Charsets.ISO_8859_1))
            rawOut.flush()

            saveTrafficLog(
                isHttps = isHttps,
                method = method,
                url = url,
                host = host,
                path = path,
                queryParams = queryParams,
                reqHeadersJson = reqHeadersJson,
                requestBody = finalBody,
                status = 502,
                statusText = "Bad Gateway",
                resHeadersJson = "{}",
                responseBody = "Proxy Upstream Error: ${e.message}",
                duration = System.currentTimeMillis() - startTime,
                contentType = "text/plain",
                wasModified = wasModified,
                mockRuleApplied = mockRuleApplied,
                isBreakpointHit = isBreakpointHit
            )
        }
    }

    private fun sendMockResponse(
        rawOut: OutputStream,
        status: Int,
        statusText: String,
        contentType: String,
        body: String
    ) {
        val bodyBytes = body.toByteArray(Charsets.UTF_8)
        val response = StringBuilder()
            .append("HTTP/1.1 ").append(status).append(" ").append(statusText).append("\r\n")
            .append("Content-Type: ").append(contentType).append("\r\n")
            .append("Content-Length: ").append(bodyBytes.size).append("\r\n")
            .append("Server: API-Flow-Inspector-MockEngine\r\n")
            .append("Connection: close\r\n\r\n")
            .toString()

        rawOut.write(response.toByteArray(Charsets.ISO_8859_1))
        rawOut.write(bodyBytes)
        rawOut.flush()
        addBytes(0, bodyBytes.size.toLong())
    }

    private fun recordSslHandshakeFailure(host: String, errorReason: String) {
        scope.launch(Dispatchers.IO) {
            val diagnosticGuide = """
                [TLS HANDSHAKE FAILED]
                The client application rejected the local Root CA certificate.
                Target host: $host
                Reason: $errorReason
                
                TROUBLESHOOTING & BYPASS FOR TEST APPS:
                1. Ensure "API Flow Inspector Root CA" is installed under Android Settings -> Security -> Encryption & Credentials -> Install a certificate -> CA certificate.
                2. In debug builds, add network_security_config.xml:
                   <network-security-config>
                       <debug-overrides>
                           <trust-anchors>
                               <certificates src="user" />
                           </trust-anchors>
                       </debug-overrides>
                   </network-security-config>
                3. For apps with OkHttp CertificatePinner or hardcoded pins, use the built-in Frida script from the CA & SSL tab.
            """.trimIndent()

            trafficRepository.insert(
                TrafficEntry(
                    method = "CONNECT",
                    url = "https://$host:443",
                    host = host,
                    path = "/",
                    responseStatus = -1,
                    responseStatusText = "SSL Handshake Failed",
                    responseBody = diagnosticGuide,
                    contentType = "text/plain",
                    isHttps = true,
                    sslError = errorReason
                )
            )
        }
    }

    private fun saveTrafficLog(
        isHttps: Boolean,
        method: String,
        url: String,
        host: String,
        path: String,
        queryParams: String,
        reqHeadersJson: String,
        requestBody: String,
        status: Int,
        statusText: String,
        resHeadersJson: String,
        responseBody: String,
        duration: Long,
        contentType: String,
        wasModified: Boolean,
        mockRuleApplied: String?,
        isBreakpointHit: Boolean
    ) {
        scope.launch(Dispatchers.IO) {
            trafficRepository.insert(
                TrafficEntry(
                    isHttps = isHttps,
                    method = method,
                    url = url,
                    host = host,
                    path = path,
                    queryParams = queryParams,
                    requestHeadersJson = reqHeadersJson,
                    requestBody = requestBody,
                    responseStatus = status,
                    responseStatusText = statusText,
                    responseHeadersJson = resHeadersJson,
                    responseBody = responseBody,
                    durationMs = duration,
                    requestSizeBytes = requestBody.length.toLong(),
                    responseSizeBytes = responseBody.length.toLong(),
                    contentType = contentType,
                    wasModified = wasModified,
                    mockRuleApplied = mockRuleApplied,
                    isBreakpointHit = isBreakpointHit
                )
            )
        }
    }

    private fun isTextOrJson(contentType: String): Boolean {
        val lower = contentType.lowercase()
        return lower.contains("json") ||
                lower.contains("text") ||
                lower.contains("xml") ||
                lower.contains("html") ||
                lower.contains("javascript") ||
                lower.contains("form-urlencoded")
    }
}
