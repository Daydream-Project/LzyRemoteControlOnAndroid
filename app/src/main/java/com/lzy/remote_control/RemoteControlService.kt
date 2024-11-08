package com.lzy.remote_control

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class RemoteControlService : Service() {

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
    }

    private fun buildNotification(contentText: String): Notification {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager;

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

        // 构建通知
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(SERVICE_TITLE)
            .setContentText(contentText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)

        builder.setCategory(Notification.CATEGORY_SERVICE)

        return builder.build();
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val config = RemoteServiceConfig(applicationContext)
        val notification = buildNotification("port = ${config.port}, password = ${config.password}")
        startForeground(1, notification)
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager;
        notificationManager.cancel(1)
    }

    override fun onBind(intent: Intent?): IBinder? {
        TODO("Not yet implemented")
    }
}