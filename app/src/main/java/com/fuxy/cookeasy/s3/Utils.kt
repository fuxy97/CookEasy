package com.fuxy.cookeasy.s3

import android.content.Context
import android.util.Log
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

fun uriEncode(input: CharSequence, encodeSlash: Boolean = true): String {
    return input.map {
        if (it !in 'A'..'Z' && it !in 'a'..'z' && it !in '0'..'9' && it != '_' && it != '-' && it != '~' && it != '.') {
            if (it == '/' && !encodeSlash)
                return@map "/"
            else
                return@map String.format("%%%02X", it.toByte())
        } else {
            return@map it
        }
    }.joinToString("")
}

fun formCanonicalRequest(
    httpMethod: HTTPMethod,
    uri: String,
    queryParameters: Map<String, String>? = null,
    headers: Map<String, String>,
    payload: ByteArray? = null
): String {
    val sb = StringBuilder()

    val md = MessageDigest.getInstance("SHA-256")
    val digest = if (payload != null) md.digest(payload) else md.digest()
    val hashedPayload = digest.fold("", { str, it -> str + "%02x".format(it) })
    val newHeaders = headers.plus(Pair("x-amz-content-sha256", hashedPayload))

    val sortedQueryParameters = queryParameters?.toSortedMap()
    val sortedHeaders = newHeaders.toSortedMap()

    sb.appendln(httpMethod.name)
    sb.appendln(uriEncode(uri, false))

    val paramIt = sortedQueryParameters?.iterator()

    if (paramIt != null) {
        while (paramIt.hasNext()) {
            val e = paramIt.next()
            if (!paramIt.hasNext()) {
                sb.appendln(uriEncode(e.key) + "=" + uriEncode(e.value))
            } else {
                sb.append(uriEncode(e.key) + "=" + uriEncode(e.value) + "&")
            }
        }
    } else {
        sb.appendln()
    }

    sortedHeaders.forEach {
        sb.appendln(it.key.toLowerCase() + ":" + it.value.trim())
    }
    sb.appendln()

    val headerIt = sortedHeaders.iterator()

    while (headerIt.hasNext()) {
        val e = headerIt.next()
        if (!headerIt.hasNext()) {
            sb.appendln(e.key.toLowerCase())
        } else {
            sb.append(e.key.toLowerCase() + ";")
        }
    }

    sb.append(hashedPayload)

    return sb.toString()
}

fun formStringToSign(context: Context, canonicalRequest: String, time: Date): String {
    val iso8601DateFormat = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.GERMANY)
    iso8601DateFormat.timeZone = TimeZone.getTimeZone("UTC")

    val simpleDateFormat = SimpleDateFormat("yyyyMMdd", Locale.GERMANY)
    simpleDateFormat.timeZone = TimeZone.getTimeZone("UTC")

    val md = MessageDigest.getInstance("SHA-256")
    val digest = md.digest(canonicalRequest.toByteArray())
    val hashedCanonicalRequest = digest.fold("", { str, it -> str + "%02x".format(it) })

    return "AWS4-HMAC-SHA256\n" +
            iso8601DateFormat.format(time) + "\n" +
            simpleDateFormat.format(time) + "/" +
            AmazonS3Credentials.getInstance(context).awsRegion + "/" +
            AmazonS3Credentials.getInstance(context).awsService + "/aws4_request\n" +
            hashedCanonicalRequest
}

fun sign(context: Context, time: Date, stringToSign: String): String {
    val simpleDateFormat = SimpleDateFormat("yyyyMMdd", Locale.GERMANY)
    simpleDateFormat.timeZone = TimeZone.getTimeZone("UTC")

    val hmac = Mac.getInstance("HmacSHA256")
    hmac.init(SecretKeySpec(
        ("AWS4" + AmazonS3Credentials.getInstance(context).secretAccessKey).toByteArray(),
        "HmacSHA256"
    ))
    val dateKey = hmac.doFinal(simpleDateFormat.format(time).toByteArray())

    hmac.init(SecretKeySpec(dateKey, "HmacSHA256"))
    val dateRegionKey = hmac.doFinal(AmazonS3Credentials.getInstance(context).awsRegion.toByteArray())

    hmac.init(SecretKeySpec(dateRegionKey, "HmacSHA256"))
    val dateRegionServiceKey = hmac.doFinal(AmazonS3Credentials.getInstance(context).awsService.toByteArray())

    hmac.init(SecretKeySpec(dateRegionServiceKey, "HmacSHA256"))
    val signingKey = hmac.doFinal("aws4_request".toByteArray())

    hmac.init(SecretKeySpec(signingKey, "HmacSHA256"))
    return hmac.doFinal(stringToSign.toByteArray()).fold("", { str, it -> str + "%02x".format(it) })
}

fun formAuthorizationHeaderValue(context: Context, time: Date, headerNames: List<String>, signature: String): String {
    val simpleDateFormat = SimpleDateFormat("yyyyMMdd", Locale.GERMANY)
    simpleDateFormat.timeZone = TimeZone.getTimeZone("UTC")

    val sb = StringBuilder()
    val newHeaderNames = headerNames.plus("x-amz-content-sha256")
    val nameIt = newHeaderNames.sorted().iterator()

    while (nameIt.hasNext()) {
        val e = nameIt.next()
        if (!nameIt.hasNext()) {
            sb.append(e.toLowerCase())
        } else {
            sb.append(e.toLowerCase() + ";")
        }
    }

    return "AWS4-HMAC-SHA256 Credential=" +
            AmazonS3Credentials.getInstance(context).accessKeyId + "/" +
            simpleDateFormat.format(time) + "/" +
            AmazonS3Credentials.getInstance(context).awsRegion + "/" +
            AmazonS3Credentials.getInstance(context).awsService + "/aws4_request,SignedHeaders=" +
            sb.toString() + ",Signature=" + signature
}

fun calculateAuthenticationCode(
    context: Context,
    httpMethod: HTTPMethod,
    uri: String,
    queryParameters: Map<String, String>? = null,
    headers: Map<String, String>,
    time: Date,
    payload: ByteArray? = null
): String {
    val canonicalRequest = formCanonicalRequest(httpMethod, uri, queryParameters, headers, payload)
    val stringToSign = formStringToSign(context, canonicalRequest, time)
    val signature = sign(context, time, stringToSign)

    return formAuthorizationHeaderValue(context, time, headers.keys.toList(), signature)
}