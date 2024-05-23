package com.bscpe.omcmapp

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class WaterEnvFragment : Fragment(R.layout.fragment_int_env) {

    private lateinit var spinner: Spinner
    private lateinit var filter: Array<String>
    private val waterValues = mutableListOf<Pair<Int, String?>>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_int_env, container, false)

        filter = resources.getStringArray(R.array.Water_Filter)
        spinner = view.findViewById(R.id.time_filter)

        val title = view.findViewById<TextView>(R.id.internal_text)
        title.text = "Usage Data"

        val unit = view.findViewById<TextView>(R.id.unit)
        unit.text = "L"

        val icon = view.findViewById<ImageView>(R.id.temp_icon)
        icon.setImageResource(R.drawable.ic_humid)

        if (spinner != null) {
            val adapter = ArrayAdapter(requireContext(), R.layout.spinner_dropdown_item, filter)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter

            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    waterValues.clear()
                    val selectedItem = filter[position]
                    fillTable(position)
                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                    TODO()
                }
            }
        }
        return view
    }

    private fun fillTable(selectedFilterPosition: Int) {
        when (selectedFilterPosition) {
            0 -> {
                fetchDataFromFB(1008)
            }
            1 -> {
                fetchDataFromFB(4320)
            }
            2 -> {
                fetchDataFromFB(12960)
            }
            3 -> {
                fetchDataFromFB(25920)
            }
            4 -> {
                fetchDataFromFB(52560)
            }
            else -> {
                fetchDataFromFB(1008)
                Toast.makeText(requireContext(), "Error", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchDataFromFB(limit: Int) {
        val database = FirebaseDatabase.getInstance()
        val currentUser = FirebaseAuth.getInstance().currentUser
        val userUid = currentUser?.uid

        val myRef = database.getReference("UsersData/$userUid/readings")

        myRef.limitToLast(limit).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                Log.d("FirebaseData", "Parent Node Key: ${dataSnapshot.key}")

                for (snapshot in dataSnapshot.children) {
                    Log.d("FirebaseData", "Child Node Key: ${snapshot.key}")

                    for (childSnapshot in snapshot.children) {
                        val waterString = snapshot.child("water_flow").getValue(String::class.java)
                        val waterInt = waterString?.toIntOrNull()
                        val waterTimestampString = snapshot.child("timestamp").getValue(String::class.java)

                        if (waterInt != null) {
                            waterValues.add(waterInt to waterTimestampString)
                        }
                    }
                    setupTable()
                }
            }
            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("LinechartsFragment", "Database error: ${databaseError.message}")
            }
        })
    }

    private fun setupTable() {
        waterValues.sortByDescending { it.first }

        if (waterValues.isNotEmpty()) {
            val intTextViewAve = view?.findViewById<TextView>(R.id.int_ave)
            val intTextViewMax = view?.findViewById<TextView>(R.id.int_max)
            val intTextViewMin = view?.findViewById<TextView>(R.id.int_min)
            val intMaxTimeTextView = view?.findViewById<TextView>(R.id.int_max_time)
            val intMinTimeTextView = view?.findViewById<TextView>(R.id.int_min_time)

            val intAverageWater = waterValues.map { it.first }.average().toInt().toString()
            val (intMaxWater, intMaxTimestamp) = waterValues.first()
            val (intMinWater, intMinTimestamp) = waterValues.last()

            intTextViewAve?.text = intAverageWater
            intTextViewMax?.text = "$intMaxWater L"
            intTextViewMin?.text = "$intMinWater L"
            intMaxTimeTextView?.text = getTimeFromTimestamp(intMaxTimestamp)
            intMinTimeTextView?.text = getTimeFromTimestamp(intMinTimestamp)

        }
    }

    private fun getTimeFromTimestamp(timestamp: String?): String {
        if (timestamp != null) {
            val timeParts = timestamp.split(" ")[1].split(":")
            val hours = timeParts[0].toInt()
            val minutes = timeParts[1]
            val period = if (hours < 12) "AM" else "PM"
            val adjustedHours = if (hours == 0 || hours == 12) 12 else hours % 12
            return String.format("%d:%s %s", adjustedHours, minutes, period)
        }
        return ""
    }
}