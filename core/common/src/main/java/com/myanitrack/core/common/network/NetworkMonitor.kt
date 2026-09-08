package com.myanitrack.core.common.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Cihazin internete erisimi olup olmadigini yayinlar.
 *
 * "Baglanti var" degil "internete cikabiliyor" olcuyoruz: [NetworkCapabilities.NET_CAPABILITY_VALIDATED]
 * sayesinde captive portal-a takilmis Wi-Fi baglantilari cevrimdisi sayilir.
 */
@Singleton
class NetworkMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    val isOnline: Flow<Boolean> = callbackFlow {
        val connectivityManager = context.getSystemService<ConnectivityManager>()
        if (connectivityManager == null) {
            // Servis alinamiyorsa cevrimici varsayiyoruz; aksi halde uygulama
            // gereksiz yere cevrimdisi uyarisi gosterirdi.
            trySend(true)
            channel.close()
            return@callbackFlow
        }

        val callback = object : ConnectivityManager.NetworkCallback() {
            private val onlineNetworks = mutableSetOf<Network>()

            override fun onCapabilitiesChanged(
                network: Network,
                capabilities: NetworkCapabilities,
            ) {
                val validated =
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                if (validated) onlineNetworks += network else onlineNetworks -= network
                trySend(onlineNetworks.isNotEmpty())
            }

            override fun onLost(network: Network) {
                onlineNetworks -= network
                trySend(onlineNetworks.isNotEmpty())
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, callback)

        trySend(connectivityManager.isCurrentlyOnline())

        awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
    }
        .conflate()
        .distinctUntilChanged()

    private fun ConnectivityManager.isCurrentlyOnline(): Boolean =
        activeNetwork
            ?.let(::getNetworkCapabilities)
            ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
}
