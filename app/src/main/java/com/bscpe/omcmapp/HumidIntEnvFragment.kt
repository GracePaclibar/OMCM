package com.bscpe.omcmapp

import SpinnerModel
import android.content.Context
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
import androidx.fragment.app.activityViewModels
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class HumidIntEnvFragment : Fragment(R.layout.fragment_int_env) {

    private lateinit var spinner: Spinner
    private lateinit var filter: Array<String>
    private val intHumidValues = mutableListOf<Pair<Float, String?>>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_int_env, container, false)

        val sharedViewModel: SpinnerModel by activityViewModels()

        // changing units and icons
        val unit = view.findViewById<TextView>(R.id.unit)
        unit.text = "%"

        val icon = view.findViewById<ImageView>(R.id.temp_icon)
        icon.setImageResource(R.drawable.ic_humid)

        sharedViewModel.selectedPosition.observe(viewLifecycleOwner) { position ->
            spinner.setSelection(position)
        }

        filter = resources.getStringArray(R.array.Filter)
        spinner = view.findViewById(R.id.time_filter)

        if (spinner != null) {
            val adapter = ArrayAdapter(requireContext(), R.layout.spinner_dropdown_item, filter)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter

            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>,
                                            view: View?, position: Int, id: Long) {
                    sharedViewModel.selectedPosition.value = position
                    intHumidValues.clear()
                    val selectedItem = filter[position]
//                    Toast.makeText(parent.context, "Selected item: $selectedItem", Toast.LENGTH_SHORT).show()
                    fillTable(position)

                    // save selection
                    val sharedPreferences = requireActivity().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                    val editor = sharedPreferences.edit()
                    editor.putInt("selectedPosition", position)
                    editor.apply()
                }

                override fun onNothingSelected(parent: AdapterView<*>) {
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
                        val intHumidString = snapshot.child("internal_humidity").getValue(String::class.java)
                        val intHumidFloat = intHumidString?.toFloatOrNull()
                        val intHumidTimestampString = snapshot.child("timestamp").getValue(String::class.java)

                        if (intHumidFloat != null) {
                            intHumidValues.add(intHumidFloat to intHumidTimestampString)
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

        if (intHumidValues.isNotEmpty()) {
            val intTextViewAve = view?.findViewById<TextView>(R.id.int_ave)
            val intTextViewMax = view?.findViewById<TextView>(R.id.int_max)
            val intTextViewMin = view?.findViewById<TextView>(R.id.int_min)
            val intMaxTimeTextView = view?.findViewById<TextView>(R.id.int_max_time)
            val intMinTimeTextView = view?.findViewById<TextView>(R.id.int_min_time)

            val intAverageHumid = String.format("%.2f", intHumidValues.map { it.first }.average())
            val (intMaxHumid, intMaxTimestamp) = intHumidValues.first()
            val (intMinHumid, intMinTimestamp) = intHumidValues.last()

            intTextViewAve?.text = "$intAverageHumid"
            intTextViewMax?.text = "$intMaxHumid%"
            intTextViewMin?.text = "$intMinHumid%"
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