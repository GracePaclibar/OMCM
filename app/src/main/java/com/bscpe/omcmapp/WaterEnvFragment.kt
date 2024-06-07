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
import java.text.SimpleDateFormat
import java.util.Locale

class WaterEnvFragment : Fragment(R.layout.fragment_water_env) {

    private lateinit var spinner: Spinner
    private lateinit var filter: Array<String>
    private val waterValues = mutableListOf<Pair<Float, String?>>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_water_env, container, false)

        filter = resources.getStringArray(R.array.Water_Filter)
        spinner = view.findViewById(R.id.time_filter)

        val unit = view.findViewById<TextView>(R.id.unit)
        unit.text = "L"

        val unitTotal = view.findViewById<TextView>(R.id.unit_total)
        unitTotal.text = "L"

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

        myRef.limitToLast(limit).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {

                val processedTimestamps = mutableSetOf<String>()

                for (snapshot in dataSnapshot.children) {
                    for (childSnapshot in snapshot.children) {
                        val waterString = snapshot.child("water_flow").getValue(String::class.java)
                        val waterFloat = waterString?.toFloatOrNull()
                        val waterTimestampString = snapshot.child("timestamp").getValue(String::class.java)

                        if (waterFloat != null
                            && waterTimestampString != null
                            && !processedTimestamps.contains(waterTimestampString)) {
                            waterValues.add(waterFloat to waterTimestampString)
                            processedTimestamps.add(waterTimestampString)
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
        val waterValuesByDate = mutableMapOf<String, Float>()

        for ((value, timestamp) in waterValues) {
            if (timestamp != null) {
                val date = timestamp.split(" ")[0]
                waterValuesByDate[date] = waterValuesByDate.getOrDefault(date, 0f) + value
            }
        }

        val maxEntry = waterValuesByDate.maxByOrNull { it.value }
        val minEntry = waterValuesByDate.minByOrNull { it.value }

        if (waterValues.isNotEmpty()) {
            val intTextViewAve = view?.findViewById<TextView>(R.id.int_ave)
            val intTextViewTotal = view?.findViewById<TextView>(R.id.int_total)
            val intTextViewMax = view?.findViewById<TextView>(R.id.int_max)
            val intTextViewMin = view?.findViewById<TextView>(R.id.int_min)
            val intMaxTimeTextView = view?.findViewById<TextView>(R.id.int_max_time)
            val intMinTimeTextView = view?.findViewById<TextView>(R.id.int_min_time)

            val intAverageWater = String.format("%.2f", waterValues.map { it.first }.average())
            val intTotalWater = String.format("%.2f", waterValues.map { it.first }.sum())

            val maxTotalWater = String.format("%.2f", maxEntry?.value ?: 0f)
            val maxDate = maxEntry?.key ?: ""
            val minTotalWater = String.format("%.2f", minEntry?.value ?: 0f)
            val minDate = minEntry?.key ?: ""

            intTextViewAve?.text = intAverageWater
            intTextViewTotal?.text = intTotalWater
            intTextViewMax?.text = "$maxTotalWater L"
            intTextViewMin?.text = "$minTotalWater L"
            intMaxTimeTextView?.text = getDateFromTimestamp(maxDate)
            intMinTimeTextView?.text = getDateFromTimestamp(minDate)
        }
    }

    private fun getDateFromTimestamp(timestamp: String?): String {
        if (timestamp != null) {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = dateFormat.parse(timestamp.split(" ")[0])
            val formattedDate = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
            return formattedDate.format(date)
        }
        return ""
    }
}