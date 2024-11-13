package com.lzy.remote_control.protocol

import java.security.InvalidParameterException
import kotlin.reflect.KClass

fun calculateCrc16(bytes: Array<UByte>, startIndex: Int = 0, endIndex: Int = bytes.size): Int
{
    if (startIndex < 0 || (bytes.isNotEmpty() && startIndex > bytes.size) || (bytes.isEmpty() && startIndex != 0))
        throw InvalidParameterException("startIndex value $startIndex is invalid")
    if (endIndex < 0 || (bytes.isNotEmpty() && endIndex > bytes.size) || (bytes.isEmpty() && endIndex != 0))
        throw InvalidParameterException("endIndex value $endIndex is invalid")

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


fun ubytesToLong(bytes: Array<UByte>, startIndex: Int): Long {
    var result : Long = 0
    var temp : Long

    for (i in 0 until 8) {
        temp = bytes[i + startIndex].toLong()
        result = result or (temp shl (i * 8))
    }

    return result
}

fun ubytesToInt(bytes: Array<UByte>, startIndex: Int): Int
{
    var result = 0
    var temp : Int

    for (i in 0 until 4) {
        temp = bytes[i + startIndex].toInt()
        result = result or (temp shl (i * 8))
    }

    return result
}

fun longToUBytes(value: Long): Array<UByte> {
    val result = Array(8) { _ -> 0.toUByte() }

    for (i in 0 until 8) {
        result[i] = (value shr (8 * i)).toUByte()
    }

    return result
}

fun intToUBytes(value: Int): Array<UByte> {
    val result = Array(4) { _ -> 0.toUByte() }

    for (i in 0 until 4) {
        result[i] = (value shr (8 * i)).toUByte()
    }

    return result
}

fun getDataTypeInfo(obj: Any): ResolvableDataType? {
    val dataTypeInfo = obj.javaClass.annotations.find { a -> a is ResolvableDataType }
    return if (dataTypeInfo != null) dataTypeInfo as ResolvableDataType else null
}

fun getDataTypeInfo(classInfo: KClass<*>): ResolvableDataType? {
    val dataTypeInfo = classInfo.java.annotations.find { a -> a is ResolvableDataType }
    return if (dataTypeInfo != null) dataTypeInfo as ResolvableDataType else null
}