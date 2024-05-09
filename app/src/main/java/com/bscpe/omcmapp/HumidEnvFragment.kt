package com.bscpe.omcmapp

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class HumidEnvFragment : Fragment(R.layout.fragment_internal_env) {

    private lateinit var spinner: Spinner
    private lateinit var filter: Array<String>
    private val intHumidValues = mutableListOf<Pair<Float, String?>>()
    private val extHumidValues = mutableListOf<Pair<Float, String?>>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_internal_env, container, false)

        filter = resources.getStringArray(R.array.Filter)
        spinner = view.findViewById(R.id.time_filter)

        if (spinner != null) {
            val adapter = ArrayAdapter(requireContext(), R.layout.spinner_dropdown_item, filter)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter

            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>,
                                            view: View?, position: Int, id: Long) {
                    intHumidValues.clear()
                    extHumidValues.clear()
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

        myRef.limitToLast(limit).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                Log.d("FirebaseData", "Parent Node Key: ${dataSnapshot.key}")

                for (snapshot in dataSnapshot.children) {
                    Log.d("FirebaseData", "Child Node Key: ${snapshot.key}")

                    for (childSnapshot in snapshot.children) {
                        val intHumidityString = snapshot.child("internal_humidity").getValue(String::class.java)
                        val intHumidityFloat = intHumidityString?.toFloatOrNull()
                        val intHumidTimestampString = snapshot.child("timestamp").getValue(String::class.java)

                        val extHumidityString = snapshot.child("external_humidity").getValue(String::class.java)
                        val extHumidityFloat = extHumidityString?.toFloatOrNull()
                        val extHumidTimestampString = snapshot.child("timestamp").getValue(String::class.java)

                        if (intHumidityFloat != null && extHumidityFloat != null) {
                            intHumidValues.add(intHumidityFloat to intHumidTimestampString)
                            extHumidValues.add(extHumidityFloat to extHumidTimestampString)
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
        intHumidValues.sortByDescending { it.first }
        extHumidValues.sortByDescending { it.first }

        if (intHumidValues.isNotEmpty() && extHumidValues.isNotEmpty()) {
            val intTextViewAve = view?.findViewById<TextView>(R.id.int_ave)
            val intTextViewMax = view?.findViewById<TextView>(R.id.int_max)
            val intTextViewMin = view?.findViewById<TextView>(R.id.int_min)
            val intMaxTimeTextView = view?.findViewById<TextView>(R.id.int_max_time)
            val intMinTimeTextView = view?.findViewById<TextView>(R.id.int_min_time)

            val intAverageTemp = String.format("%.2f", intHumidValues.map { it.first }.average())
            val (intMaxTemp, intMaxTimestamp) = intHumidValues.first()
            val (intMinTemp, intMinTimestamp) = intHumidValues.last()

            intTextViewAve?.text = "$intAverageTemp%"
            intTextViewMax?.text = "$intMaxTemp%"
            intTextViewMin?.text = "$intMinTemp%"
            intMaxTimeTextView?.text = getTimeFromTimestamp(intMaxTimestamp)
            intMinTimeTextView?.text = getTimeFromTimestamp(intMinTimestamp)

            val extTextViewAve = view?.findViewById<TextView>(R.id.ext_ave)
            val extTextViewMax = view?.findViewById<TextView>(R.id.ext_max)
            val extTextViewMin = view?.findViewById<TextView>(R.id.ext_min)
            val extMaxTimeTextView = view?.findViewById<TextView>(R.id.ext_max_time)
            val extMinTimeTextView = view?.findViewById<TextView>(R.id.ext_min_time)

            val extAverageTemp = String.format("%.2f", extHumidValues.map { it.first }.average())
            val (extMaxTemp, extMaxTimestamp) = extHumidValues.first()
            val (extMinTemp, extMinTimestamp) = extHumidValues.last()

            extTextViewAve?.text = "$extAverageTemp%"
            extTextViewMax?.text = "$extMaxTemp%"
            extTextViewMin?.text = "$extMinTemp%"
            extMaxTimeTextView?.text = getTimeFromTimestamp(extMaxTimestamp)
            extMinTimeTextView?.text = getTimeFromTimestamp(extMinTimestamp)
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