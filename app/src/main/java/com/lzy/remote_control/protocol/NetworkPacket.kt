package com.lzy.remote_control.protocol

import java.security.InvalidParameterException
import kotlin.RuntimeException

@ResolvableDataType(1,"Network package.")
class NetworkPacket: ResolvableData {

    companion object {
        //Package Begin and package end bytes.
        private val packageBegin = arrayOf(0xff.toUByte(), 0xfe.toUByte(), 0xfd.toUByte())
        private val packageEnd = arrayOf(0xfd.toUByte(), 0xfe.toUByte(), 0xff.toUByte())

        //fix byte count for fix part of Package.
        private const val crcSize = 4
        private const val packageLengthSize = 4
        private const val dataTypeIdSize = 4

        //Load subtypes of ResolvableData
        private val loader = ResolvableDataLoader()

        //Check a package is valid package
        private fun packageCheck(bytes: Array<UByte>, startIndex: Int, endIndex: Int)
        {
            if (startIndex < 0 || (bytes.isNotEmpty() && startIndex > bytes.size) || (bytes.isEmpty() && startIndex != 0))
                throw InvalidParameterException("startIndex value $startIndex is invalid")
            if (endIndex < 0 || (bytes.isNotEmpty() && endIndex > bytes.size) || (bytes.isEmpty() && endIndex != 0))
                throw InvalidParameterException("endIndex value $endIndex is invalid")

            //Check if the size of bytes can be package.
            if (endIndex - startIndex < packageBegin.size + packageEnd.size + dataTypeIdSize + packageLengthSize + crcSize)
                throw RuntimeException("invalid network package.")

            //Check the package begin bytes.
            for (i in packageBegin.indices) {
                if (bytes[startIndex + i] != packageBegin[i])
                    throw RuntimeException("invalid network package begins.")
            }

            //Check the package end bytes.
            val packageSize = endIndex - startIndex

            for (i in packageEnd.indices) {
                if (bytes[endIndex - packageEnd.size + i] != packageEnd[i])
                    throw RuntimeException("invalid network package ends.")
            }

            //Check the data type id is in loader.
            val dataTypeId = ubytesToInt(bytes, startIndex + packageBegin.size)

            if (!loader.resolvableDataTypeMap.containsKey(dataTypeId))
                throw RuntimeException("Invalid data type id")

            //Check the package size calculated by index is same of the size in package.
            val dataSize = ubytesToInt(bytes, startIndex + packageBegin.size + dataTypeIdSize)

            if (dataSize + packageBegin.size + packageEnd.size + crcSize + packageLengthSize + dataTypeIdSize != packageSize)
                throw RuntimeException("invalid network package length.")
        }

        //Check the calculated CRC value is same in package.
        private fun crcCheck(bytes: Array<UByte>, startIndex: Int, endIndex: Int)
        {
            val crcValue = calculateCrc16(bytes, startIndex + packageBegin.size + dataTypeIdSize + packageLengthSize,endIndex - packageEnd.size - crcSize)
            val crcValueInPackage = ubytesToInt(bytes,endIndex - packageEnd.size - crcSize)

            if (crcValue != crcValueInPackage)
                throw RuntimeException("crc value not matched.")
        }
    }

    var content: ResolvableData? = null

    //Parse to bytes check
    //Now just check the content is not null
    private fun parseCheck()
    {
        if (content == null)
            throw InvalidParameterException("content can not be null.")
    }

    override fun fromUBytes(bytes: Array<UByte>, startIndex: Int, endIndex: Int) {
        packageCheck(bytes, startIndex, endIndex)

        crcCheck(bytes, startIndex, endIndex)

        //Parse data type id
        val dataTypeId = ubytesToInt(bytes, startIndex + packageBegin.size)

        //Parse data length
        val contentLength = ubytesToInt(bytes, startIndex + packageBegin.size + dataTypeIdSize)

        //New content for parse
        val tempContent = (loader.resolvableDataTypeMap[dataTypeId]?.java?.constructors?.find { constructor -> constructor.parameterTypes.isEmpty() })?.newInstance()
            ?: throw RuntimeException("Can not construct object belong to subtype of ResolvableData without constructor with no parameter.")

        //Calculate the end index of data
        val contentStartIndex = startIndex + packageBegin.size + dataTypeIdSize + packageLengthSize

        //Parse content
        (tempContent as ResolvableData).fromUBytes(bytes,contentStartIndex,contentStartIndex + contentLength)

        content = tempContent
    }

    override fun toUBytes(): Array<UByte> {
        parseCheck()

        //Get data type id
        val dataTypeId = getDataTypeInfo(content!!)?.id ?: throw RuntimeException("Can not get data type info for subtype of ResolvableData")
        //Get data type id bytes.
        val dataTypeIdUByteArray = intToUBytes(dataTypeId)
        //Get content bytes.
        val contentUByteArray = content!!.toUBytes()
        //Get data size.
        val dataSizeUByteArray = longToUBytes(contentUByteArray.size.toLong())
        //Get package size.
        val packageSize = countUBytes()
        //Get crc value of data.
        val crcUByteArray = intToUBytes(calculateCrc16(contentUByteArray))

        //Just return byte by index in package on lambda.
        //Then Array function assign the value to final package bytes.
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