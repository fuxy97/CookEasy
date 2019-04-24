package com.fuxy.cookeasy.s3

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

suspend fun <T> Call<T>.await(): T = suspendCoroutine { continuation ->
    enqueue(object : Callback<T> {
        override fun onFailure(call: Call<T>, t: Throwable) {
            continuation.resumeWithException(t)
        }

        override fun onResponse(call: Call<T>, response: Response<T>) {
            if (response.isSuccessful)
                continuation.resumeWith(Result.success(response.body()!!))
            else
                continuation.resumeWithException(Exception("Response error code: ${response.code()}"))
        }
    })
}