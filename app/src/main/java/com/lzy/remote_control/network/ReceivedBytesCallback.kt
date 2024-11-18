package com.lzy.remote_control.network


interface ReceivedBytesCallback {
    fun onBytesReceived(bytes: ByteArray?, length: Int, exception: Exception?)
}