package com.lzy.remote_control

import android.os.Handler
import com.lzy.remote_control.protocol.NetworkPacket

class PacketProcessor: PacketReceiveHandler {
    override fun onPacketReceived(packet: NetworkPacket, handler: Handler) {
    }

    override fun onPacketReceiveError(exception: Exception, handler: Handler) {
        TODO("Not yet implemented")
    }
}