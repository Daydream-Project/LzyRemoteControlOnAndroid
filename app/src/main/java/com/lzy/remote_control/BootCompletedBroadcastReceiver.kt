package com.lzy.remote_control

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootCompletedBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent != null && intent.action == Intent.ACTION_BOOT_COMPLETED) {
            if (context != null) {
                try {
                    //Read service config in SharedPreferences.
                    val remoteServiceConfig = RemoteServiceConfig(context.applicationContext)

                    //If the enableService state in config is enabled, start service.
                    if (remoteServiceConfig.enableService) {
                        RemoteServiceConfig.startService(context.applicationContext, null)
                    }
                } catch (e: Exception) {
                    Log.d(RemoteServiceConfig.logTag, "Start Service failed! Exception is $e")
                }
            }
        }
    }
}
