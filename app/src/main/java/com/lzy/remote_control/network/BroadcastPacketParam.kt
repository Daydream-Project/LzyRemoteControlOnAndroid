package com.lzy.remote_control.network

import java.net.DatagramPacket

//Parameter of BROADCAST_PACKET
data class BroadcastPacketParam(val packet: DatagramPacket, val callback: NetworkMessageCallback?)
