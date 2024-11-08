package com.lzy.remote_control.protocol

interface ResolvableData {
    fun toUBytes(): Array<UByte>
    fun countUBytes(): Int
    fun fromUBytes(bytes: Array<UByte>, startIndex: Int = 0, endIndex: Int = bytes.size)
}