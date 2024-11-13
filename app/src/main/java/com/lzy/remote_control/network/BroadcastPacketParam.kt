package com.lzy.remote_control.network

import java.net.DatagramPacket

//Parameter of BROADCAST_PACKET
data class BroadcastPacketParam(var packet: DatagramPacket, var callback: NetworkMessageCallback?)
