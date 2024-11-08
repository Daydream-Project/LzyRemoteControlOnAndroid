package com.lzy.remote_control

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Build
import android.util.Base64
import androidx.appcompat.app.AppCompatActivity
import java.security.InvalidParameterException
import java.security.MessageDigest

class RemoteServiceConfig(context: Context) {

    private var sharedPreferences = context.getSharedPreferences(
        configName,
        AppCompatActivity.MODE_PRIVATE
    )

    companion object {

        private const val configName = "LzyRemoteControlService";

        private const val passwordKey = "passwordHash"

        private const val portKey = "port"

        private const val enableServiceKey = "enableService"

        fun hashPassword(password: String): String {
            val crypto = MessageDigest.getInstance("SHA-256")
            val passwordBytes = password.encodeToByteArray()
            val resultBytes = crypto.digest(passwordBytes)
            return Base64.encodeToString(resultBytes, Base64.DEFAULT)
        }

        fun startService(context: Context) {
            val serviceIntent = Intent(context, RemoteControlService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            }
            context.startService(serviceIntent)
        }

        fun stopService(context: ContextWrapper) {
            val serviceIntent = Intent(context, RemoteControlService::class.java)
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
        get()
        {
            return sharedPreferences.getInt(portKey, 0)
        }
        set(value)
        {
            if (!isValidPort(value))
                throw InvalidParameterException("value")

            val editor = sharedPreferences.edit()
            editor.putInt(portKey, value)
            editor.apply()
        }
}