package com.lzy.remote_control.network


interface SSLTransferCallback {
    fun onTransferCompleted(bytes: ByteArray, offset: Int, length: Int, transferBytes: Int, isReceive: Boolean, exception: Exception?)
}