package com.example.core.performance

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ConnectionQuality(
    val labelAr: String,
    val refreshIntervalMs: Long,
    val indicatorColorHex: Long
) {
    ULTRA_FAST("اتصال فائق (Wi-Fi 6G / 5G)", 1500L, 0xFF4FE3C1),
    GOOD("اتصال مستقر (4G / LTE)", 3000L, 0xFF5BC8FF),
    MODERATE("شبكة متوسطة (3G)", 6000L, 0xFFFFB74D),
    POOR("اتصال ضعيف (توفير بيانات)", 12000L, 0xFFFF8A65),
    OFFLINE("بدون اتصال (وضع محلي)", 30000L, 0xFFFF5252)
}

/**
 * Intelligent Connection Quality Detector
 * Automatically adapts WebSocket & polling ticker rates based on real-time link speed.
 */
class ConnectionQualityMonitor(context: Context) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private val _quality = MutableStateFlow(ConnectionQuality.ULTRA_FAST)
    val quality: StateFlow<ConnectionQuality> = _quality.asStateFlow()

    init {
        registerNetworkCallback()
    }

    private fun registerNetworkCallback() {
        if (connectivityManager == null) return

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(
            request,
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    evaluateNetwork(network)
                }

                override fun onCapabilitiesChanged(
                    network: Network,
                    capabilities: NetworkCapabilities
                ) {
                    evaluateCapabilities(capabilities)
                }

                override fun onLost(network: Network) {
                    _quality.value = ConnectionQuality.OFFLINE
                }
            }
        )
    }

    private fun evaluateNetwork(network: Network) {
        val caps = connectivityManager?.getNetworkCapabilities(network) ?: return
        evaluateCapabilities(caps)
    }

    private fun evaluateCapabilities(caps: NetworkCapabilities) {
        val downlinkKbps = caps.linkDownstreamBandwidthKbps
        val isWifi = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)

        _quality.value = when {
            isWifi || downlinkKbps > 20000 -> ConnectionQuality.ULTRA_FAST
            downlinkKbps > 5000 -> ConnectionQuality.GOOD
            downlinkKbps > 1000 -> ConnectionQuality.MODERATE
            downlinkKbps > 0 -> ConnectionQuality.POOR
            else -> ConnectionQuality.GOOD
        }
    }
}
