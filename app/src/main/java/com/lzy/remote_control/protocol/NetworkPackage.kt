package com.lzy.remote_control.protocol

import java.lang.RuntimeException
import java.security.InvalidParameterException

class NetworkPackage: ResolvableData {

    companion object {
        private val packageBegin = arrayOf(0xff.toByte(), 0xfe.toByte(), 0xfd.toByte())
        private val packageEnd = arrayOf(0xfd.toByte(), 0xfe.toByte(), 0xff.toByte())

        private const val crcSize = 4
        private const val packageLengthSize = 8

        private fun packageCheck(bytes: Array<Byte>)
        {
            //检查大小
            if (bytes.size < packageBegin.size + packageEnd.size + packageLengthSize + crcSize)
                throw RuntimeException("invalid network package.")

            //检查包头
            for (i in 0..packageBegin.size) {
                if (bytes[i] != packageBegin[i])
                    throw RuntimeException("invalid network package begins.")
            }

            //检查包尾
            val packageSize = bytes.size

            for (i in 0..packageEnd.size) {
                if (bytes[packageSize - i - 1] != packageEnd[i])
                    throw RuntimeException("invalid network package ends.")
            }

            //检查大小
            val dataSize = bytesToLong(bytes, packageBegin.size + packageLengthSize)

            if (dataSize + packageBegin.size + packageEnd.size + crcSize + packageLengthSize != packageSize.toLong())
                throw RuntimeException("invalid network package length.")
        }

        private fun crcCheck(bytes: Array<Byte>)
        {
            val crcValue = calculateCrc16(bytes, packageBegin.size + packageLengthSize,bytes.size - packageEnd.size - crcSize)
            val crcValueInPackage = bytesToInt(bytes,bytes.size - crcSize)

            if (crcValue != crcValueInPackage)
                throw RuntimeException("crc value not matched.")
        }
    }

    var content: ResolvableData? = null
        get() = field
        set(value) { field = value }

    private fun parseCheck()
    {
        if (content == null)
            throw InvalidParameterException("content can not be null.")
    }

    override fun fromBytes(bytes: Array<Byte>) {
        packageCheck(bytes)

        crcCheck(bytes)



    }

    override fun toBytes(): Array<Byte> {
        parseCheck()

        val contentByteArray = content!!.toBytes()
        val dataSizeByteArray = longToBytes(contentByteArray.size.toLong())
        val packageSize = countBytes().toInt()
        val crcByteArray = intToBytes(calculateCrc16(contentByteArray))

        val init: (Int) -> Byte = { index ->

            if (index < packageBegin.size)
                packageBegin[index]

            if (index - packageBegin.size < packageLengthSize)
                dataSizeByteArray[index - packageBegin.size]

            if (index > packageBegin.size + packageLengthSize && index < packageSize - crcSize - packageEnd.size)
                contentByteArray[index - packageBegin.size - packageLengthSize]

            if (index > packageBegin.size + packageLengthSize + contentByteArray.size && index < packageSize - packageEnd.size)
                crcByteArray[index - packageBegin.size - packageLengthSize - contentByteArray.size]

            if (index > packageSize - packageEnd.size && index < packageSize)
                packageEnd[index - packageSize + packageEnd.size]

            0.toByte()
        }

        return Array(packageSize, init)
    }

    override fun countBytes(): Long {
        parseCheck()
        return packageBegin.size + packageLengthSize + content!!.countBytes() + crcSize + packageEnd.size
    }

    override fun getDataType(): Int {
        return 0
    }
}