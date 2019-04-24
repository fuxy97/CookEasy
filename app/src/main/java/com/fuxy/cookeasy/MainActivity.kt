package com.fuxy.cookeasy

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import android.widget.TextView
import com.fuxy.cookeasy.s3.AmazonS3Credentials
import com.fuxy.cookeasy.s3.deleteObjectFromBucket
import com.fuxy.cookeasy.s3.getObjectFromBucket
import com.fuxy.cookeasy.s3.putObjectToBucket
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.nio.charset.Charset
import java.util.logging.Logger

class MainActivity : AppCompatActivity() {

    private var helloWorldTextView: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        helloWorldTextView = findViewById(R.id.tv_hello_world)

        GlobalScope.launch {
            val responseBody = getObjectFromBucket(applicationContext, "amazon_zalupa.png")
            Log.d("response bytes size: ", responseBody.bytes().size.toString())
        }
    }
}
