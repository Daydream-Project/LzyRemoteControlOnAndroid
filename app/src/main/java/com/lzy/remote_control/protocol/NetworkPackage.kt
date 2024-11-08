package com.lzy.remote_control.protocol

import java.security.InvalidParameterException
import kotlin.RuntimeException

@ResolvableDataType(1,"Network package.")
class NetworkPackage: ResolvableData {

    companion object {
        private val packageBegin = arrayOf(0xff.toUByte(), 0xfe.toUByte(), 0xfd.toUByte())
        private val packageEnd = arrayOf(0xfd.toUByte(), 0xfe.toUByte(), 0xff.toUByte())

        private const val crcSize = 4
        private const val packageLengthSize = 4
        private const val dataTypeIdSize = 4

        private val loader = ResolvableDataLoader()

        private fun packageCheck(bytes: Array<UByte>, startIndex: Int, endIndex: Int)
        {
            if (startIndex < 0 || (bytes.isNotEmpty() && startIndex > bytes.size - 1) || (bytes.isEmpty() && startIndex != 0))
                throw InvalidParameterException("startIndex value $startIndex is invalid")
            if (endIndex < 0 || (bytes.isNotEmpty() && endIndex > bytes.size - 1) || (bytes.isEmpty() && endIndex != 0))
                throw InvalidParameterException("endIndex value $endIndex is invalid")

            //检查大小
            if (endIndex - startIndex < packageBegin.size + packageEnd.size + dataTypeIdSize + packageLengthSize + crcSize)
                throw RuntimeException("invalid network package.")

            //检查包头
            for (i in startIndex until packageBegin.size) {
                if (bytes[startIndex + i] != packageBegin[i])
                    throw RuntimeException("invalid network package begins.")
            }

            //检查包尾
            val packageSize = endIndex - startIndex

            for (i in 0 until  packageEnd.size) {
                if (bytes[endIndex - packageEnd.size + i] != packageEnd[i])
                    throw RuntimeException("invalid network package ends.")
            }

            //检查类型
            val dataTypeId = ubytesToInt(bytes, startIndex + packageBegin.size)

            if (!loader.resolvableDataTypeMap.containsKey(dataTypeId))
                throw RuntimeException("Invalid data type id")

            //检查大小
            val dataSize = ubytesToInt(bytes, startIndex + packageBegin.size + dataTypeIdSize)

            if (dataSize + packageBegin.size + packageEnd.size + crcSize + packageLengthSize + dataTypeIdSize != packageSize)
                throw RuntimeException("invalid network package length.")
        }

        private fun crcCheck(bytes: Array<UByte>, startIndex: Int, endIndex: Int)
        {
            val packageSize = endIndex - startIndex
            val crcValue = calculateCrc16(bytes, startIndex + packageBegin.size + dataTypeIdSize + packageLengthSize,endIndex - packageEnd.size - crcSize)
            val crcValueInPackage = ubytesToInt(bytes,endIndex - packageEnd.size - crcSize)

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

    override fun fromUBytes(bytes: Array<UByte>, startIndex: Int, endIndex: Int) {
        packageCheck(bytes, startIndex, endIndex)

        crcCheck(bytes, startIndex, endIndex)

        val dataTypeId = ubytesToInt(bytes, startIndex + packageBegin.size)

        val contentLength = ubytesToInt(bytes, startIndex + packageBegin.size + dataTypeIdSize)

        val tempContent = (loader.resolvableDataTypeMap[dataTypeId]?.java?.constructors?.find { constructor -> constructor.parameterTypes.isEmpty() })?.newInstance()
            ?: throw RuntimeException("Can not construct object belong to subtype of ResolvableData without constructor with no parameter.")

        val contentStartIndex = startIndex + packageBegin.size + dataTypeIdSize + packageLengthSize

        (tempContent as ResolvableData).fromUBytes(bytes,contentStartIndex,contentStartIndex + contentLength)

        content = tempContent
    }

    override fun toUBytes(): Array<UByte> {
        parseCheck()

        val dataTypeId = getDataTypeInfo(content!!)?.id ?: throw RuntimeException("Can not get data type info for subtype of ResolvableData")
        val dataTypeIdUByteArray = intToUBytes(dataTypeId)
        val contentUByteArray = content!!.toUBytes()
        val dataSizeUByteArray = longToUBytes(contentUByteArray.size.toLong())
        val packageSize = countUBytes().toInt()
        val crcUByteArray = intToUBytes(calculateCrc16(contentUByteArray))

        return Array(packageSize) { index ->
            if (index < packageBegin.size)
                packageBegin[index]
            else if (index - packageBegin.size < dataTypeIdSize)
                dataTypeIdUByteArray[index - packageBegin.size]
            else if (index - packageBegin.size - dataTypeIdSize < packageLengthSize)
                dataSizeUByteArray[index - packageBegin.size - dataTypeIdSize]
            else if (index >= packageBegin.size + dataTypeIdSize + packageLengthSize && index < packageSize - crcSize - packageEnd.size)
                contentUByteArray[index - packageBegin.size - dataTypeIdSize - packageLengthSize]
            else if (index >= packageBegin.size + dataTypeIdSize + packageLengthSize + contentUByteArray.size && index < packageSize - packageEnd.size)
                crcUByteArray[index - packageBegin.size - dataTypeIdSize - packageLengthSize - contentUByteArray.size]
            else if (index >= packageSize - packageEnd.size && index < packageSize)
                packageEnd[index - packageSize + packageEnd.size]
            else
                throw RuntimeException("invalid index $index")
        }
    }

    override fun countUBytes(): Int {
        parseCheck()
        return packageBegin.size + dataTypeIdSize + packageLengthSize + content!!.countUBytes() + crcSize + packageEnd.size
    }
}