package com.lzy.remote_control.network

//Parameter of INIT_SSL_SERVER and DESTROY_SSL_SERVER
data class OperateSSLSocketParam(var port: Int, var callback: NetworkMessageCallback?)
