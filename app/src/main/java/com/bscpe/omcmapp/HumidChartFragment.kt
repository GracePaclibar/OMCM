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
import java.text.SimpleDateFormat
import java.util.Date

class HumidChartFragment : Fragment(R.layout.fragment_humid_chart) {

    private lateinit var humidChart: LineChart
    private val intHumidValues = mutableListOf<Float>()
    private val extHumidValues = mutableListOf<Float>()


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

                    val unixTime = snapshot.key?.toLong() ?: 0 // Assuming key is Unix timestamp
                    val formattedDate = parseUnixToDate(unixTime)
                    Log.d("FirebaseData", "Date: $formattedDate")

                    for (childSnapshot in snapshot.children) {
                        val intHumidString = snapshot.child("internal_humidity").getValue(String::class.java)
                        val intHumidFloat = intHumidString?.toFloatOrNull()

                        if (intHumidFloat != null ) {
                            intHumidValues.add(intHumidFloat)
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
        for((index, intTemperature) in intHumidValues.withIndex()) {
            entries.add(Entry(index.toFloat(), intTemperature))
        }

        val dataSet = LineDataSet(entries, "Humidity")
        dataSet.color = ContextCompat.getColor(requireContext(), R.color.highlight)
        dataSet.setCircleColor(ContextCompat.getColor(requireContext(), R.color.highlight))
        dataSet.setDrawCircles(false)
        dataSet.setDrawValues(false)
        dataSet.lineWidth = 2F

        humidChart.description.isEnabled = false
        humidChart.legend.isEnabled = false

        humidChart.axisRight.isEnabled = false

        val lineData = LineData(dataSet)
        humidChart.data = lineData
        humidChart.invalidate()
    }

    private fun parseUnixToDate(unixTime: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val date = Date(unixTime * 1000)
        return sdf.format(date)
    }
}