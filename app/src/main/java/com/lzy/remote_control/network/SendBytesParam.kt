package com.lzy.remote_control.network

data class SendBytesParam(var bytes: ByteArray, var callback: NetworkMessageCallback?) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SendBytesParam

        if (!bytes.contentEquals(other.bytes)) return false
        if (callback != other.callback) return false

        return true
    }

    override fun hashCode(): Int {
        var result = bytes.contentHashCode()
        result = 31 * result + (callback?.hashCode() ?: 0)
        return result
    }
}
