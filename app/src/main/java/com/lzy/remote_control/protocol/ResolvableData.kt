package com.lzy.remote_control.protocol

interface ResolvableData {
    //Parse to bytes
    fun toUBytes(): Array<UByte>
    //Get byte count parse to
    fun countUBytes(): Int
    //Parse from byte
    //range is [startIndex, endIndex)
    fun fromUBytes(bytes: Array<UByte>, startIndex: Int = 0, endIndex: Int = bytes.size)
}