package com.bscpe.omcmapp

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Locale

class TempChartFragment : Fragment(R.layout.fragment_temp_chart) {

    private lateinit var tempChart: LineChart
    private val intTemperatureValues = mutableListOf<Float>()
    private val extTemperatureValues = mutableListOf<Float>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_temp_chart, container, false)
        tempChart = view.findViewById(R.id.temp_lineChart)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fetchDataFromFB(view)
    }

    private fun fetchDataFromFB(view: View) {
        val database = FirebaseDatabase.getInstance()
        val currentUser = FirebaseAuth.getInstance().currentUser
        val userUid = currentUser?.uid

        val myRef = database.getReference("UsersData/$userUid/readings")

        myRef.limitToLast(7).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // Log the key of the parent node
                Log.d("FirebaseData", "Parent Node Key: ${dataSnapshot.key}")

                val currentDate = view.findViewById<TextView>(R.id.currentDateChart)

                val entries = mutableListOf<Entry>()
                val labels = mutableListOf<String>()

                val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val timeOutputFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
                val dateOutputFormatter = SimpleDateFormat("MMM dd", Locale.getDefault())

                for (snapshot in dataSnapshot.children) {
                    // Log the key of each child node
                    Log.d("FirebaseData", "Child Node Key: ${snapshot.key}")

                    for (childSnapshot in snapshot.children) {
                        val intTemperatureString = snapshot.child("internal_temperature").getValue(String::class.java)
                        val intTemperatureFloat = intTemperatureString?.toFloatOrNull()
                        val intTempTimestampString = snapshot.child("timestamp").getValue(String::class.java)

                        if (intTemperatureFloat != null && intTempTimestampString != null) {
                            intTemperatureValues.add(intTemperatureFloat)
                            entries.add(Entry(entries.size.toFloat(), intTemperatureFloat))

                            val date = dateFormatter.parse(intTempTimestampString)
                            val timeFormattedTime = timeOutputFormatter.format(date)
                            val dateFormattedDate = dateOutputFormatter.format(date)

                            currentDate.text = dateFormattedDate
                            labels.add(timeFormattedTime)
                        }
                    }
                }
                setupChart(entries, labels)
            }
            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("LinechartsFragment", "Database error: ${databaseError.message}")
            }
        })
    }

    private fun setupChart(entries: List<Entry>, labels: List<String>) {
        val entries = mutableListOf<Entry>()
        for((index, intTemperature) in intTemperatureValues.withIndex()) {
            entries.add(Entry(index.toFloat(), intTemperature))
        }

        val dataSet = LineDataSet(entries, "Temperature")
        dataSet.color = ContextCompat.getColor(requireContext(), R.color.highlight)
        dataSet.setCircleColor(ContextCompat.getColor(requireContext(), R.color.highlight))
        dataSet.setDrawCircles(false)
        dataSet.setDrawValues(false)
        dataSet.lineWidth = 2F

        tempChart.description.isEnabled = false
        tempChart.legend.isEnabled = false
        tempChart.axisRight.isEnabled = false

        val lineData = LineData(dataSet)
        tempChart.data = lineData

        //        tempChart.xAxis.isEnabled = false
        tempChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        tempChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        tempChart.invalidate()
    }
}