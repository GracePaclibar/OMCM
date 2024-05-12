package com.bscpe.omcmapp

import SpinnerModel
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

class HumidExtEnvFragment : Fragment(R.layout.fragment_temp_ext_env) {

    private lateinit var spinner: Spinner
    private lateinit var filter: Array<String>
    private val extHumidValues = mutableListOf<Pair<Float, String?>>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_temp_ext_env, container, false)

        filter = resources.getStringArray(R.array.Filter)
        spinner = view.findViewById(R.id.time_filter)

        val sharedViewModel: SpinnerModel by activityViewModels()

        // changing units and icons
        val unit = view.findViewById<TextView>(R.id.unit)
        unit.text = "%"

        val icon = view.findViewById<ImageView>(R.id.temp_icon)
        icon.setImageResource(R.drawable.ic_humid)

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

                for (snapshot in dataSnapshot.children) {

                    for (childSnapshot in snapshot.children) {
                        val extHumidString = snapshot.child("external_humidity").getValue(String::class.java)
                        val extHumidFloat = extHumidString?.toFloatOrNull()
                        val extHumidTimestampString = snapshot.child("timestamp").getValue(String::class.java)

                        if (extHumidFloat != null) {
                            extHumidValues.add(extHumidFloat to extHumidTimestampString)
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
        extHumidValues.sortByDescending { it.first }

        if (extHumidValues.isNotEmpty()) {
            val extTextViewAve = view?.findViewById<TextView>(R.id.ext_ave)
            val extTextViewMax = view?.findViewById<TextView>(R.id.ext_max)
            val extTextViewMin = view?.findViewById<TextView>(R.id.ext_min)
//            val extMaxTimeTextView = view?.findViewById<TextView>(R.id.ext_max_time)
//            val extMinTimeTextView = view?.findViewById<TextView>(R.id.ext_min_time)

            val extAverageHumid = String.format("%.2f", extHumidValues.map { it.first }.average())
            val (extMaxHumid, extMaxTimestamp) = extHumidValues.first()
            val (extMinHumid, extMinTimestamp) = extHumidValues.last()

            extTextViewAve?.text = "$extAverageHumid"
            extTextViewMax?.text = "$extMaxHumid%"
            extTextViewMin?.text = "$extMinHumid%"
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