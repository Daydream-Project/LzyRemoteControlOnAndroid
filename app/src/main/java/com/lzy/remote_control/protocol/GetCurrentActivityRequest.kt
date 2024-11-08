package com.lzy.remote_control.protocol

@ResolvableDataType(2,"Request to get current running activity in device")
class GetCurrentActivityRequest: ResolvableData {
    override fun toBytes(): Array<Byte> {
        return arrayOf()
    }

    override fun countBytes(): Long {
        return 0
    }

    override fun fromBytes(bytes: Array<Byte>) {
        return
    }
}