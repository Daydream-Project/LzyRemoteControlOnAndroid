package com.lzy.remote_control

import android.os.Handler
import android.os.Looper
import android.os.Message
import com.lzy.remote_control.network.NetworkMessageHandler
import com.lzy.remote_control.network.SSLTransferParam
import com.lzy.remote_control.network.SSLTransferCallback
import com.lzy.remote_control.protocol.NetworkPacket
import com.lzy.remote_control.protocol.ubytesToInt

class PacketLoopReceiver(_handler: Handler, _callback: PacketReceiveHandler) : SSLTransferCallback {
    private var recvStep = 1
    private var bytesRemain = LENGTH_BEFORE_CONTENT
    private var contentLength = 0

    private val handler = _handler
    private val mainThreadHandler = Handler(Looper.getMainLooper())

    private val callback = _callback

    private val beforeContentBuffer = ByteArray(LENGTH_BEFORE_CONTENT)
    private val afterContentBuffer = ByteArray(LENGTH_AFTER_CONTENT)
    private var contentBytes: ByteArray? = null

    companion object {
        private const val RECEIVE_PACKET_BEGIN_TYPE_AND_LENGTH = 1
        private const val RECEIVE_PACKET_CONTENT = 2
        private const val RECEIVE_PACKET_CRC_AND_PACKET_END = 3

        private const val LENGTH_BEFORE_CONTENT = 11
        private const val LENGTH_AFTER_CONTENT = 7

        private const val CONTENT_LENGTH_OFFSET = 7
    }

    fun postReceiveBytes() {
        val message = Message()

        val messageParam = SSLTransferParam(ByteArray(0), 0, 0, true, this)

        message.what = NetworkMessageHandler.RECEIVE_SSL_BYTES
        message.obj = messageParam

        synchronized(this) {
            if (bytesRemain == 0) {
                ++recvStep

                if (recvStep > RECEIVE_PACKET_CRC_AND_PACKET_END)
                    recvStep = RECEIVE_PACKET_BEGIN_TYPE_AND_LENGTH

                if (recvStep == RECEIVE_PACKET_CONTENT) {
                    contentLength = ubytesToInt(beforeContentBuffer.map { it.toUByte() }.toTypedArray(), CONTENT_LENGTH_OFFSET)
                    contentBytes = ByteArray(contentLength)
                }
            }

            when (recvStep) {
                RECEIVE_PACKET_BEGIN_TYPE_AND_LENGTH -> {
                    messageParam.buffer = beforeContentBuffer
                    messageParam.offset = beforeContentBuffer.size - bytesRemain
                    messageParam.length = bytesRemain
                }
                RECEIVE_PACKET_CONTENT -> {
                    messageParam.buffer = contentBytes!!
                    messageParam.offset = contentLength - bytesRemain
                    messageParam.length = bytesRemain
                }
                RECEIVE_PACKET_CRC_AND_PACKET_END -> {
                    messageParam.buffer = afterContentBuffer
                    messageParam.offset = afterContentBuffer.size - bytesRemain
                    messageParam.length = bytesRemain
                }
            }
        }

        handler.sendMessage(message)
    }

    override fun onTransferCompleted(
        bytes: ByteArray,
        offset: Int,
        length: Int,
        transferBytes: Int,
        isReceive: Boolean,
        exception: Exception?
    )
    {
        if (exception == null) {
            synchronized(this) {
                if (recvStep == RECEIVE_PACKET_CRC_AND_PACKET_END && bytesRemain - length == 0) {
                    val ubyteArray =
                        Array<UByte>(beforeContentBuffer.size + contentBytes!!.size + afterContentBuffer.size) { index ->
                            if (index < beforeContentBuffer.size)
                                beforeContentBuffer[index].toUByte()
                            else if (index - LENGTH_BEFORE_CONTENT < contentBytes!!.size)
                                contentBytes!![index - LENGTH_BEFORE_CONTENT].toUByte()
                            else if (index - LENGTH_BEFORE_CONTENT - contentBytes!!.size < afterContentBuffer.size)
                                afterContentBuffer[index - LENGTH_BEFORE_CONTENT - contentBytes!!.size].toUByte()
                            else
                                0.toUByte()
                        }
                    try {
                        val packet = NetworkPacket()
                        packet.fromUBytes(ubyteArray)

                        mainThreadHandler.post {
                            callback.onPacketReceived(packet, handler)
                        }

                    } catch (exception: Exception) {
                        mainThreadHandler.post {
                            callback.onPacketReceiveError(exception, handler)
                        }
                    }
                }
            }
            postReceiveBytes()
        } else {
            mainThreadHandler.post {
                callback.onPacketReceiveError(exception, handler)
            }
        }
    }
}