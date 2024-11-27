package com.lzy.remote_control.network

class SSLTransferParam(var buffer: ByteArray, var offset: Int, var length: Int, var isReceive: Boolean, var callback: SSLTransferCallback) {
}
