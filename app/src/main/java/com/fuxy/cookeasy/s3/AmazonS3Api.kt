package com.fuxy.cookeasy.s3

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*

interface AmazonS3Api {

    @GET("{objectName}")
    fun getObject(@Path("objectName") objectName: String,
                  @Header("Host") host: String,
                  @Header("Date") date: String,
                  @Header("Authorization") authorizationString: String): Call<ResponseBody>

    @GET("{objectName}")
    fun getObject(@Path("objectName") objectName: String,
                  @HeaderMap headerMap: Map<String, String>): Call<ResponseBody>

    @DELETE("{objectName}")
    fun deleteObject(@Path("objectName") objectName: String,
                     @HeaderMap headerMap: Map<String, String>): Call<ResponseBody>

    @PUT("{objectName}")
    fun putObject(@Path("objectName") objectName: String,
                  @HeaderMap headerMap: Map<String, String>,
                  @Body body: RequestBody): Call<ResponseBody>

}