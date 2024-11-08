package com.lzy.remote_control

import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.ToggleButton
import android.widget.Toast;
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.security.InvalidParameterException

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val remoteServiceConfig = RemoteServiceConfig(applicationContext)

        val enableServiceButton = findViewById<ToggleButton>(R.id.enableServiceBtn)
        val passwordBox = findViewById<EditText>(R.id.passwordBox)
        val portBox = findViewById<EditText>(R.id.portBox)
        val showPassword = findViewById<ToggleButton>(R.id.showPassword)

        passwordBox.setText(remoteServiceConfig.password)
        portBox.setText(remoteServiceConfig.port.toString())

        if (remoteServiceConfig.enableService)
        {
             try {
                 RemoteServiceConfig.startService(application)
                 enableServiceButton.isChecked = true
                 passwordBox.isEnabled = false
                 portBox.isEnabled = false
             } catch (_:Exception) {
                 enableServiceButton.isChecked = false
             }
        }

        enableServiceButton.setOnClickListener {
            view ->

            val btn = view as ToggleButton

            if (btn.isChecked)
            {
                var ok = false

                do {
                    var port = 0

                    try {

                        port = portBox.text.toString().toInt()

                        if (port <= 0 || port > 65535)
                            throw InvalidParameterException()

                    } catch (_ : Exception) {
                        Toast.makeText(this,"Invalid port value", Toast.LENGTH_LONG).show()
                        break
                    }

                    val password = passwordBox.text.toString()

                    remoteServiceConfig.updateConnectionConfig(password, port)

                    try {
                        RemoteServiceConfig.stopService(application)
                        RemoteServiceConfig.startService(application)
                    } catch (_ : Exception) {
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


                try {
                    RemoteServiceConfig.stopService(application)
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

            if (btn.isChecked)
                passwordBox.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            else
                passwordBox.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
    }
}



