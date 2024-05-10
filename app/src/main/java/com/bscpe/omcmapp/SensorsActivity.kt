package com.bscpe.omcmapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import android.view.View
import android.widget.Toast
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth

class SensorsActivity : AppCompatActivity() {

    private lateinit var sensorsRecyclerView: RecyclerView
    private lateinit var tvLoadingData: TextView
    private lateinit var sensorsList: ArrayList<SensorsModel>
    private lateinit var dbRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sensors)
        FirebaseApp.initializeApp(this)
        sensorsRecyclerView = findViewById(R.id.rvSensors)
        sensorsRecyclerView.layoutManager = LinearLayoutManager(this)
        sensorsRecyclerView.setHasFixedSize(true)
        tvLoadingData = findViewById(R.id.tvLoadingData)

        sensorsList = arrayListOf<SensorsModel>()

        getSensorsData()

    }

    private fun getSensorsData() {
        sensorsRecyclerView.visibility = View.GONE
        tvLoadingData.visibility = View.VISIBLE
        val currentUser = FirebaseAuth.getInstance().currentUser
        dbRef =
            FirebaseDatabase.getInstance().getReference("UsersData/${currentUser?.uid}/readings")

        // Modify the query to order data by timestamp in descending order
        dbRef.orderByChild("timestamp").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                sensorsList.clear()
                if (snapshot.exists()) {
                    val reversedList = snapshot.children.reversed() // Reverse the order of children
                    for (empSnap in reversedList) {
                        val empData = empSnap.getValue(SensorsModel::class.java)
                        empData?.let { sensorsList.add(it) }
                    }
                    val mAdapter = SensorsAdapter(sensorsList)
                    sensorsRecyclerView.adapter = mAdapter

                    mAdapter.setOnItemClickListener(object : SensorsAdapter.onItemClickListener {
                        override fun onItemClick(position: Int) {
                            val intent =
                                Intent(this@SensorsActivity, SensorsDetailsActivity::class.java)
                            //put extras
                            intent.putExtra("temperature", sensorsList[position].temperature)
                            intent.putExtra("humidity", sensorsList[position].humidity)
                            intent.putExtra("lux", sensorsList[position].lux)
                            intent.putExtra("timestamp", sensorsList[position].timestamp)
                            startActivity(intent)
                        }
                    })

                    sensorsRecyclerView.visibility = View.VISIBLE
                    tvLoadingData.visibility = View.GONE
                } else {
                    // Handle case when no data is available
                    // For example, show a toast or a message to the user
                    Toast.makeText(this@SensorsActivity, "No data available", Toast.LENGTH_SHORT)
                        .show()
                    sensorsRecyclerView.visibility = View.GONE
                    tvLoadingData.visibility = View.VISIBLE
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle onCancelled
                // For example, show an error message
                Toast.makeText(
                    this@SensorsActivity,
                    "Failed to retrieve data: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
                sensorsRecyclerView.visibility = View.GONE
                tvLoadingData.visibility = View.VISIBLE
            }
        })
    }
}
