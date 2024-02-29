package com.example.datingplanner

import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
class LocationDetailsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location_details)

        // Retrieve data from intent
        val name = intent.getStringExtra("name")
        val address = intent.getStringExtra("address")
        Log.e(TAG, "Name: " + name)
        Log.e(TAG, "Address: " + address)
        // Set data to views
        val textViewName: TextView = findViewById(R.id.textViewName)
        textViewName.text = name
        val textViewAddress: TextView = findViewById(R.id.textViewAddress)
        textViewAddress.text = address
    }
}