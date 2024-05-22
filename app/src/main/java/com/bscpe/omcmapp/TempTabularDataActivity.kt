package com.bscpe.omcmapp

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Locale

class TempTabularDataActivity : AppCompatActivity() {

    private val existingTimestamps = HashSet<String>()
    private var currentPage = 1
    private val rowsPerPage = 20
    private lateinit var gestureDetector: GestureDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tabular_data)

        val tempTable: TableLayout = findViewById(R.id.tempTabularData)

        val database = FirebaseDatabase.getInstance()
        val currentUser = FirebaseAuth.getInstance().currentUser
        val userUid = currentUser?.uid

        val dataRef = database.getReference("UsersData/$userUid/readings")

        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                val deltaX = e2?.x?.minus(e1?.x ?: 0f) ?: 0f
                if (deltaX < 0) { // Swipe to the left
                    currentPage++
                    fetchDataAndUpdateUI(dataRef)
                }
                return true
            }
        })

        tempTable.setOnTouchListener { _, event -> gestureDetector.onTouchEvent(event) }

        fetchDataAndUpdateUI(dataRef)
    }

    private fun fetchDataAndUpdateUI(dataRef: DatabaseReference) {
        dataRef.removeEventListener(dataListener)
        dataRef.addValueEventListener(dataListener)
    }

    private val dataListener = object : ValueEventListener {
        override fun onDataChange(dataSnapshot: DataSnapshot) {
            val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val dateOutputFormatter = SimpleDateFormat("MM/dd/yy : hh:mm a", Locale.getDefault())

            val tempTable: TableLayout = findViewById(R.id.tempTabularData)

            var rowIndex = 0

            for (snapshot in dataSnapshot.children.reversed()) {
                val timestampString = snapshot.child("timestamp").getValue(String::class.java)

                if (timestampString != null && !existingTimestamps.contains(timestampString)) {
                    existingTimestamps.add(timestampString)

                    if (rowIndex >= currentPage * rowsPerPage) {
                        break
                    }

                    if (rowIndex >= (currentPage - 1) * rowsPerPage) {
                        val tableRow = createAndPopulateTableRow(snapshot, dateFormatter, dateOutputFormatter)

                        if (rowIndex % 2 == 0) {
                            tableRow.setBackgroundColor(ContextCompat.getColor(this@TempTabularDataActivity, R.color.detail))
                        } else {
                        }
                        tempTable.addView(tableRow)
                    }

                    rowIndex++
                }
            }
        }

        override fun onCancelled(error: DatabaseError) {
            TODO("Not yet implemented")
        }
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
            layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT, 1f)
            text = "${dateOutputFormatter.format(dateFormatter.parse(timestampString))}"
            gravity = Gravity.CENTER
            setPadding(0, 10, 0, 10)
        }

        val internalTempTextView = TextView(this@TempTabularDataActivity).apply {
            layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT, 1f)
            text = intTemperatureString
            gravity = Gravity.CENTER
            setPadding(0, 10, 0, 10)
        }

        val externalTempTextView = TextView(this@TempTabularDataActivity).apply {
            layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT, 1f)
            text = extTemperatureString
            gravity = Gravity.CENTER
            setPadding(0, 10, 0, 10)
        }

        tableRow.addView(dateTextView)
        tableRow.addView(internalTempTextView)
        tableRow.addView(externalTempTextView)

        return tableRow
    }
    override fun onTouchEvent(event: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event)
    }

    fun goToMain(view : View) {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)

        val options = ActivityOptions.makeCustomAnimation(this,
            R.anim.slide_enter_left,
            R.anim.slide_exit_right
        )
        startActivity(intent, options.toBundle())
    }
}