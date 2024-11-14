package com.lzy.remote_control.security

import android.util.Base64
import java.security.MessageDigest

fun hashPassword(password: String): String {
    val crypto = MessageDigest.getInstance("SHA-256")
    val passwordBytes = password.encodeToByteArray()
    val resultBytes = crypto.digest(passwordBytes)
    return Base64.encodeToString(resultBytes, Base64.DEFAULT)
}
