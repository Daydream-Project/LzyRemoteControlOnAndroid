package com.lzy.remote_control.protocol

interface ResolvableData {
    fun toBytes(): Array<Byte>
    fun countBytes(): Long
    fun fromBytes(bytes: Array<Byte>)
}