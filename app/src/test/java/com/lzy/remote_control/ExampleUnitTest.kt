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
        assert(loader.resolvableDataTypes.size == 1)
        assert(loader.resolvableDataTypes[0] == NetworkPackage::class)
    }
}