package com.lzy.remote_control.network

import android.os.Handler
import android.os.Looper
import android.os.Message
import com.lzy.remote_control.protocol.BROADCAST_INFO_PORT

import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.util.Collections
import kotlin.RuntimeException

class NetworkMessageHandler(looper: Looper): Handler(looper) {
    private var broadcastSocket: DatagramSocket? = null
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
                try {
                    if (broadcastSocket != null) {
                        broadcastSocket!!.close()
                        broadcastSocket = null
                    }
                } finally {
                    looper.quit()
                }
            }
            INIT_UDP_SOCKET -> {
                if (msg.obj !is OperateUdpSocketParam)
                    return

                val param = msg.obj as OperateUdpSocketParam

                if (broadcastSocket != null) {
                    param.callback?.onMessageHandled(RuntimeException("udpSocket is not null."))
                    return
                }

                messageHandler(param.callback) {
                    broadcastSocket = DatagramSocket(InetSocketAddress(InetAddress.getByName("0.0.0.0"), BROADCAST_INFO_PORT))
                }
            }
            DESTROY_UDP_SOCKET -> {
                if (msg.obj !is OperateUdpSocketParam)
                    return

                val param = msg.obj as OperateUdpSocketParam

                if (broadcastSocket == null) {
                    param.callback?.onMessageHandled(RuntimeException("udpSocket is null."))
                    return
                }

                messageHandler(param.callback) {
                    broadcastSocket!!.close()
                    broadcastSocket = null
                }
            }
            BROADCAST_PACKET -> {
                if (msg.obj !is BroadcastPacketParam)
                    return

                val param  = msg.obj as BroadcastPacketParam

                if (broadcastSocket == null) {
                    param.callback?.onMessageHandled(RuntimeException("udpSocket is null"))
                    return
                }

                messageHandler(param.callback) {
                    //List all address info in all network interface.
                    //If a address info has a broadcast address, use this address to send message.

                    val interfaces: List<NetworkInterface> =
                        Collections.list(NetworkInterface.getNetworkInterfaces())
                    for (interfaceObj in interfaces) {
                        if (!interfaceObj.isLoopback) {
                            for (address in interfaceObj.interfaceAddresses) {
                                val broadcastAddress = address.broadcast
                                if (broadcastAddress != null) {

                                    param.packet.address = broadcastAddress
                                    param.packet.port = BROADCAST_INFO_PORT

                                    broadcastSocket!!.send(param.packet)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val EXIT_THREAD = 0
        const val INIT_UDP_SOCKET = 1
        const val DESTROY_UDP_SOCKET = 2
        const val BROADCAST_PACKET = 3
    }
}