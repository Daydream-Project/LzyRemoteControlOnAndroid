package com.lzy.remote_control.network

import android.content.Context
import android.content.Context.WIFI_SERVICE
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.Collections


//CHeck a ip address is IPV6 version.
fun IPisV6(ip: String): Boolean {
    return ip.find { value ->
        return  value == 'a' || value == 'b' || value == 'c' || value == 'd' || value == 'e' || value == 'f' ||
                value == 'A' || value == 'B' || value == 'C' || value == 'D' || value == 'E' || value == 'F' ||
                value == ':'
    } != null
}

//Get All IPs in host.
fun getIPs(type: IPType): Array<String> {
    val interfaces: List<NetworkInterface> =
        Collections.list(NetworkInterface.getNetworkInterfaces())
    val result: MutableList<String> = mutableListOf()
    for (inter in interfaces) {
        val addrs: List<InetAddress> = Collections.list(inter.getInetAddresses())
        for (addr in addrs) {
            if (!addr.isLoopbackAddress) {
                addr.hostAddress?.let { ip ->
                    if (type == IPType.ALL) {
                        result.add(ip)
                    } else {
                        val ipIsV6 = IPisV6(ip)
                        if ((type == IPType.IPV6 && ipIsV6) || (type == IPType.IPV4 && !ipIsV6))
                            result.add(ip) else ip
                    }
                }
            }
        }
    }
    return result.toTypedArray()
}