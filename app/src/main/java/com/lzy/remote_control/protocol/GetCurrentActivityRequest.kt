package com.lzy.remote_control.protocol

@ResolvableDataType(2,"Request to get current running activity in device")
class GetCurrentActivityRequest: ResolvableData {

    //Just using data type id to identify this request, so override ResolvableData methods width empty implement.

    override fun toUBytes(): Array<UByte> {
        return arrayOf()
    }

    override fun countUBytes(): Int {
        return 0
    }

    override fun fromUBytes(bytes: Array<UByte>, startIndex: Int, endIndex: Int) {
        return
    }
}