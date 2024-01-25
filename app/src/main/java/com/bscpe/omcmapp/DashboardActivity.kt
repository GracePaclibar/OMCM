package com.bscpe.omcmapp

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.jjoe64.graphview.DefaultLabelFormatter
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import java.text.SimpleDateFormat
import java.util.*

class DashboardActivity : AppCompatActivity() {
    private lateinit var graphView: GraphView
    private lateinit var series: LineGraphSeries<DataPoint>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        graphView = findViewById(R.id.idGraphView) // Replace with your actual GraphView ID
        series = LineGraphSeries()

        // Enable zooming and scrolling
        graphView.viewport.isScalable = true
        graphView.viewport.isScrollable = true
        graphView.viewport.isXAxisBoundsManual = true

        // Set custom label formatter for the x-axis to display datetime as strings
        graphView.gridLabelRenderer.labelFormatter = object : DefaultLabelFormatter() {
            private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

            override fun formatLabel(value: Double, isValueX: Boolean): String {
                return if (isValueX) {
                    dateFormat.format(Date(value.toLong()))
                } else {
                    super.formatLabel(value, false)
                }
            }
        }

        // Set custom label count and increment for the x-axis
        graphView.gridLabelRenderer.numHorizontalLabels = 4 // Adjust the number of labels as needed
        graphView.gridLabelRenderer.labelHorizontalHeight = 150 // Adjust the height of the labels for better visibility
        graphView.gridLabelRenderer.setHorizontalLabelsAngle(45)

        // Initialize Firebase Database
        val database = FirebaseDatabase.getInstance()
        val currentUser = FirebaseAuth.getInstance().currentUser
        val myRef =
            database.getReference("UsersData/${currentUser?.uid}/readings") // Replace with your actual data node

        // Example: Read data from Firebase
        myRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // Process the data from dataSnapshot and update the GraphView
                updateGraphView(dataSnapshot)
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle errors
                Toast.makeText(this@DashboardActivity, "Failed to read value", Toast.LENGTH_SHORT)
                    .show()
            }
        })
    }

    private fun updateGraphView(dataSnapshot: DataSnapshot) {
        val dataPoints = mutableSetOf<DataPoint>() // Use a Set to automatically eliminate duplicates

        for (childSnapshot in dataSnapshot.children) {
            // Assuming your data structure has temperature and timestamp values
            val temperatureString = childSnapshot.child("temperature").value.toString()
            val timestampString = childSnapshot.child("timestamp").value.toString()

            try {
                val xValue = parseTimestamp(timestampString)
                val yValue = temperatureString.toDouble()

                // Check if the x-value already exists in the Set
                if (!dataPoints.any { it.x == xValue }) {
                    val dataPoint = DataPoint(xValue, yValue)
                    dataPoints.add(dataPoint)
                }
            } catch (e: NumberFormatException) {
                // Handle the case where parsing fails (e.g., log the error)
                Log.e(
                    "DashboardActivity",
                    "Error parsing data: $timestampString, $temperatureString",
                    e
                )
            }
        }

        // Create or update LineGraphSeries
        series.resetData(dataPoints.toTypedArray())
        graphView.removeAllSeries()
        graphView.addSeries(series)
    }

    private fun parseTimestamp(timestampString: String): Double {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val date = dateFormat.parse(timestampString)

        // Return the timestamp as milliseconds since a certain epoch
        return date?.time?.toDouble() ?: 0.0
    }
}
