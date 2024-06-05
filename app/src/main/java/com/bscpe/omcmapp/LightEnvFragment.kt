package com.bscpe.omcmapp

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class LightEnvFragment : Fragment(R.layout.fragment_int_env) {

    private lateinit var spinner: Spinner
    private lateinit var filter: Array<String>
    private val intLightValues = mutableListOf<Pair<Int, String?>>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_int_env, container, false)

        filter = resources.getStringArray(R.array.Filter)
        spinner = view.findViewById(R.id.time_filter)

        // changing units and icons
        val unit = view.findViewById<TextView>(R.id.unit)
        unit.text = "lux"

        val icon = view.findViewById<ImageView>(R.id.temp_icon)
        icon.setImageResource(R.drawable.ic_light)

        if (spinner != null) {
            val adapter = ArrayAdapter(requireContext(), R.layout.spinner_dropdown_item, filter)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter

            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    intLightValues.clear()
                    val selectedItem = filter[position]
//                    Toast.makeText(parent.context, "Selected item: $selectedItem", Toast.LENGTH_SHORT).show()
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
                fetchDataFromFB(114)
            }
            1 -> {
                fetchDataFromFB(1008)
            }
            2 -> {
                fetchDataFromFB(4320)
            }
            3 -> {
                fetchDataFromFB(12960)
            }
            4 -> {
                fetchDataFromFB(25920)
            }
            5 -> {
                fetchDataFromFB(52560)
            }
            else -> {
                fetchDataFromFB(114)
                Toast.makeText(requireContext(), "Error", Toast.LENGTH_SHORT).show()
            }
        }

    }

    private fun fetchDataFromFB(limit: Int) {
        val database = FirebaseDatabase.getInstance()
        val currentUser = FirebaseAuth.getInstance().currentUser
        val userUid = currentUser?.uid

        val myRef = database.getReference("UsersData/$userUid/readings")

        myRef.limitToLast(limit).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {

                val processedTimestamps = mutableSetOf<String>()

                for (snapshot in dataSnapshot.children) {
                    for (childSnapshot in snapshot.children) {
                        val intLightString = snapshot.child("lux").getValue(String::class.java)
                        val intLightInt = intLightString?.toIntOrNull()
                        val intLightTimestampString = snapshot.child("timestamp").getValue(String::class.java)

                        if (intLightInt != null
                            && intLightTimestampString != null
                            && !processedTimestamps.contains(intLightTimestampString)) {

                            intLightValues.add(intLightInt to intLightTimestampString)
                            processedTimestamps.add(intLightTimestampString)
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
        intLightValues.sortByDescending { it.first }

        if (intLightValues.isNotEmpty()) {
            val intTextViewAve = view?.findViewById<TextView>(R.id.int_ave)
            val intTextViewMax = view?.findViewById<TextView>(R.id.int_max)
            val intTextViewMin = view?.findViewById<TextView>(R.id.int_min)
            val intMaxTimeTextView = view?.findViewById<TextView>(R.id.int_max_time)
            val intMinTimeTextView = view?.findViewById<TextView>(R.id.int_min_time)

            val intAverageLight = intLightValues.map { it.first }.average().toInt().toString()
            val (intMaxLight, intMaxTimestamp) = intLightValues.first()
            val (intMinLight, intMinTimestamp) = intLightValues.last()

            intTextViewAve?.text = intAverageLight
            intTextViewMax?.text = "$intMaxLight lux"
            intTextViewMin?.text = "$intMinLight lux"
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