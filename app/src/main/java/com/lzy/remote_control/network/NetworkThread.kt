package com.lzy.remote_control.network

import android.os.Handler
import android.os.Looper
import android.os.Message

class NetworkThread: Thread() {
    private var handler: NetworkMessageHandler? = null

    fun terminate() {
        if (state == State.NEW) return
        val exitThreadMessage = Message()
        exitThreadMessage.what = NetworkMessageHandler.EXIT_THREAD
        getHandler().sendMessage(exitThreadMessage)
        while (state != Thread.State.TERMINATED) continue
    }

    override fun run() {
        super.run()
        //Initialize looper
        Looper.prepare()
        val looper = Looper.myLooper() ?: throw RuntimeException("Get not get looper for current thread.")
        //Initialize handler
        synchronized(this) {
            handler = NetworkMessageHandler(looper)
        }
        //Run looper
        Looper.loop()
    }

    fun getHandler(): Handler {
        var result: NetworkMessageHandler?

        //Wait for handler being initialized.
        //Or if thread is terminated, throw exception.
        while(true) {

            synchronized(this) {
                result = handler
            }

            if (result != null)
                break

            if (state == State.TERMINATED)
                throw RuntimeException("Thread is terminated, so can not get handler.")
        }

        return result!!
    }
}