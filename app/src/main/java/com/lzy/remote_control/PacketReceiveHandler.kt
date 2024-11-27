package com.lzy.remote_control

import com.lzy.remote_control.protocol.NetworkPacket
import android.os.Handler

interface PacketReceiveHandler {
    fun onPacketReceived(packet: NetworkPacket, handler: Handler)
    fun onPacketReceiveError(exception: Exception, handler: Handler)
}