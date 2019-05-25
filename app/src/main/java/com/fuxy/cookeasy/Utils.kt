package com.fuxy.cookeasy

import android.content.Context
import android.net.ConnectivityManager
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket

fun isNetworkConnected(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    return (cm.activeNetworkInfo != null) && cm.activeNetworkInfo.isConnected
}

suspend fun isOnline(timeoutMs: Int): Boolean {
    return try {
        val socket = Socket()
        val socketAddress = InetSocketAddress("amazon.com", 80)
        socket.connect(socketAddress, timeoutMs)
        socket.close()
        true
    } catch (e: IOException) {
        false
    }
}