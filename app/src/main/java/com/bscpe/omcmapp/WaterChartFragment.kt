package com.bscpe.omcmapp

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class WaterChartFragment : Fragment(R.layout.fragment_water_flow) {

    private val calendar = Calendar.getInstance()
    private lateinit var waterChart: BarChart

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_water_flow, container, false)
        waterChart = view.findViewById(R.id.barChart)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val dateTextView = view.findViewById<TextView>(R.id.currentDateChart)

        setDate(dateTextView)
        fetchDataFromFB(view)
    }

    private fun setDate(
        dateTextView: TextView
    ) {
        val currentDate = SimpleDateFormat("MMMM dd", Locale.getDefault()).format(calendar.time)
        dateTextView.text = currentDate
    }

    private fun fetchDataFromFB(view: View) {
        val database = FirebaseDatabase.getInstance()
        val currentUser = FirebaseAuth.getInstance().currentUser
        val userUid = currentUser?.uid

        val myRef = database.getReference("UsersData/$userUid/readings")

        myRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {

                val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val dateOutputFormatter = SimpleDateFormat("MM/dd", Locale.getDefault())

                val aggregatedValues = mutableMapOf<String, Float>()
                val processedTimestamps = mutableSetOf<String>()

                for (snapshot in dataSnapshot.children) {
                    for (childSnapshot in snapshot.children) {
                        val waterString = snapshot.child("water_flow").getValue(String::class.java)
                        val waterFloat = waterString?.toFloatOrNull()
                        val waterTimeStampString = snapshot.child("timestamp").getValue(String::class.java)

                        if (waterFloat != null && waterTimeStampString != null && !processedTimestamps.contains(waterTimeStampString)) {
                            val date = dateFormatter.parse(waterTimeStampString)
                            val dateFormattedDate = dateOutputFormatter.format(date)

                            val currentValue = aggregatedValues[dateFormattedDate] ?: 0f
                            aggregatedValues[dateFormattedDate] = currentValue + waterFloat

                            processedTimestamps.add(waterTimeStampString)
                        }
                    }
                }

                val waterEntries = mutableListOf<Entry>()
                val labels = mutableListOf<String>()

                val last7Days = aggregatedValues.keys.toList().takeLast(7).reversed()
                for (date in last7Days) {
                    waterEntries.add(Entry(labels.size.toFloat(), aggregatedValues[date] ?: 0f))
                    labels.add(date)
                }

                setupChart(waterEntries, labels, view)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("BarchartsFragment", "Database error: ${databaseError.message}")
            }
        })
    }



    private fun setupChart(entries: List<Entry>, labels: List<String>, view: View) {
        val barEntries = entries.mapIndexed { index, entry ->
            BarEntry(index.toFloat(), entry.y)
        }

        val barDataSet = BarDataSet(barEntries, "Water Flow").apply {
            color = ContextCompat.getColor(requireContext(), R.color.main)
            setDrawValues(true)
        }
        val data = BarData(barDataSet)

        val barChart = view.findViewById<BarChart>(R.id.barChart)
        barChart.data = data
        barChart.setFitBars(true)
        barChart.description.isEnabled = false
        barChart.legend.isEnabled = false
        barChart.axisRight.isEnabled = false

        val xAxis = barChart.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setCenterAxisLabels(false)
        xAxis.isGranularityEnabled = true

        barChart.invalidate()
    }

}