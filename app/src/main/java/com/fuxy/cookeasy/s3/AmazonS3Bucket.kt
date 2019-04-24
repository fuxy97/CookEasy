package com.fuxy.cookeasy.s3

import android.content.Context
import android.os.AsyncTask.execute
import android.util.Log
import okhttp3.*
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*

fun getSimpleObjectOperationHeaders(httpMethod: HTTPMethod, context: Context, absolutePath: String): Map<String, String> {
    val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    val iso8601DateFormat = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.GERMANY)
    iso8601DateFormat.timeZone = TimeZone.getTimeZone("UTC")

    val headers = hashMapOf(
        "Host" to AmazonS3Credentials.getInstance(context).host,
        "x-amz-date" to iso8601DateFormat.format(calendar.time)
    )

    val authenticationCode = calculateAuthenticationCode(
        context = context,
        httpMethod = httpMethod,
        uri = "/$absolutePath",
        headers = headers
    )

    val md = MessageDigest.getInstance("SHA-256")
    val digest = md.digest()
    val hashedPayload = digest.fold("", { str, it -> str + "%02x".format(it) })

    var newHeaders = headers.plus(Pair("x-amz-content-sha256", hashedPayload))
    newHeaders = newHeaders.plus(Pair("Authorization", authenticationCode))

    return newHeaders
}

suspend fun getObjectFromBucket(context: Context, absolutePath: String): ResponseBody {
    val api = AmazonS3Service.getInstance(context).api
    val headers = getSimpleObjectOperationHeaders(HTTPMethod.GET, context, absolutePath)
    return api.getObject(absolutePath, headers).await()
}

suspend fun deleteObjectFromBucket(context: Context, absolutePath: String): ResponseBody {
    val api = AmazonS3Service.getInstance(context).api
    val headers = getSimpleObjectOperationHeaders(HTTPMethod.DELETE, context, absolutePath)
    return api.deleteObject(absolutePath, headers).await()
}

suspend fun putObjectToBucket(context: Context, absolutePath: String, contentType: String, bytes: ByteArray): ResponseBody {
    val api = AmazonS3Service.getInstance(context).api

    val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    val iso8601DateFormat = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.GERMANY)
    iso8601DateFormat.timeZone = TimeZone.getTimeZone("UTC")

    val headers = hashMapOf(
        "Host" to AmazonS3Credentials.getInstance(context).host,
        "x-amz-date" to iso8601DateFormat.format(calendar.time)
    )
    val authenticationCode = calculateAuthenticationCode(
        context = context,
        httpMethod = HTTPMethod.PUT,
        uri = "/$absolutePath",
        headers = headers,
        payload = bytes
    )

    val md = MessageDigest.getInstance("SHA-256")
    val digest = md.digest(bytes)
    val hashedPayload = digest.fold("", { str, it -> str + "%02x".format(it) })

    var newHeaders = headers.plus(Pair("x-amz-content-sha256", hashedPayload))
    newHeaders = newHeaders.plus(Pair("Authorization", authenticationCode))

    return api.putObject(absolutePath, newHeaders,
        RequestBody.create(MediaType.parse(contentType), bytes)).await()
}

