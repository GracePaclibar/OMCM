package com.bscpe.omcmapp

import android.os.Bundle
import android.view.Gravity
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Locale

class TempTabularDataActivity : AppCompatActivity() {

    private val existingTimestamps = HashSet<String>()
    private var currentPage = 1
    private val rowsPerPage = 15
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tabular_data)

        val tempTable: TableLayout = findViewById(R.id.tempTabularData)

        val database = FirebaseDatabase.getInstance()
        val currentUser = FirebaseAuth.getInstance().currentUser
        val userUid = currentUser?.uid

        val dataRef = database.getReference("UsersData/$userUid/readings")

        dataRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val dateOutputFormatter = SimpleDateFormat("MM/dd/yy : hh:mm a", Locale.getDefault())

                var rowIndex = 0

                for (snapshot in dataSnapshot.children.reversed()) { // Iterate in reverse order
                    val timestampString = snapshot.child("timestamp").getValue(String::class.java)

                    if (timestampString != null && !existingTimestamps.contains(timestampString)) {
                        existingTimestamps.add(timestampString)

                        if (rowIndex >= currentPage * rowsPerPage) {
                            break
                        }

                        if (rowIndex >= (currentPage - 1) * rowsPerPage) {
                            val tableRow = createAndPopulateTableRow(snapshot, dateFormatter, dateOutputFormatter)
                            tempTable.addView(tableRow)
                        }

                        rowIndex++
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        })
    }


    private fun createAndPopulateTableRow(
        snapshot: DataSnapshot,
        dateFormatter: SimpleDateFormat,
        dateOutputFormatter: SimpleDateFormat
    ): TableRow {
        val tableRow = TableRow(this@TempTabularDataActivity).apply {
            layoutParams = TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT)
            setPadding(0, 10, 0, 10)
        }

        val intTemperatureString = snapshot.child("internal_temperature").getValue(String::class.java)
        val extTemperatureString = snapshot.child("external_temperature").getValue(String::class.java)
        val timestampString = snapshot.child("timestamp").getValue(String::class.java)

        val dateTextView = TextView(this@TempTabularDataActivity).apply {
            layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f)
            text = "${dateOutputFormatter.format(dateFormatter.parse(timestampString))}"
            gravity = Gravity.CENTER
        }

        val internalTempTextView = TextView(this@TempTabularDataActivity).apply {
            layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f)
            text = intTemperatureString
            gravity = Gravity.CENTER
        }

        val externalTempTextView = TextView(this@TempTabularDataActivity).apply {
            layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f)
            text = extTemperatureString
            gravity = Gravity.CENTER
        }

        tableRow.addView(dateTextView)
        tableRow.addView(internalTempTextView)
        tableRow.addView(externalTempTextView)

        return tableRow
    }
}