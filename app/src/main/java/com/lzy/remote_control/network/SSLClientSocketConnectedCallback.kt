package com.lzy.remote_control.network

import javax.net.ssl.SSLSocket

interface SSLClientConnectedCallback {
    fun onSSLClientConnected(socket: SSLSocket?, exception: Exception?)
}