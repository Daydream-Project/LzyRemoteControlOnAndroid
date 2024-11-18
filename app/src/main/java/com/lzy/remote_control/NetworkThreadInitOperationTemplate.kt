package com.lzy.remote_control

import com.lzy.remote_control.network.NetworkMessageCallback

abstract class NetworkThreadInitOperationTemplate: NetworkMessageCallback {
    private var initStatus = 0

    override fun onMessageHandled(exception: Exception?) {
        synchronized(this) {
            initStatus = if (exception == null) 1 else 2
        }
    }

    fun waitForInitSuccess(): Boolean {
        //Wait for the result of initialization of udpSocket.
        while(true) {
            var initUdpSocketStatusCopy: Int

            synchronized(this) {
                initUdpSocketStatusCopy = initStatus
            }

            //If initialization is failed, return false
            if (initUdpSocketStatusCopy == 2) {
                return false
            }

            //If initialization is ok, return true
            else if (initUdpSocketStatusCopy == 1) {
                return true
            }
        }
    }
}