package com.example.m2_2.Data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

class ConnectionStateReceiver(private val onConnectionChanged: (Boolean) -> Unit) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val isConnected = isConnectedToInternet(context)
        onConnectionChanged(isConnected)
    }

    companion object {
        fun isConnectedToInternet(context: Context): Boolean {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivityManager.activeNetwork ?: return false
            val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            return networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }

        fun register(context: Context, onConnectionChanged: (Boolean) -> Unit): ConnectionStateReceiver {
            val receiver = ConnectionStateReceiver(onConnectionChanged)
            val intentFilter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
            context.registerReceiver(receiver, intentFilter)
            return receiver
        }

        fun unregister(context: Context, receiver: ConnectionStateReceiver) {
            context.unregisterReceiver(receiver)
        }
    }
}