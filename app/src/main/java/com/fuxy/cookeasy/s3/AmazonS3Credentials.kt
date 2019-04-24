package com.fuxy.cookeasy.s3

import android.content.Context
import com.fuxy.cookeasy.R
import com.fuxy.cookeasy.SingletonHolder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.InputStreamReader

class AmazonS3Credentials private constructor(context: Context) {
    val awsService: String
    val awsRegion: String
    val accessKeyId: String
    val secretAccessKey: String
    val bucketName: String
    val host: String
    val protocol: String

    init {
        val inputStream = context.resources.openRawResource(R.raw.s3_credentials)
        val jsonObject = JsonParser().parse(InputStreamReader(inputStream, "UTF-8")) as JsonObject
        val s3CredentialsObject = jsonObject.getAsJsonObject("s3_credentials")
        awsService = s3CredentialsObject.get("aws_service").asString
        awsRegion = s3CredentialsObject.get("aws_region").asString
        accessKeyId = s3CredentialsObject.get("access_key_id").asString
        secretAccessKey = s3CredentialsObject.get("secret_access_key").asString
        bucketName = s3CredentialsObject.get("bucket_name").asString
        host = s3CredentialsObject.get("host").asString
        protocol = s3CredentialsObject.get("protocol").asString
    }

    companion object : SingletonHolder<AmazonS3Credentials, Context>(::AmazonS3Credentials)
}