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

class LightChartFragment : Fragment(R.layout.fragment_light_chart) {

    private lateinit var lightChart: LineChart
    private val lightValues = mutableListOf<Float>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_light_chart, container, false)
        lightChart = view.findViewById(R.id.light_lineChart)
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

                val currentDate = view?.findViewById<TextView>(R.id.currentDateChart)

                val entries = mutableListOf<Entry>()
                val labels = mutableListOf<String>()

                val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val timeOutputFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
                val dateOutputFormatter = SimpleDateFormat("MMM dd", Locale.getDefault())

                for (snapshot in dataSnapshot.children) {

                    for (childSnapshot in snapshot.children) {
                        val lightString = snapshot.child("lux").getValue(String::class.java)
                        val lightFloat = lightString?.toFloatOrNull()
                        val lightTimestampString = snapshot.child("timestamp").getValue(String::class.java)

                        if (lightFloat != null && lightTimestampString != null) {
                            lightValues.add(lightFloat)
                            entries.add(Entry(entries.size.toFloat(), lightFloat))

                            val date = dateFormatter.parse(lightTimestampString)
                            val timeFormattedTime = timeOutputFormatter.format(date)
                            val dateFormattedDate = dateOutputFormatter.format(date)

                            currentDate?.text = dateFormattedDate
                            labels.add(timeFormattedTime)
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
        val entries = mutableListOf<Entry>()
        for((index, intTemperature) in lightValues.withIndex()) {
            entries.add(Entry(index.toFloat(), intTemperature))
        }

        val dataSet = LineDataSet(entries, "Light")
        dataSet.color = ContextCompat.getColor(requireContext(), R.color.highlight)
        dataSet.setDrawCircles(false)
        dataSet.setDrawValues(false)
        dataSet.lineWidth = 2F
        dataSet.form = Legend.LegendForm.LINE

        val legend = lightChart.legend
        legend.verticalAlignment = Legend.LegendVerticalAlignment.TOP

        lightChart.description.isEnabled = false
        lightChart.legend.isEnabled = true
        lightChart.axisRight.isEnabled = false

        val lineData = LineData(dataSet)
        lightChart.data = lineData

        //        tempChart.xAxis.isEnabled = false
        lightChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        lightChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        lightChart.invalidate()
    }
}