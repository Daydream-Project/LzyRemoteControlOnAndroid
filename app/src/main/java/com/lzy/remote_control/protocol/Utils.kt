package com.lzy.remote_control.protocol

import java.security.InvalidParameterException

fun calculateCrc16(bytes: Array<Byte>, startIndex: Int = 0, endIndex: Int = bytes.size): Int
{
    if (startIndex < 0 || startIndex > bytes.size - 1)
        throw InvalidParameterException("startIndex is invalid")
    if (endIndex < 0 || endIndex > bytes.size - 1)
        throw InvalidParameterException("endIndex is invalid")

    var crc = 0xFFFF
    for (idx in startIndex until  endIndex) {
        val byte = bytes[idx]
        crc = crc xor byte.toInt()
        for (i in 0..7) {
            crc = if ((crc and 0x0001) != 0) {
                (crc shr 1) xor 0xA001
            } else {
                crc shr 1
            }
        }
    }

    return crc and 0xFFFF
}

fun bytesToLong(bytes: Array<Byte>, startIndex: Int): Long {
    var result : Long = 0
    var temp : Long

    for (i in startIndex until startIndex + 8) {
        temp = bytes[i].toLong()
        result = result or (temp shl ((i - startIndex) * 8))
    }

    return result
}

fun bytesToInt(bytes: Array<Byte>, startIndex: Int): Int
{
    var result = 0
    var temp : Int

    for (i in startIndex until startIndex + 4) {
        temp = bytes[i].toInt()
        result = result or (temp shl ((i - startIndex) * 8))
    }

    return result
}

fun longToBytes(value: Long): Array<Byte> {
    val result = Array<Byte>(8) { _ -> 0 }

    for (i in 0 until 8) {
        result[i] = (value shr (8 * (7 - i))).toByte()
    }

    return result
}

fun intToBytes(value: Int): Array<Byte> {
    val result = Array<Byte>(4) { _ -> 0 }

    for (i in 0 until 4) {
        result[i] = (value shr (8 * (7 - i))).toByte()
    }

    return result
}