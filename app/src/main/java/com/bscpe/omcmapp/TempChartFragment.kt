package com.bscpe.omcmapp

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Locale

class TempChartFragment : Fragment(R.layout.fragment_line_chart) {

    private lateinit var tempChart: LineChart
    private val intTemperatureValues = mutableListOf<Float>()
    private val extTemperatureValues = mutableListOf<Float>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_line_chart, container, false)
        tempChart = view.findViewById(R.id.lineChart)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val database = FirebaseDatabase.getInstance()
        val currentUser = FirebaseAuth.getInstance().currentUser
        val userUid = currentUser?.uid
        val myRef = database.getReference("UsersData/$userUid/readings")

        myRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                fetchDataFromFB(view)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("WaterChartFragment", "Database error: ${error.message}")
            }

        })
    }

    private fun fetchDataFromFB(view: View) {
        val database = FirebaseDatabase.getInstance()
        val currentUser = FirebaseAuth.getInstance().currentUser
        val userUid = currentUser?.uid

        val myRef = database.getReference("UsersData/$userUid/readings")

        myRef.limitToLast(37).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val currentDate = view.findViewById<TextView>(R.id.currentDateChart)

                val labels = mutableListOf<String>()

                val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val timeOutputFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
                val dateOutputFormatter = SimpleDateFormat("MMM dd", Locale.getDefault())

                val processedTimestamps = mutableSetOf<String>()

                for (snapshot in dataSnapshot.children) {
                    for (childSnapshot in snapshot.children) {
                        val intTemperatureString = snapshot.child("internal_temperature").getValue(String::class.java)
                        val intTemperatureFloat = intTemperatureString?.toFloatOrNull()
                        val extTemperatureString = snapshot.child("external_temperature").getValue(String::class.java)
                        val extTemperatureFloat = extTemperatureString?.toFloatOrNull()
                        val intTempTimestampString = snapshot.child("timestamp").getValue(String::class.java)

                        if (intTemperatureFloat != null
                            && intTempTimestampString != null
                            && extTemperatureFloat != null
                            && !processedTimestamps.contains(intTempTimestampString)) {
                            intTemperatureValues.add(intTemperatureFloat)
                            extTemperatureValues.add(extTemperatureFloat)

                            val date = dateFormatter.parse(intTempTimestampString)
                            val timeFormattedTime = timeOutputFormatter.format(date)
                            val dateFormattedDate = dateOutputFormatter.format(date)

                            currentDate.text = dateFormattedDate
                            labels.add(timeFormattedTime)

                            processedTimestamps.add(intTempTimestampString)
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

            val intDataSet = LineDataSet(intEntries, "Internal Temperature").apply {
                color = ContextCompat.getColor(requireContext(), R.color.highlight)
                setDrawCircles(false)
                setDrawValues(false)
                lineWidth = 2F
                form = Legend.LegendForm.LINE
            }

            val extDataSet = LineDataSet(extEntries, "External Temperature").apply {
                color = ContextCompat.getColor(requireContext(), R.color.main)
                setDrawCircles(false)
                setDrawValues(false)
                lineWidth = 2F
                form = Legend.LegendForm.LINE
            }


            val idealValue = 28f
            val idealEntries = labels.indices.map { Entry(it.toFloat(), idealValue) }
            val idealDataSet = LineDataSet(idealEntries, "Ideal Temperature").apply {
                color = ContextCompat.getColor(requireContext(), R.color.detail)
                setDrawCircles(false)
                setDrawValues(false)
                lineWidth = 2F
                enableDashedLine(500f, 500f, 0f)
                form = Legend.LegendForm.LINE
            }

            tempChart.apply {
                description.isEnabled = false
                legend.isEnabled = true
                axisRight.isEnabled = false

                tempChart.setVisibleXRangeMaximum(10f)
                tempChart.moveViewToX(0f)

                legend.verticalAlignment = Legend.LegendVerticalAlignment.TOP

                xAxis.apply {
                    valueFormatter = IndexAxisValueFormatter(labels)
                    position = XAxis.XAxisPosition.BOTTOM
                }

                setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                    override fun onValueSelected(e: Entry?, h: Highlight?) {
                        e?.let {
                            val index = e.x.toInt()
                            val data = e.y

                            val timestamp = labels.getOrNull(index)
                            if (timestamp != null) {
                                val message = "$data°C at $timestamp"
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }

                    override fun onNothingSelected() {
                    }
                })

                data = LineData(intDataSet, extDataSet, idealDataSet)
                invalidate()
            }
        } else {
            Log.d("ChartSetup", "No temperature data to display.")
        }
    }
}