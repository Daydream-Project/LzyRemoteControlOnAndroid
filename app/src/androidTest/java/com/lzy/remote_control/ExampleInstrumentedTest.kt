package com.lzy.remote_control

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lzy.remote_control.protocol.GetCurrentActivityRequest
import com.lzy.remote_control.protocol.NetworkPackage
import com.lzy.remote_control.protocol.ResolvableDataLoader

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.lzy.remote_control", appContext.packageName)
    }

    @Test
    fun testResolvableDataLoader() {
        val loader = ResolvableDataLoader()
        assert(loader.resolvableDataTypeMap.size == 2)
        assert(loader.resolvableDataTypeMap[1] == NetworkPackage::class)
        assert(loader.resolvableDataTypeMap[2] == GetCurrentActivityRequest::class)
    }
}