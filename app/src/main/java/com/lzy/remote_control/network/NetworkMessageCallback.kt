package com.lzy.remote_control.network

interface NetworkMessageCallback {
    //Calls when message handle completed. success with null for exception, and error with non null value for exception.
    fun onBroadcastCompleted(exception: Exception?)
}