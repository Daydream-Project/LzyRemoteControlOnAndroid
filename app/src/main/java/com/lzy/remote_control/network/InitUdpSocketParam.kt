package com.lzy.remote_control.network

//Parameter of INIT_UDP_SOCKET
data class InitUdpSocketParam(var ipAddress: String, var port: Int, var callback: NetworkMessageCallback?)
