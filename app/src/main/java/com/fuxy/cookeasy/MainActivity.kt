package com.fuxy.cookeasy

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import com.fuxy.cookeasy.s3.AmazonS3Credentials

class MainActivity : AppCompatActivity() {

    private var helloWorldTextView: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        helloWorldTextView = findViewById(R.id.tv_hello_world)

        helloWorldTextView?.text = AmazonS3Credentials.getInstance(applicationContext).awsService
    }
}
