package com.example.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.ApiFlowApp
import com.example.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProxyVpnService : VpnService() {

    private val tag = "ProxyVpnService"
    private var vpnInterface: ParcelFileDescriptor? = null
    private var statsJob: Job? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP) {
            stopVpn()
            return START_NOT_STICKY
        }

        val targetPackage = intent?.getStringExtra(EXTRA_TARGET_PACKAGE)
        startVpn(targetPackage)
        return START_STICKY
    }

    private fun startVpn(targetPackage: String?) {
        try {
            val notification = buildNotification("Interceptor active on port 8888")
            startForeground(NOTIFICATION_ID, notification)

            val builder = Builder()
                .setSession("API Flow Inspector")
                .addAddress("10.0.0.2", 32)
                .addDnsServer("8.8.8.8")
                .addRoute("0.0.0.0", 0)

            if (!targetPackage.isNullOrBlank()) {
                try {
                    builder.addAllowedApplication(targetPackage)
                    Log.i(tag, "VPN filtered strictly to target package: $targetPackage")
                } catch (e: Exception) {
                    Log.w(tag, "Could not restrict to package $targetPackage: ${e.message}")
                }
            }

            // Exclude our own app from looping into VPN
            try {
                builder.addDisallowedApplication(packageName)
            } catch (_: Exception) {}

            vpnInterface = builder.establish()
            _isVpnRunning.value = true
            _filteredPackage.value = targetPackage

            // Start proxy server if not already running
            ApiFlowApp.instance.proxyServer.start(8888)

            observeStats()
            Log.i(tag, "API Flow VPN Service started successfully")
        } catch (e: Exception) {
            Log.e(tag, "Failed to establish VPN interface", e)
            stopVpn()
        }
    }

    private fun observeStats() {
        statsJob?.cancel()
        statsJob = CoroutineScope(Dispatchers.Main).launch {
            ApiFlowApp.instance.proxyServer.stats.collect { stats ->
                if (_isVpnRunning.value) {
                    val text = "Port ${stats.port} | ${stats.totalRequests} reqs | Active conns: ${stats.activeConnections}"
                    val notification = buildNotification(text)
                    val manager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
                    manager.notify(NOTIFICATION_ID, notification)
                }
            }
        }
    }

    private fun stopVpn() {
        statsJob?.cancel()
        try {
            vpnInterface?.close()
        } catch (e: Exception) {
            Log.e(tag, "Error closing VPN interface", e)
        }
        vpnInterface = null
        _isVpnRunning.value = false
        _filteredPackage.value = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }

    private fun buildNotification(contentText: String): Notification {
        val launchIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        val stopIntent = Intent(this, ProxyVpnService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        return NotificationCompat.Builder(this, ApiFlowApp.CHANNEL_ID_PROXY)
            .setContentTitle("API Flow Inspector")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
            .build()
    }

    companion object {
        const val ACTION_STOP = "com.example.service.STOP_VPN"
        const val EXTRA_TARGET_PACKAGE = "EXTRA_TARGET_PACKAGE"
        const val NOTIFICATION_ID = 1001

        private val _isVpnRunning = MutableStateFlow(false)
        val isVpnRunning: StateFlow<Boolean> = _isVpnRunning.asStateFlow()

        private val _filteredPackage = MutableStateFlow<String?>(null)
        val filteredPackage: StateFlow<String?> = _filteredPackage.asStateFlow()
    }
}
