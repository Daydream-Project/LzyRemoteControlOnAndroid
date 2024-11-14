package com.lzy.remote_control.protocol

@ResolvableDataType(3,"Server ip and port info in udp broadcast")
class BroadcastRemoteControlServer: ResolvableData {
    var ip: String = ""
    var port: Int = 0

    companion object {
        private const val IPLengthSize = 4
        private const val PortSize = 4
    }

    override fun toUBytes(): Array<UByte> {
        //Check port value
        if (port < 0 || port > 65535)
            throw RuntimeException("Invalid ip port")

        //Get all parts of bytes
        val ipBytes = ip.toByteArray(Charsets.UTF_8)
        val ipLengthBytes = intToUBytes(ipBytes.size)
        val portBytes = intToUBytes(port)
        val totalSize = ipBytes.size + ipLengthBytes.size + portBytes.size

        //Return new array
        return Array(totalSize) {
            index ->
            if (index < ipLengthBytes.size)
                ipLengthBytes[index]
            else if (index - ipLengthBytes.size < ipBytes.size)
                ipBytes[index - ipLengthBytes.size].toUByte()
            else if (index - ipLengthBytes.size - ipBytes.size < portBytes.size)
                portBytes[index - ipLengthBytes.size - ipBytes.size]
            else
                0.toUByte()
        }
    }

    override fun countUBytes(): Int {
        return ip.toByteArray(Charsets.UTF_8).size + IPLengthSize + PortSize
    }

    override fun fromUBytes(bytes: Array<UByte>, startIndex: Int, endIndex: Int) {
        //Check package is larger than minimum size.
        val packetLength = endIndex - startIndex

        if (packetLength < IPLengthSize + PortSize)
            throw RuntimeException("Packet is too small.")

        val ipLength = ubytesToInt(bytes, startIndex)

        //Check package size calculated by index is same size calculated by package content.
        if (ipLength + IPLengthSize + PortSize != packetLength)
            throw RuntimeException("Packet string length not match.")

        //Parse data
        ip = String(bytes.map { it.toByte() }.toByteArray(), startIndex + IPLengthSize, packetLength - PortSize - IPLengthSize, Charsets.UTF_8)

        port = ubytesToInt(bytes, endIndex - PortSize)
    }
}
