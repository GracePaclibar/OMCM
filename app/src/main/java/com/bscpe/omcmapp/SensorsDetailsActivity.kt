package com.bscpe.omcmapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView

class SensorsDetailsActivity : AppCompatActivity() {
    private lateinit var tvTemperature: TextView
    private lateinit var tvHumidity: TextView
    private lateinit var tvLight: TextView
    private lateinit var btnUpdate: Button
    private lateinit var btnDelete: Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sensors_details)

        initView()
        setValuesToViews()

    }

    private fun initView() {
        tvTemperature = findViewById(R.id.tvTemperature)
        tvHumidity = findViewById(R.id.tvHumidity)
        tvLight = findViewById(R.id.tvLight)

        btnUpdate = findViewById(R.id.btnUpdate)
        btnDelete = findViewById(R.id.btnDelete)
    }

    private fun setValuesToViews() {
        tvTemperature.text = intent.getStringExtra("temperature")
        tvHumidity.text = intent.getStringExtra("humidity")
        tvLight.text = intent.getStringExtra("lux")

    }
}