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
            callback?.onMessageHandled(exception)
        }

        when (msg.what) {
            EXIT_THREAD -> {
                if (udpSocket != null) {
                    udpSocket!!.close()
                    udpSocket = null
                }
                looper.quit()
            }
            INIT_UDP_SOCKET -> {
                if (msg.obj !is InitUdpSocketParam)
                    return

                val param = msg.obj as InitUdpSocketParam

                if (udpSocket != null) {
                    param.callback?.onMessageHandled(RuntimeException("udpSocket is not null."))
                    return
                }

                messageHandler(param.callback) {
                    udpSocket = DatagramSocket(InetSocketAddress(InetAddress.getByName(param.ipAddress), param.port))
                }
            }
            BROADCAST_PACKET -> {
                if (msg.obj !is BroadcastPacketParam)
                    return

                val param  = msg.obj as BroadcastPacketParam

                if (udpSocket == null) {
                    param.callback?.onMessageHandled(RuntimeException("udpSocket is null"))
                    return
                }

                messageHandler(param.callback) {
                    param.packet.address = InetAddress.getByName("255.255.255.255")
                    udpSocket!!.send(param.packet)
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