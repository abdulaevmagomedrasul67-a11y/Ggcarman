package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import com.example.core.database.AppDatabase
import com.example.core.proxy.HttpProxyServer
import com.example.core.repository.RulesRepository
import com.example.core.repository.TrafficRepository
import com.example.core.security.CertificateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.InetSocketAddress
import java.net.Proxy

class ApiFlowApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var certificateManager: CertificateManager
        private set

    lateinit var trafficRepository: TrafficRepository
        private set

    lateinit var rulesRepository: RulesRepository
        private set

    lateinit var proxyServer: HttpProxyServer
        private set

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        instance = this

        createNotificationChannels()

        database = AppDatabase.getInstance(this)
        certificateManager = CertificateManager(this)
        trafficRepository = TrafficRepository(database.trafficDao())
        rulesRepository = RulesRepository(database.rulesDao())
        proxyServer = HttpProxyServer(
            trafficRepository = trafficRepository,
            rulesRepository = rulesRepository,
            certificateManager = certificateManager,
            scope = applicationScope
        )

        // Automatically start the proxy server on port 8888
        proxyServer.start(8888)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID_PROXY,
                "API Flow Proxy Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live interception proxy status and network activity"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun triggerTestSimulatedRequest(method: String, url: String, body: String? = null) {
        applicationScope.launch(Dispatchers.IO) {
            try {
                val proxy = Proxy(Proxy.Type.HTTP, InetSocketAddress("127.0.0.1", proxyServer.stats.value.port))
                val client = OkHttpClient.Builder()
                    .proxy(proxy)
                    .build()

                val reqBuilder = Request.Builder().url(url)
                if (method == "POST" && body != null) {
                    val mediaType = "application/json".toMediaTypeOrNull()
                    reqBuilder.post(body.toRequestBody(mediaType))
                } else {
                    reqBuilder.get()
                }

                client.newCall(reqBuilder.build()).execute().use { resp ->
                    Log.d("ApiFlowApp", "Simulated request to $url returned code: ${resp.code}")
                }
            } catch (e: Exception) {
                Log.d("ApiFlowApp", "Simulated request exception: ${e.message}")
            }
        }
    }

    companion object {
        const val CHANNEL_ID_PROXY = "api_flow_proxy_channel"
        lateinit var instance: ApiFlowApp
            private set
    }
}
