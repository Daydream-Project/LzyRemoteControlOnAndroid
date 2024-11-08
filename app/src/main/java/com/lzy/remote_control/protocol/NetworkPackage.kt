package com.lzy.remote_control.protocol

import java.security.InvalidParameterException
import kotlin.RuntimeException

@ResolvableDataType(1,"Network package.")
class NetworkPackage: ResolvableData {

    companion object {
        private val packageBegin = arrayOf(0xff.toByte(), 0xfe.toByte(), 0xfd.toByte())
        private val packageEnd = arrayOf(0xfd.toByte(), 0xfe.toByte(), 0xff.toByte())

        private const val crcSize = 4
        private const val packageLengthSize = 8
        private const val dataTypeIdSize = 4

        private val loader = ResolvableDataLoader()

        private fun packageCheck(bytes: Array<Byte>)
        {
            //检查大小
            if (bytes.size < packageBegin.size + packageEnd.size + dataTypeIdSize + packageLengthSize + crcSize)
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

            //检查类型
            val dataTypeId = bytesToInt(bytes, packageBegin.size)

            if (!loader.resolvableDataTypeMap.containsKey(dataTypeId))
                throw RuntimeException("Invalid data type id")

            //检查大小
            val dataSize = bytesToLong(bytes, packageBegin.size + dataTypeIdSize)

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

        val dataTypeId = bytesToInt(bytes, packageBegin.size)

        val contentBytes = bytes.slice(packageBegin.size + dataTypeIdSize + packageLengthSize..bytes.size - packageEnd.size - crcSize)

        val _content = (loader.resolvableDataTypeMap[dataTypeId]?.java?.constructors?.find { constructor -> constructor.parameterTypes.isEmpty() })?.newInstance()
            ?: throw RuntimeException("Can not construct object belong to subtype of ResolvableData without constructor with no parameter.")

        (_content as ResolvableData).fromBytes(contentBytes.toTypedArray())

        content = _content
    }

    override fun toBytes(): Array<Byte> {
        parseCheck()

        val dataTypeId = getDataTypeInfo(content!!)?.id ?: throw RuntimeException("Can not get data type info for subtype of ResolvableData")
        val dataTypeIdByteArray = intToBytes(dataTypeId)
        val contentByteArray = content!!.toBytes()
        val dataSizeByteArray = longToBytes(contentByteArray.size.toLong())
        val packageSize = countBytes().toInt()
        val crcByteArray = intToBytes(calculateCrc16(contentByteArray))

        return Array(packageSize) { index ->
            if (index < packageBegin.size)
                packageBegin[index]
            else if (index - packageBegin.size < dataTypeIdSize)
                dataTypeIdByteArray[index - packageBegin.size]
            else if (index - packageBegin.size - dataTypeIdSize < packageLengthSize)
                dataSizeByteArray[index - packageBegin.size - dataTypeIdSize]
            else if (index >= packageBegin.size + dataTypeIdSize + packageLengthSize && index < packageSize - crcSize - packageEnd.size)
                contentByteArray[index - packageBegin.size - dataTypeIdSize - packageLengthSize]
            else if (index >= packageBegin.size + dataTypeIdSize + packageLengthSize + contentByteArray.size && index < packageSize - packageEnd.size)
                crcByteArray[index - packageBegin.size - dataTypeIdSize - packageLengthSize - contentByteArray.size]
            else if (index >= packageSize - packageEnd.size && index < packageSize)
                packageEnd[index - packageSize + packageEnd.size]
            else
                throw RuntimeException("invalid index $index")
        }
    }

    override fun countBytes(): Long {
        parseCheck()
        return packageBegin.size + dataTypeIdSize + packageLengthSize + content!!.countBytes() + crcSize + packageEnd.size
    }
}