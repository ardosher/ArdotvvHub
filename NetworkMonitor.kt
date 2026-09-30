package uz.ardo.tvhub

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.os.Build

/** Wi-Fi va internet holati. */
data class NetState(
    val wifiConnected: Boolean,
    /** 0..4 oralig'ida; noma'lum bo'lsa -1. */
    val wifiLevel: Int,
    val internet: Boolean
)

/**
 * NetworkCallback orqali tarmoq o'zgarishlarini kuzatadi.
 * Callback'lar asosiy oqimga o'tkazilmaydi; [listener] ni chaqiruvchi o'zi asosiy oqimga o'tkazishi kerak.
 */
class NetworkMonitor(context: Context, private val listener: (NetState) -> Unit) {

    private val app = context.applicationContext
    private val cm: ConnectivityManager? =
        app.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    private var registered = false
    private var last: NetState? = null

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = publish()
        override fun onLost(network: Network) = publish()
        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) = publish()
    }

    fun start() {
        val manager = cm ?: return
        if (registered) return
        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            manager.registerNetworkCallback(request, callback)
            registered = true
        } catch (e: Exception) {
            registered = false
        }
        publish()
    }

    fun stop() {
        val manager = cm ?: return
        if (!registered) return
        try {
            manager.unregisterNetworkCallback(callback)
        } catch (e: Exception) {
            // allaqachon olib tashlangan
        }
        registered = false
    }

    fun current(): NetState = compute()

    private fun publish() {
        val s = compute()
        if (s == last) return
        last = s
        listener(s)
    }

    @Suppress("DEPRECATION")
    private fun compute(): NetState {
        val manager = cm ?: return NetState(false, -1, false)
        var wifi = false
        var internet = false
        try {
            for (n in manager.allNetworks) {
                val caps = manager.getNetworkCapabilities(n) ?: continue
                if (!caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) continue
                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) wifi = true
                val validated = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                if (validated) internet = true
            }
        } catch (e: Exception) {
            // SecurityException va boshqalar: holat noma'lum sifatida qoladi
        }
        // Wi-Fi tarmog'i tekshiruvdan o'tmagan bo'lsa ham "ulangan", lekin internet yo'q bo'lishi mumkin.
        val level = if (wifi) wifiLevel() else -1
        return NetState(wifi, level, internet)
    }

    @Suppress("DEPRECATION")
    private fun wifiLevel(): Int = try {
        val wm = app.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val rssi = wm?.connectionInfo?.rssi ?: -127
        when {
            wm == null || rssi <= -127 -> -1
            Build.VERSION.SDK_INT >= 30 -> wm.calculateSignalLevel(rssi).coerceIn(0, 4)
            else -> WifiManager.calculateSignalLevel(rssi, 5).coerceIn(0, 4)
        }
    } catch (e: Exception) {
        -1
    }
}
