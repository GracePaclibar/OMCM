package com.bscpe.omcmapp

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class LinechartsHumidFragment : Fragment(R.layout.fragment_humid_chart) {

    private lateinit var humidChart: LineChart
    private val humidValues = mutableListOf<Float>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_humid_chart, container, false)
        humidChart = view.findViewById(R.id.humid_lineChart)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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
                        val humidString = snapshot.child("humidity").getValue(String::class.java)
                        val humidFloat = humidString?.toFloatOrNull()

                        // Check if the temperature value is not null and not already in the list
                        if (humidFloat != null && !humidValues.contains(humidFloat)) {
                            humidValues.add(humidFloat)
                        }
                    }
                    setupChart()
                }
            }
            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("LinechartsFragment", "Database error: ${databaseError.message}")
            }
        })
    }

    private fun setupChart() {
        val entries = mutableListOf<Entry>()
        for((index, temperature) in humidValues.withIndex()) {
            entries.add(Entry(index.toFloat(), temperature))
        }

        val dataSet = LineDataSet(entries, "Humidity")
        dataSet.color = ContextCompat.getColor(requireContext(), R.color.highlight)
        dataSet.setCircleColor(ContextCompat.getColor(requireContext(), R.color.highlight))
        dataSet.lineWidth = 2F


        humidChart.description.isEnabled = false
        humidChart.legend.isEnabled = false

        val lineData = LineData(dataSet)
        humidChart.data = lineData
        humidChart.invalidate()
    }
}