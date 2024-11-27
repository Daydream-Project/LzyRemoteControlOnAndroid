package com.lzy.remote_control

import org.junit.Test

import org.junit.Assert.*

import com.lzy.remote_control.protocol.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testResolvableDataLoader() {
        val loader = ResolvableDataLoader()
        assert(loader.resolvableDataTypeMap.size == 3)
        assert(loader.resolvableDataTypeMap[1] == NetworkPacket::class)
        assert(loader.resolvableDataTypeMap[2] == GetCurrentActivityRequest::class)
        assert(loader.resolvableDataTypeMap[3] == BroadcastRemoteControlServer::class)
    }

    @Test
    fun testNetworkPackage()
    {
        val request = GetCurrentActivityRequest()
        val networkPackage = NetworkPacket()

        networkPackage.content = request

        val packageBytes1 = networkPackage.toUBytes()

        assert(packageBytes1.size == 18)

        val packageBytes2 = arrayOf(0xff.toUByte(),0xfe.toUByte(),0xfd.toUByte(),0x2.toUByte(),0x0.toUByte(),0x0.toUByte(),0x0.toUByte(),0x0.toUByte(),0x0.toUByte(),0x0.toUByte(),0x0.toUByte(),0xff.toUByte(),0xff.toUByte(),0x0.toUByte(),0x0.toUByte(),0xfd.toUByte(),0xfe.toUByte(),0xff.toUByte())

        for (i in 0 until 18) {
            assert(packageBytes1[i] == packageBytes2[i])
        }

        val packageBytes3 = networkPackage.toUBytes()

        for (i in 0 until 18) {
            assert(packageBytes3[i] == packageBytes2[i])
        }

        val packageBytes4 = arrayOf(0x9.toUByte(),0x9.toUByte(),0xff.toUByte(),0xfe.toUByte(),0xfd.toUByte(),0x2.toUByte(),0x0.toUByte(),0x0.toUByte(),0x0.toUByte(),0x0.toUByte(),0x0.toUByte(),0x0.toUByte(),0x0.toUByte(),0xff.toUByte(),0xff.toUByte(),0x0.toUByte(),0x0.toUByte(),0xfd.toUByte(),0xfe.toUByte(),0xff.toUByte(),0x9.toUByte(),0x9.toUByte())

        networkPackage.fromUBytes(packageBytes4,2 , packageBytes4.size - 2)

        assert(networkPackage.content is GetCurrentActivityRequest)

        var hasException = false

        try {
            networkPackage.fromUBytes(packageBytes4,0,18)
        } catch (e: Exception) {
            hasException = true
        }

        assert(hasException)
    }
}