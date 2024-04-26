package com.bscpe.omcmapp

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class LinechartsActivity : AppCompatActivity() {

    private lateinit var tempChart: LineChart
    private val temperatureValues = mutableListOf<Float>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_linecharts)

        tempChart = findViewById(R.id.temp_lineChart)

        fetchDataFromFB()
    }

    private fun fetchDataFromFB() {
        val database = FirebaseDatabase.getInstance()
        val currentUser = FirebaseAuth.getInstance().currentUser
        val userUid = currentUser?.uid

        val myRef = database.getReference("UsersData/$userUid/readings")

        myRef.limitToLast(5).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // Log the key of the parent node
                Log.d("FirebaseData", "Parent Node Key: ${dataSnapshot.key}")

                for (snapshot in dataSnapshot.children) {
                    // Log the key of each child node
                    Log.d("FirebaseData", "Child Node Key: ${snapshot.key}")

                    for (childSnapshot in snapshot.children) {
                        val temperatureString = snapshot.child("temperature").getValue(String::class.java)
                        val temperatureFloat = temperatureString?.toFloatOrNull()
                        temperatureFloat?.let {
                            temperatureValues.add(it)
                        }
                    }
                    setupChart()
                }
            }
            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("LinechartsActivity", "Database error: ${databaseError.message}")
            }
        })
    }

    private fun setupChart() {
        val entries = mutableListOf<Entry>()
        for((index, temperature) in temperatureValues.withIndex()) {
            entries.add(Entry(index.toFloat(), temperature))
        }

        val dataSet = LineDataSet(entries, "Temperature")
        val lineData = LineData(dataSet)
        tempChart.data = lineData
        tempChart.invalidate()
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