package com.lzy.remote_control.network

//Parameter of INIT_UDP_SOCKET
data class InitUdpSocketParam(val ipAddress: String, val port: Int, val callback: NetworkMessageCallback?)
