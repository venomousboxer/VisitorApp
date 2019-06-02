package com.example.venomousboxer.visitorapp

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.TextView

class VisitorActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_visitor)
        val intent = intent
        val i = intent.getIntExtra("VisitorCount",1)
        val tv : TextView = findViewById(R.id.welcome_message_tv)
        tv.text = "Welcome back for $i time"
    }
}
