package com.lzy.remote_control

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootCompletedBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent != null && intent.action == Intent.ACTION_BOOT_COMPLETED) {
            if (context != null) {
                val remoteServiceConfig = RemoteServiceConfig(context.applicationContext)

                if (remoteServiceConfig.enableService)
                    RemoteServiceConfig.startService(context.applicationContext as Application)
            }
        }
    }
}
