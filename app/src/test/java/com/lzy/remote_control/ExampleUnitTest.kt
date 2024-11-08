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
        assert(loader.resolvableDataTypeMap.size == 2)
        assert(loader.resolvableDataTypeMap[1] == NetworkPackage::class)
        assert(loader.resolvableDataTypeMap[2] == GetCurrentActivityRequest::class)
    }

    @Test
    fun testNetworkPackage()
    {
        val request = GetCurrentActivityRequest()
        val networkPackage = NetworkPackage()

        networkPackage.content = request

        val packageBytes1 = networkPackage.toBytes()

        assert(packageBytes1.size == 22)

        val packageBytes2: Array<Byte> = arrayOf(-1,-2,-3,0,0,0,2,0,0,0,0,0,0,0,0,0,0,-1,-1,-3,-2,-1)

        for (i in 0 until 22) {
            assert(packageBytes1[i] == packageBytes2[i])
        }
    }
}