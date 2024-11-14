package com.lzy.remote_control

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.Message
import android.util.Log
import androidx.core.app.NotificationCompat
import com.lzy.remote_control.network.BroadcastPacketParam
import com.lzy.remote_control.network.IPType
import com.lzy.remote_control.network.OperateUdpSocketParam
import com.lzy.remote_control.network.NetworkMessageCallback
import com.lzy.remote_control.network.NetworkMessageHandler
import com.lzy.remote_control.network.NetworkThread
import com.lzy.remote_control.network.getIPs
import com.lzy.remote_control.protocol.BroadcastRemoteControlServer
import com.lzy.remote_control.protocol.NetworkPackage
import java.net.DatagramPacket


class RemoteControlService : Service() {
    private var thread: NetworkThread? = null

    companion object {
        private const val CHANNEL_ID = "LzyRemoteControl"
        private const val CHANNEL_NAME = "LzyRemoteControlService"
        private const val CHANNEL_DESCRIPTION = "Channel for LzyRemoteControl Service"
        private const val SERVICE_TITLE = "LzyRemoteControl Service"
        private const val SERVICE_TEXT = "This is a remote control service"
    }

    override fun onCreate() {
        super.onCreate()
        startForeground(1, buildNotification(SERVICE_TEXT))
        initNetworkThread()
    }

    private fun exitThread() {
        if (thread != null) {
            val exitThreadMessage = Message()
            exitThreadMessage.what = NetworkMessageHandler.EXIT_THREAD
            thread!!.getHandler().sendMessage(exitThreadMessage)
            thread = null
        }
    }

    private fun initNetworkThread() {
        val config = RemoteServiceConfig(applicationContext)

        thread = NetworkThread()

        //Start network thread.
        thread!!.start()

        //Create a init udpSocket callback that mark if success the initialization is
        val initUdpSocketCallback = object : NetworkMessageCallback {
            var initUdpSocketStatus = 0

            override fun onMessageHandled(exception: Exception?) {
                synchronized(this) {
                    initUdpSocketStatus = if (exception == null) 1 else 2
                }
            }

        }

        //Send init udp socket message and check the initialization is ok.
        val initUdpSocketMessage = Message()
        initUdpSocketMessage.what = NetworkMessageHandler.INIT_UDP_SOCKET
        initUdpSocketMessage.obj = OperateUdpSocketParam(initUdpSocketCallback)

        thread!!.getHandler().sendMessage(initUdpSocketMessage)

        //Wait for the result of initialization of udpSocket.
        while(true) {
            var initUdpSocketStatusCopy: Int

            synchronized(initUdpSocketCallback) {
                initUdpSocketStatusCopy = initUdpSocketCallback.initUdpSocketStatus
            }

            //If initialization is failed, stop service and exit.
            if (initUdpSocketStatusCopy == 2) {
                exitThread()
                stopSelf()
                return
            }

            //If initialization is ok, do rest things.
            else if (initUdpSocketStatusCopy == 1) {
                break
            }
        }

        //Make a object that broadcast ip and address when 5s passed.
        val broadcastIPPort = object : Runnable {
            private val port = config.port
            private val handler = thread!!.getHandler()

            override fun run() {
                val ips = getIPs(IPType.ALL)
                val protocolPacket = NetworkPackage()
                val protocolPacketContent = BroadcastRemoteControlServer()
                val tempArray = Array<Byte>(0) { _ -> 0 }

                protocolPacket.content = protocolPacketContent

                for (ip in ips) {
                    protocolPacketContent.ip = ip
                    protocolPacketContent.port = port

                    val bytes = protocolPacket.toUBytes()
                    val packet = DatagramPacket(bytes.map { it.toByte() }.toByteArray(), bytes.size)

                    val param = BroadcastPacketParam(DatagramPacket(tempArray.toByteArray(), tempArray.size), object : NetworkMessageCallback {
                        override fun onMessageHandled(exception: Exception?) {
                            exception?.let { Log.d("RemoteControlService", exception.toString()) }
                        }
                    })

                    param.packet = packet
                    param.callback = object : NetworkMessageCallback {
                        override fun onMessageHandled(exception: Exception?) {
                            exception?.let { Log.d("RemoteControlService", exception.toString()) }
                            postThis()
                        }
                    }

                    val message = Message()
                    message.what = NetworkMessageHandler.BROADCAST_PACKET
                    message.obj = param

                    handler.sendMessage(message)
                }
            }

            fun postThis() {
                handler.postDelayed(this, 10000)
            }
        }

        //Start broadcast
        broadcastIPPort.postThis()
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
    }

    override fun onBind(intent: Intent?): IBinder? {
        TODO("Not yet implemented")
    }
}