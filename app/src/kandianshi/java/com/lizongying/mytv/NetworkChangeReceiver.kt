package com.lizongying.mytv

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager

class NetworkChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
            val isConnected = context?.isNetworkConnected
            if (isConnected == true) {
            }
        }
    }
}