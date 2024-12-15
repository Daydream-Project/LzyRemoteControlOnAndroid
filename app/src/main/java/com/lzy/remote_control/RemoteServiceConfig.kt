package com.lzy.remote_control

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import java.security.InvalidParameterException

class RemoteServiceConfig(context: Context) {

    private var sharedPreferences = context.getSharedPreferences(
        configName,
        AppCompatActivity.MODE_PRIVATE
    )

    companion object {

        private const val configName = "LzyRemoteControlService"

        private const val passwordKey = "passwordHash"

        private const val portKey = "port"

        private const val enableServiceKey = "enableService"

        const val logTag = "LzyRemoteControl"

        fun startService(context: Context, serviceConnection: ServiceConnection?) {
            val serviceIntent = Intent(context, RemoteControlService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
            if (serviceConnection != null) {
                serviceIntent.action = "com.lzy.remote_control.BIND_SERVICE"
                context.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
            }
        }

        fun stopService(context: ContextWrapper, serviceConnection: ServiceConnection?) {
            val serviceIntent = Intent(context, RemoteControlService::class.java)
            try {
                if (serviceConnection != null)
                    context.unbindService(serviceConnection)
            } catch (_: Exception) {

            }
            context.stopService(serviceIntent)
        }
    }

    private fun isValidPort(portNum: Int): Boolean {
        return portNum in 1..65535
    }

    fun updateConnectionConfig(password: String,portNum: Int)
    {
        if (!isValidPort(portNum))
            throw InvalidParameterException("portNum")

        val editor = sharedPreferences.edit()
        editor.putString(passwordKey, password)
        editor.putInt(portKey, portNum)
        editor.apply()
    }

    var enableService : Boolean
        get() {
            return sharedPreferences.getBoolean(enableServiceKey,false)
        }
        set(value) {
            val editor = sharedPreferences.edit()
            editor.putBoolean(enableServiceKey, value)
            editor.apply()
        }

    var password: String?
        get() {
            return sharedPreferences.getString(passwordKey, null)
        }
        set(value) {
            val editor = sharedPreferences.edit()
            editor.putString(passwordKey, value)
            editor.apply()
        }

    var port: Int
        get() {
            return sharedPreferences.getInt(portKey, 0)
        }
        set(value) {
            if (!isValidPort(value))
                throw InvalidParameterException("value")

            val editor = sharedPreferences.edit()
            editor.putInt(portKey, value)
            editor.apply()
        }
}