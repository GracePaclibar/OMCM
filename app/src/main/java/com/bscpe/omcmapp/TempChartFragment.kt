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
import com.github.mikephil.charting.components.Legend
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
                val currentDate = view.findViewById<TextView>(R.id.currentDateChart)

                val labels = mutableListOf<String>()

                val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val timeOutputFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
                val dateOutputFormatter = SimpleDateFormat("MMM dd", Locale.getDefault())

                for (snapshot in dataSnapshot.children) {
                    for (childSnapshot in snapshot.children) {
                        val intTemperatureString = snapshot.child("internal_temperature").getValue(String::class.java)
                        val intTemperatureFloat = intTemperatureString?.toFloatOrNull()
                        val extTemperatureString = snapshot.child("external_temperature").getValue(String::class.java)
                        val extTemperatureFloat = extTemperatureString?.toFloatOrNull()
                        val intTempTimestampString = snapshot.child("timestamp").getValue(String::class.java)

                        if (intTemperatureFloat != null && intTempTimestampString != null && extTemperatureFloat != null) {
                            intTemperatureValues.add(intTemperatureFloat)
                            extTemperatureValues.add(extTemperatureFloat)

                            val date = dateFormatter.parse(intTempTimestampString)
                            val timeFormattedTime = timeOutputFormatter.format(date)
                            val dateFormattedDate = dateOutputFormatter.format(date)

                            currentDate.text = dateFormattedDate
                            labels.add(timeFormattedTime)
                        }
                    }
                }

                val intEntries = mutableListOf<Entry>()
                for ((index, value) in intTemperatureValues.withIndex()) {
                    intEntries.add(Entry(index.toFloat(), value))
                }

                val extEntries = mutableListOf<Entry>()
                for ((index, value) in extTemperatureValues.withIndex()) {
                    extEntries.add(Entry(index.toFloat(), value))
                }

                setupChart(intEntries, extEntries, labels)
            }
            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("LinechartsFragment", "Database error: ${databaseError.message}")
            }
        })
    }

    private fun setupChart(
        intEntries: List<Entry>,
        extEntries: List<Entry>,
        labels: List<String>
    ) {
        if (intEntries.isNotEmpty() && extEntries.isNotEmpty()) {
            val intDataSet = LineDataSet(intEntries, "Internal Temperature")
            intDataSet.color = ContextCompat.getColor(requireContext(), R.color.highlight)
            intDataSet.setDrawCircles(false)
            intDataSet.setDrawValues(false)
            intDataSet.lineWidth = 2F
            intDataSet.form = Legend.LegendForm.LINE

            val extDataSet = LineDataSet(extEntries, "External Temperature")
            extDataSet.color = ContextCompat.getColor(requireContext(), R.color.main)
            extDataSet.setDrawCircles(false)
            extDataSet.setDrawValues(false)
            extDataSet.lineWidth = 2F

            tempChart.description.isEnabled = false
            tempChart.legend.isEnabled = true
            tempChart.axisRight.isEnabled = false
            extDataSet.form = Legend.LegendForm.LINE

            val legend = tempChart.legend
            legend.verticalAlignment = Legend.LegendVerticalAlignment.TOP

            val lineData = LineData(intDataSet, extDataSet)
            tempChart.data = lineData

            tempChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            tempChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
            tempChart.invalidate()
        } else {
            Log.d("ChartSetup", "No temperature data to display.")
        }
    }
}