package com.bscpe.omcmapp

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class TempChartsActivity : AppCompatActivity() {



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tempcharts)

        supportFragmentManager.beginTransaction()
            .replace(R.id.tempFragmentContainer, LinechartsTempFragment())

            .commit()

        val filter = resources.getStringArray(R.array.Filter)
        val spinner = findViewById<Spinner>(R.id.time_filter)
        if (spinner != null) {
            val adapter = ArrayAdapter(this,
                android.R.layout.simple_spinner_dropdown_item, filter)
            spinner.adapter = adapter
        }

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>,
                                        view: View?, position: Int, id: Long) {
                val selectedItem = filter[position]
                Toast.makeText(parent.context, "Selected item: $selectedItem", Toast.LENGTH_SHORT).show()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Handle case where nothing is selected
            }
        }
    }

    fun goToMain(view: View) {
        val intent = Intent(this, MainActivity::class.java)

        val options = ActivityOptions.makeCustomAnimation(this,
                R.anim.slide_enter_right, //Entrance animation
                R.anim.slide_exit_left //Exit animation
        )

        startActivity(intent, options.toBundle())
    }
}