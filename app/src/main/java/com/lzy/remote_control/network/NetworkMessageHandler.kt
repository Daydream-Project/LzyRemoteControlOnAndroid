package com.lzy.remote_control.network

import android.os.Handler
import android.os.Looper
import android.os.Message

import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import kotlin.RuntimeException

class NetworkMessageHandler(looper: Looper): Handler(looper) {
    private var udpSocket: DatagramSocket? = null
    override fun handleMessage(msg: Message) {
        super.handleMessage(msg)

        //Message handle template, run message handle function and catch exception if has, last run callback with exception.
        val messageHandler: (NetworkMessageCallback?, Runnable) -> Unit = { callback, function ->
            var exception: Exception? = null
            try {
                function.run()
            } catch (exception2: Exception) {
                exception = exception2
            }
            callback?.onBroadcastCompleted(exception)
        }

        when (msg.what) {
            EXIT_THREAD -> {
                looper.quit()
            }
            INIT_UDP_SOCKET -> {
                if (msg.obj !is InitUdpSocketParam)
                    return

                val param = msg.obj as InitUdpSocketParam

                if (udpSocket != null) {
                    param.callback?.onBroadcastCompleted(RuntimeException("udpSocket is not null."))
                    return
                }

                messageHandler(param.callback) {
                    udpSocket = DatagramSocket()
                    udpSocket!!.bind(InetSocketAddress(InetAddress.getByName(param.ipAddress), param.port))
                }
            }
            BROADCAST_PACKET -> {
                if (msg.obj !is BroadcastPacketParam)
                    return

                val param  = msg.obj as BroadcastPacketParam

                if (udpSocket == null) {
                    param.callback?.onBroadcastCompleted(RuntimeException("udpSocket is null"))
                    return
                }

                messageHandler(param.callback) {
                    udpSocket!!.broadcast = true
                    udpSocket!!.send(param.packet)
                    udpSocket!!.broadcast = false
                }
            }
        }
    }

    companion object {
        const val EXIT_THREAD = 0
        const val INIT_UDP_SOCKET = 1
        const val BROADCAST_PACKET = 2
    }
}