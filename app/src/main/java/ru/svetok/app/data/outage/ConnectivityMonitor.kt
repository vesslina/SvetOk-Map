package ru.svetok.app.data.outage

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

object ConnectivityMonitor {

    private const val TAG = "ConnectivityMonitor"

    fun observe(context: Context): Flow<Boolean> = callbackFlow {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        trySend(cm.activeNetwork != null)

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { trySend(true) }
            override fun onLost(network: Network) {
                trySend(cm.activeNetwork != null)
            }
        }

        val request = NetworkRequest.Builder().build()
        try {
            cm.registerNetworkCallback(request, callback)
        } catch (e: Exception) {
            Log.w(TAG, "Network callback registration failed", e)
        }

        awaitClose {
            try { cm.unregisterNetworkCallback(callback) } catch (e: Exception) {
                Log.w(TAG, "Network callback unregistration failed", e)
            }
        }
    }.distinctUntilChanged()
}
