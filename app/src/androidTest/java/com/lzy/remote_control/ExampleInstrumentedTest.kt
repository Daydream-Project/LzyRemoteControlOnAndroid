package com.lzy.remote_control

import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lzy.remote_control.network.IPType
import com.lzy.remote_control.network.IPisV6
import com.lzy.remote_control.network.getIPs

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
    fun testIPGetter() {
        val ips = getIPs(IPType.ALL)
        for (ip in ips)
            Log.d("testIPGetter", ip)
        assert(ips.isNotEmpty())
        for (ip in getIPs(IPType.IPV6))
            assert(IPisV6(ip))
        for (ip in getIPs(IPType.IPV4))
            assert(!IPisV6(ip))
    }
}