package com.lzy.remote_control.network

data class ReceiveBytesParam(var bytes: ByteArray, var callback: ReceivedBytesCallback?) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ReceiveBytesParam

        if (!bytes.contentEquals(other.bytes)) return false
        if (callback != other.callback) return false

        return true
    }

    override fun hashCode(): Int {
        var result = bytes.contentHashCode()
        result = 31 * result + callback.hashCode()
        return result
    }
}
