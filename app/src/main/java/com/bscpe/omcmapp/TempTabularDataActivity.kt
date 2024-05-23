package com.bscpe.omcmapp

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.ScrollView
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
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
    private var currentPage : Int = 1
    private val rowsPerPage = 16
    private var prevScrollY: Int = 0
    private var totalPages : Int = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tabular_data)

        val tempTable: TableLayout = findViewById(R.id.tempTabularData)

        val prevButton: Button = findViewById(R.id.prev_page_btn)
        val nextButton: Button = findViewById(R.id.next_page_btn)

        val database = FirebaseDatabase.getInstance()
        val currentUser = FirebaseAuth.getInstance().currentUser
        val userUid = currentUser?.uid
        val dataRef = database.getReference("UsersData/$userUid/readings")

        nextButton.setOnClickListener {
            currentPage++
            goToNextPage(dataRef, currentPage, tempTable)
            updatePageNumber(currentPage, dataRef)
        }

        prevButton.setOnClickListener {
            if (currentPage > 1){
                currentPage--
            }
            goToPrevPage(dataRef, currentPage, tempTable)
            updatePageNumber(currentPage, dataRef)
        }

        fetchDataAndUpdateUI(dataRef, currentPage, tempTable)
        updatePageNumber(currentPage, dataRef)
    }

    private fun updatePageNumber(currentPage: Int, dataRef: DatabaseReference){
        countChildren(dataRef)
        val currentPageTextView: TextView = findViewById(R.id.currentPageNo)

        currentPageTextView.text = "Page $currentPage / $totalPages"
    }

    private fun countChildren(dataRef: DatabaseReference) {
        dataRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val childCount = dataSnapshot.childrenCount
                totalPages = calculateTotalPages(childCount, rowsPerPage)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                println("Error: ${databaseError.message}")
            }
        })
    }

    private fun calculateTotalPages(childCount: Long, rowsPerPage: Int): Int {
        val fullPages: Int = (childCount / rowsPerPage).toInt()
        val remainingRows: Int = if (childCount % rowsPerPage > 0) 1 else 0
        return fullPages + remainingRows
    }

    private fun clearTableLayout(tableLayout: TableLayout) {
        tableLayout.removeAllViews()
    }

    private fun goToNextPage(dataRef: DatabaseReference, currentPage: Int, tempTable: TableLayout) {
        Toast.makeText(this@TempTabularDataActivity, "Next Page Turned", Toast.LENGTH_SHORT).show()
        fetchDataAndUpdateUI(dataRef, currentPage, tempTable)
        updateButtons(currentPage)
        scrollToLastPage()
    }

    private fun goToPrevPage(dataRef: DatabaseReference, currentPage: Int, tempTable: TableLayout) {
        Toast.makeText(this@TempTabularDataActivity, "Previous Page Turned", Toast.LENGTH_SHORT).show()

        val prevPage = currentPage - 1
        if (prevPage >= 1) {
            fetchDataAndUpdateUI(dataRef, currentPage, tempTable)
            updateButtons(currentPage)
            restoreScrollPosition()
        }
    }

    private fun restoreScrollPosition() {
        val scrollView: ScrollView = findViewById(R.id.scroll_layout)
        scrollView.post {
            scrollView.scrollTo(0, prevScrollY)
        }
    }

    private fun scrollToLastPage() {
        val scrollPage: ScrollView = findViewById(R.id.scroll_layout)
        scrollPage.postDelayed({
            scrollPage.fullScroll(ScrollView.FOCUS_DOWN)
        }, 50)
    }

    private fun fetchDataAndUpdateUI(dataRef: DatabaseReference, currentPage: Int, tempTable: TableLayout) {
        updateButtons(currentPage)
        clearTableLayout(tempTable)

        dataRef.removeEventListener(dataListener)
        dataRef.addValueEventListener(dataListener)
    }

    private fun updateButtons(currentPage: Int) {
        val prevButton: Button = findViewById(R.id.prev_page_btn)

        if (currentPage == 1) {
            prevButton.visibility = View.INVISIBLE
        } else {
            prevButton.visibility = View.VISIBLE
        }
    }

    private val dataListener = object : ValueEventListener {
        override fun onDataChange(dataSnapshot: DataSnapshot) {
            val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val timeOutputFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val dateOutputFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

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
                        val tableRow = createAndPopulateTableRow(snapshot, dateFormatter, dateOutputFormatter, timeOutputFormatter)

                        if (rowIndex % 2 == 0) {
                            tableRow.setBackgroundColor(ContextCompat.getColor(this@TempTabularDataActivity, R.color.white))
                        } else {
                            tableRow.setBackgroundColor(ContextCompat.getColor(this@TempTabularDataActivity, R.color.detail))
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
        dateOutputFormatter: SimpleDateFormat,
        timeOutputFormatter: SimpleDateFormat
    ): TableRow {
        val tableRow = TableRow(this@TempTabularDataActivity).apply {
            layoutParams = TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT)
            setPadding(0, 10, 5, 10)
        }

        val intTemperatureString = snapshot.child("internal_temperature").getValue(String::class.java)
        val extTemperatureString = snapshot.child("external_temperature").getValue(String::class.java)
        val timestampString = snapshot.child("timestamp").getValue(String::class.java)

        val dateTextView = TextView(this@TempTabularDataActivity).apply {
            layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1.25f)
            text = "${dateOutputFormatter.format(dateFormatter.parse(timestampString))}"
            gravity = Gravity.CENTER
            setPadding(0, 15, 0, 15)
        }

        val timeTextView = TextView(this@TempTabularDataActivity).apply{
            layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f)
            text = "${timeOutputFormatter.format(dateFormatter.parse(timestampString))}"
            gravity = Gravity.CENTER
            setPadding(0, 15, 0, 15)
        }

        val internalTempTextView = TextView(this@TempTabularDataActivity).apply {
            layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 0.75f)
            text = intTemperatureString
            gravity = Gravity.CENTER
            setPadding(0, 15, 0, 15)
        }

        val externalTempTextView = TextView(this@TempTabularDataActivity).apply {
            layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 0.75f)
            text = extTemperatureString
            gravity = Gravity.CENTER
            setPadding(0, 15, 0, 15)
        }

        tableRow.addView(dateTextView)
        tableRow.addView(timeTextView)
        tableRow.addView(internalTempTextView)
        tableRow.addView(externalTempTextView)

        return tableRow
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