package com.lzy.remote_control.network

import android.os.Handler
import android.os.Looper
import android.os.Message
import com.lzy.remote_control.protocol.BROADCAST_INFO_PORT

import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.SocketTimeoutException
import java.util.Collections
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLServerSocket
import javax.net.ssl.SSLSocket
import kotlin.RuntimeException

class NetworkMessageHandler(looper: Looper): Handler(looper) {
    private var broadcastSocket: DatagramSocket? = null
    private var sslServerSocket: SSLServerSocket? = null
    private var sslClientSocket: SSLSocket? = null
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
                    if (sslServerSocket != null) {
                        sslServerSocket!!.close()
                        sslServerSocket = null
                    }
                    if (sslClientSocket != null) {
                        sslClientSocket!!.close()
                        sslClientSocket = null
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
            INIT_SSL_SERVER -> {
                if (msg.obj !is OperateSSLSocketParam)
                    return

                val param = msg.obj as OperateSSLSocketParam

                if (sslServerSocket != null) {
                    param.callback?.onMessageHandled(RuntimeException("sslServerSocket is not null"))
                    return
                }

                messageHandler(param.callback) {
                    val sslContext = SSLContext.getInstance("TLS")
                    sslServerSocket = sslContext.serverSocketFactory.createServerSocket(param.port) as SSLServerSocket
                    sslServerSocket!!.soTimeout = 0
                }
            }
            DESTROY_SSL_SERVER -> {
                if (msg.obj !is OperateSSLSocketParam)
                    return

                val param = msg.obj as OperateSSLSocketParam

                if (sslServerSocket == null) {
                    param.callback?.onMessageHandled(RuntimeException("sslServerSocket is null"))
                    return
                }

                messageHandler(param.callback) {
                    sslClientSocket!!.close()
                    sslClientSocket = null
                }
            }
            SEND_SSL_BYTES -> {
                if (msg.obj !is SendBytesParam)
                    return

                val param = msg.obj as SendBytesParam

                if (sslClientSocket == null) {
                    param.callback?.onMessageHandled(RuntimeException("sslServerSocket is null"))
                    return
                }

                messageHandler(param.callback) {
                    sslClientSocket!!.outputStream.write(param.bytes)
                }
            }
            RECEIVE_SSL_BYTES -> {
                if (msg.obj !is ReceiveBytesParam)
                    return

                val param = msg.obj as ReceiveBytesParam

                if (sslClientSocket == null) {
                    param.callback?.onBytesReceived(null,0,RuntimeException("sslServerSocket is null"))
                    return
                }

                try {
                    val readBytes = sslClientSocket!!.inputStream.read(param.bytes)
                    param.callback?.onBytesReceived(param.bytes, readBytes, null)
                } catch (exception: Exception) {
                    param.callback?.onBytesReceived(null, 0, exception)
                }
            }
            DESTROY_SSL_CLIENT -> {
                if (msg.obj !is NetworkMessageCallback)
                    return

                val callback = msg.obj as NetworkMessageCallback

                messageHandler(callback) {
                    if (sslClientSocket != null) {
                        sslClientSocket!!.close()
                        sslClientSocket = null
                    }
                }
            }
            ACCEPT_SSL_CLIENT -> {
                if (msg.obj !is SSLClientConnectedCallback)
                    return

                val callback = msg.obj as SSLClientConnectedCallback

                try {
                    val socket = sslServerSocket?.accept()
                    socket?.let {
                        val sslSocket = socket as SSLSocket

                        if (sslClientSocket == null) {
                            callback.onSSLClientConnected(sslSocket, null)
                            sslClientSocket = sslSocket
                            sslClientSocket!!.startHandshake()
                            sslClientSocket!!.soTimeout = 0
                        }
                        else {
                            callback.onSSLClientConnected(null, null)
                            sslSocket.close()
                        }

                    }
                } catch (exception2: SocketTimeoutException) {
                    callback.onSSLClientConnected(null, exception2)
                } catch (exception2: Exception) {
                    callback.onSSLClientConnected(null, exception2)

                    val closeSSLSocketParam = OperateSSLSocketParam(0, null)
                    val message = Message()

                    message.what = DESTROY_SSL_SERVER
                    message.obj = closeSSLSocketParam

                    sendMessage(message)
                }
            }
        }
    }

    companion object {
        const val EXIT_THREAD = 0
        const val INIT_UDP_SOCKET = 1
        const val DESTROY_UDP_SOCKET = 2
        const val BROADCAST_PACKET = 3
        const val INIT_SSL_SERVER = 4
        const val DESTROY_SSL_SERVER = 5
        const val SEND_SSL_BYTES = 6
        const val RECEIVE_SSL_BYTES = 7
        const val ACCEPT_SSL_CLIENT = 8
        const val DESTROY_SSL_CLIENT = 9
    }
}