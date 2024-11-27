package com.lzy.remote_control

import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.text.InputType
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.security.InvalidParameterException
import com.lzy.remote_control.permission.PermissionUtils

class MainActivity : AppCompatActivity(), ServiceConnection {

    private var requestPermissionsResultHandler: ((Int, Array<out String>, IntArray) -> Unit)? = null
    private var binder: RemoteControlService.RemoteControlServiceBinder? = null

    //If the handler of RequestPermissionsResult registered, forward message to it.
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        requestPermissionsResultHandler?.invoke(requestCode, permissions, grantResults)
    }

    override fun onDestroy() {
        super.onDestroy()
        //Unbind service.
        unbindService(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        //Load layout file.
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        //Get permissions not allowed registered in AndroidManifest.xml
        val notAllowedPermissions = PermissionUtils.getNotAllowedPermissions(this)

        //Register the handler
        requestPermissionsResultHandler = { code, _, results ->
            if (results.isEmpty() || results[0] != PackageManager.PERMISSION_GRANTED)
                Toast.makeText(this,"Permission ${notAllowedPermissions[code]} is not granted", Toast.LENGTH_SHORT).show()
        }

        //Request them and the request code is index of permissions.
        PermissionUtils.requestPermissions(notAllowedPermissions, this)

        //Open service config.
        val remoteServiceConfig = RemoteServiceConfig(applicationContext)

        //Get controls in layout.
        val enableServiceButton = findViewById<ToggleButton>(R.id.enableServiceBtn)
        val passwordBox = findViewById<EditText>(R.id.passwordBox)
        val portBox = findViewById<EditText>(R.id.portBox)
        val showPassword = findViewById<ToggleButton>(R.id.showPassword)

        //Load config to controls.
        passwordBox.setText(remoteServiceConfig.password)
        portBox.setText(remoteServiceConfig.port.toString())

        if (remoteServiceConfig.enableService) {
             try {
                 RemoteServiceConfig.startService(application, this)
                 enableServiceButton.isChecked = true
                 passwordBox.isEnabled = false
                 portBox.isEnabled = false
             } catch (_:Exception) {
                 enableServiceButton.isChecked = false
             }
        }

        //Set click event handler.
        enableServiceButton.setOnClickListener {
            view ->

            val btn = view as ToggleButton

            if (btn.isChecked)
            {
                //If control is checked
                //Write config and start service.
                var ok = false

                do {
                    var port: Int

                    try {

                        port = portBox.text.toString().toInt()

                        if (port <= 0 || port > 65535)
                            throw InvalidParameterException()

                    } catch (_ : Exception) {
                        Toast.makeText(this,"Invalid port value", Toast.LENGTH_LONG).show()
                        break
                    }

                    val password = passwordBox.text.toString()

                    if (password.length < 8) {
                        Toast.makeText(this,"Password must has chars more than eight.", Toast.LENGTH_LONG).show()
                        break
                    }

                    remoteServiceConfig.updateConnectionConfig(password, port)

                    try {
                        RemoteServiceConfig.stopService(application, this)
                        RemoteServiceConfig.startService(application, this)
                    } catch (exception: Exception) {
                        Toast.makeText(this, "Can not start service", Toast.LENGTH_LONG).show()
                        break
                    }

                    ok = true
                } while (false)

                if (!ok) {
                    btn.isChecked = false
                    remoteServiceConfig.enableService = false
                }
                else {
                    passwordBox.isEnabled = false
                    portBox.isEnabled = false
                    remoteServiceConfig.enableService = true
                }
            }
            else
            {
                //If control is not checked.
                //Stop Service.

                try {
                    RemoteServiceConfig.stopService(application, this)
                } catch (_ : Exception) {
                    Toast.makeText(this,"Can not stop service",Toast.LENGTH_LONG).show()
                }

                remoteServiceConfig.enableService = false

                passwordBox.isEnabled = true
                portBox.isEnabled = true
            }
        }

        showPassword.isChecked = false

        showPassword.setOnClickListener {
            view ->

            val btn = view as ToggleButton

            //If control is checked show password else hide password.

            if (btn.isChecked)
                passwordBox.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            else
                passwordBox.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        binder = service as RemoteControlService.RemoteControlServiceBinder
        val enableServiceButton = findViewById<ToggleButton>(R.id.enableServiceBtn)
        val activityCopy = this
        val binderCopy = binder

        //When service is bind, loop run a object checks service is running.

        val serviceRunningCheck = object: Runnable {
            private val handler = Handler(Looper.getMainLooper())
            override fun run() {
                val serviceStatus = binderCopy!!.getServiceStatus()
                if (serviceStatus != RemoteControlService.SERVICE_STATUS_EXITED) {
                    postThis()
                } else {
                    enableServiceButton.isChecked = false
                    try {
                        unbindService(activityCopy)
                    } catch (_: Exception) {

                    }
                    Toast.makeText(activityCopy, "Service stopped.", Toast.LENGTH_LONG).show()
                }
            }

            fun postThis() {
                handler.post(this)
            }
        }

        serviceRunningCheck.postThis()
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        //On unbind, set the binder to null.
        binder = null
    }
}
