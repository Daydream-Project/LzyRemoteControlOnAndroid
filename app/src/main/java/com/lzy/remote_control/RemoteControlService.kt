package com.lzy.remote_control

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.util.Log
import androidx.core.app.NotificationCompat
import com.lzy.remote_control.network.BroadcastPacketParam
import com.lzy.remote_control.network.IPType
import com.lzy.remote_control.network.OperateUdpSocketParam
import com.lzy.remote_control.network.NetworkMessageCallback
import com.lzy.remote_control.network.NetworkMessageHandler
import com.lzy.remote_control.network.NetworkThread
import com.lzy.remote_control.network.OperateSSLSocketParam
import com.lzy.remote_control.network.SSLClientConnectedCallback
import com.lzy.remote_control.network.getIPs
import com.lzy.remote_control.protocol.BroadcastRemoteControlServer
import com.lzy.remote_control.protocol.NetworkPacket
import java.net.DatagramPacket
import javax.net.ssl.SSLSocket


class RemoteControlService : Service() {
    inner class RemoteControlServiceBinder(private val service: RemoteControlService) : Binder() {
        //a wrapper to RemoteControlService.getServiceStatus
        fun getServiceStatus(): Int {
            return service.getServiceStatus()
        }
    }

    private var serviceStatus = SERVICE_STATUS_NOT_START
    private var thread: NetworkThread? = null
    private val binder = RemoteControlServiceBinder(this)

    companion object {
        private const val CHANNEL_ID = "LzyRemoteControl"
        private const val CHANNEL_NAME = "LzyRemoteControlService"
        private const val CHANNEL_DESCRIPTION = "Channel for LzyRemoteControl Service"
        private const val SERVICE_TITLE = "LzyRemoteControl Service"
        private const val SERVICE_TEXT = "This is a remote control service"

        //Service status values indicate service is running.
        const val SERVICE_STATUS_NOT_START = 0
        const val SERVICE_STATUS_RUNNING = 1
        const val SERVICE_STATUS_EXITED = 2
    }

    //Set service status, thread safe
    private fun setServiceStatus(_serviceStatus: Int) {
        synchronized(this) {
            serviceStatus = _serviceStatus
        }
    }

    //Get service safe, thread safe
    private fun getServiceStatus(): Int {
        val valCopy: Int
        synchronized(this) {
            valCopy = serviceStatus
        }
        return valCopy
    }

    override fun onCreate() {
        super.onCreate()
        startForeground(1, buildNotification(SERVICE_TEXT))
        initNetworkThread()
        setServiceStatus(SERVICE_STATUS_RUNNING)
    }

    private fun exitThread() {
        if (thread != null) {
            thread!!.terminate()
            thread = null
        }
    }

    private fun initNetworkThread() {
        val config = RemoteServiceConfig(applicationContext)

        thread = NetworkThread(applicationContext)

        //Start network thread.
        thread!!.start()

        var initSuccess = false

        //Create a init udpSocket callback that mark if success the initialization is
        val initUdpSocketCallback = object: NetworkThreadInitOperationTemplate() {
            fun initUdpSocket() {
                //Send init udp socket message and check the initialization is ok.
                val initUdpSocketMessage = Message()
                initUdpSocketMessage.what = NetworkMessageHandler.INIT_UDP_SOCKET
                initUdpSocketMessage.obj = OperateUdpSocketParam(this)

                thread!!.getHandler().sendMessage(initUdpSocketMessage)
            }
        }

        val initSSLServerSocket = object: NetworkThreadInitOperationTemplate() {
            fun initSSLServerSocket() {
                val initSSLServerSocketMessage = Message()
                initSSLServerSocketMessage.what = NetworkMessageHandler.INIT_SSL_SERVER
                initSSLServerSocketMessage.obj = OperateSSLSocketParam(config.port, this)

                thread!!.getHandler().sendMessage(initSSLServerSocketMessage)
            }
        }

        //Make objects receive message and handle it.
        val packetReceiver = PacketLoopReceiver(thread!!.getHandler(), PacketProcessor())

        //Make a object that broadcast ip and address when 5s passed.
        val broadcastIPPort = object: Runnable, NetworkMessageCallback {
            private val port = config.port
            private val handler = thread!!.getHandler()

            override fun run() {
                val ips = getIPs(IPType.ALL)
                val protocolPacket = NetworkPacket()
                val protocolPacketContent = BroadcastRemoteControlServer()

                protocolPacket.content = protocolPacketContent

                for (ip in ips) {
                    protocolPacketContent.ip = ip
                    protocolPacketContent.port = port

                    val bytes = protocolPacket.toUBytes()

                    val param = BroadcastPacketParam(DatagramPacket(bytes.map { it.toByte() }.toByteArray(), bytes.size), this)

                    val message = Message()
                    message.what = NetworkMessageHandler.BROADCAST_PACKET
                    message.obj = param

                    handler.sendMessage(message)
                }

                postThis()
            }

            override fun onMessageHandled(exception: Exception?) {
                exception?.let { Log.d("RemoteControlService", exception.toString()) }
            }

            fun postThis() {
                handler.postDelayed(this, 5000)
            }
        }

        //Make a object that loop accept ssl client
        val acceptSSLClient = object: SSLClientConnectedCallback {
            private val handler = thread!!.getHandler()

            fun sendAcceptSSlClientMessage() {
                val message = Message()
                message.what = NetworkMessageHandler.ACCEPT_SSL_CLIENT
                message.obj = this
                handler.sendMessageDelayed(message, 2)
            }

            override fun onSSLClientConnected(socket: SSLSocket?, exception: Exception?) {
                sendAcceptSSlClientMessage()

                if (socket != null) {
                    packetReceiver.postReceiveBytes()
                }
            }
        }

        do {

            //Init udp socket
            initUdpSocketCallback.initUdpSocket()

            //If initialization is failed, break
            //Else do rest things.
            if (!initUdpSocketCallback.waitForInitSuccess())
                break

            //Init ssl server socket.
            initSSLServerSocket.initSSLServerSocket()

            //Start accept ssl client.
            acceptSSLClient.sendAcceptSSlClientMessage()

            if (!initSSLServerSocket.waitForInitSuccess())
                break

            //Start broadcast
            broadcastIPPort.postThis()

            initSuccess = true
        } while (false)

        //If initialization is failed, stop service and exit.
        if (!initSuccess) {
            exitThread()
            exitService()
        }
    }

    private fun buildNotification(contentText: String): Notification {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            )

            serviceChannel.description = CHANNEL_DESCRIPTION

            notificationManager.createNotificationChannel(serviceChannel)
        }

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        // build notification.
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(SERVICE_TITLE)
            .setContentText(contentText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)

        builder.setCategory(Notification.CATEGORY_SERVICE)

        return builder.build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val config = RemoteServiceConfig(applicationContext)
        val notification = buildNotification("port = ${config.port}, password = ${config.password}")
        startForeground(1, notification)
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(1)
        exitThread()
        setServiceStatus(SERVICE_STATUS_EXITED)
    }

    private fun exitService() {
        //Method stopSelf do not run onDestroy
        //So write a method that stop service self and tun onDestroy method.
        stopSelf()
        onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        //Return binder
        return binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        return true
    }
}