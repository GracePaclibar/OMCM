package com.bscpe.omcmapp

import SpinnerModel
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
import androidx.fragment.app.activityViewModels
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class TempExtEnvFragment : Fragment(R.layout.fragment_ext_env) {

    private lateinit var spinner: Spinner
    private lateinit var filter: Array<String>
    private val extTemperatureValues = mutableListOf<Pair<Float, String?>>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_ext_env, container, false)

        filter = resources.getStringArray(R.array.Filter)
        spinner = view.findViewById(R.id.time_filter)

        val sharedViewModel: SpinnerModel by activityViewModels()

        sharedViewModel.selectedPosition.observe(viewLifecycleOwner) { position ->
            spinner.setSelection(position)
        }

        if (spinner != null) {
            val adapter = ArrayAdapter(requireContext(), R.layout.spinner_dropdown_item, filter)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter

            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>,
                                            view: View?, position: Int, id: Long) {
                    sharedViewModel.selectedPosition.value = position
                    extTemperatureValues.clear()
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

                for (snapshot in dataSnapshot.children) {

                    for (childSnapshot in snapshot.children) {
                        val extTemperatureString = snapshot.child("external_temperature").getValue(String::class.java)
                        val extTemperatureFloat = extTemperatureString?.toFloatOrNull()
                        val extTempTimestampString = snapshot.child("timestamp").getValue(String::class.java)

                        if (extTemperatureFloat != null) {
                            extTemperatureValues.add(extTemperatureFloat to extTempTimestampString)
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
        extTemperatureValues.sortByDescending { it.first }

        if (extTemperatureValues.isNotEmpty()) {
            val extTextViewAve = view?.findViewById<TextView>(R.id.ext_ave)
            val extTextViewMax = view?.findViewById<TextView>(R.id.ext_max)
            val extTextViewMin = view?.findViewById<TextView>(R.id.ext_min)
//            val extMaxTimeTextView = view?.findViewById<TextView>(R.id.ext_max_time)
//            val extMinTimeTextView = view?.findViewById<TextView>(R.id.ext_min_time)

            val extAverageTemp = String.format("%.2f", extTemperatureValues.map { it.first }.average())
            val (extMaxTemp, extMaxTimestamp) = extTemperatureValues.first()
            val (extMinTemp, extMinTimestamp) = extTemperatureValues.last()

            extTextViewAve?.text = "$extAverageTemp"
            extTextViewMax?.text = "$extMaxTemp°C"
            extTextViewMin?.text = "$extMinTemp°C"
//            extMaxTimeTextView?.text = getTimeFromTimestamp(extMaxTimestamp)
//            extMinTimeTextView?.text = getTimeFromTimestamp(extMinTimestamp)
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