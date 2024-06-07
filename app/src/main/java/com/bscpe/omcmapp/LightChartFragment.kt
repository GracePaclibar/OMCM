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

class LightChartFragment : Fragment(R.layout.fragment_line_chart) {

    private lateinit var lightChart: LineChart
    private val lightValues = mutableListOf<Float>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_line_chart, container, false)
        lightChart = view.findViewById(R.id.lineChart)
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
                fetchDataFromFB()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("LightChartFragment", "Database error: ${error.message}")
            }

        })
    }

    private fun fetchDataFromFB() {
        val database = FirebaseDatabase.getInstance()
        val currentUser = FirebaseAuth.getInstance().currentUser
        val userUid = currentUser?.uid

        val myRef = database.getReference("UsersData/$userUid/readings")

        myRef.limitToLast(37).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {

                val currentDate = view?.findViewById<TextView>(R.id.currentDateChart)

                val entries = mutableListOf<Entry>()
                val labels = mutableListOf<String>()

                val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val timeOutputFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
                val dateOutputFormatter = SimpleDateFormat("MMM dd", Locale.getDefault())

                val processedTimestamps = mutableSetOf<String>()

                for (snapshot in dataSnapshot.children) {

                    for (childSnapshot in snapshot.children) {
                        val lightString = snapshot.child("lux").getValue(String::class.java)
                        val lightFloat = lightString?.toFloatOrNull()
                        val lightTimestampString = snapshot.child("timestamp").getValue(String::class.java)

                        if (lightFloat != null
                            && lightTimestampString != null
                            && !processedTimestamps.contains(lightTimestampString)) {
                            lightValues.add(lightFloat)
                            entries.add(Entry(entries.size.toFloat(), lightFloat))

                            val date = dateFormatter.parse(lightTimestampString)
                            val timeFormattedTime = timeOutputFormatter.format(date)
                            val dateFormattedDate = dateOutputFormatter.format(date)

                            currentDate?.text = dateFormattedDate
                            labels.add(timeFormattedTime)

                            processedTimestamps.add(lightTimestampString)
                        }
                    }
                    setupChart(entries, labels)
                }
            }
            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("LinechartsFragment", "Database error: ${databaseError.message}")
            }
        })
    }

    private fun setupChart(entries: List<Entry>, labels: List<String>) {
        val dataSet = LineDataSet(entries, "Light").apply {
            color = ContextCompat.getColor(requireContext(), R.color.highlight)
            setDrawCircles(false)
            setDrawValues(false)
            lineWidth = 2F
            form = Legend.LegendForm.LINE
        }

        lightChart.apply {
            description.isEnabled = false
            legend.isEnabled = true
            axisRight.isEnabled = false

            setVisibleXRangeMaximum(10f)
            moveViewToX(0f)

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
                            // Show the data and timestamp associated with the selected data point
                            val message = "$data lux at $timestamp"
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                override fun onNothingSelected() {
                }
            })

            data = LineData(dataSet)
            invalidate()
        }
    }

}