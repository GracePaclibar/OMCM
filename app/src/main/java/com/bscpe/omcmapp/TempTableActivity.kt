package com.bscpe.omcmapp

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.HandlerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Locale

class TempTableActivity : AppCompatActivity() {
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var readingsList: MutableList<Readings>
    private lateinit var adapter: ReadingsAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var prevButton: Button
    private lateinit var nextButton: Button
    private lateinit var loadingIndicator: ProgressBar
    private lateinit var pageNoTextView: TextView
    private lateinit var titles: LinearLayout
    private var currentPage = 1
    private val itemsPerPage = 15
    private var handler: Handler? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_temp_table)

        titles = findViewById(R.id.titles)
        pageNoTextView = findViewById(R.id.pageNo)

        loadingIndicator = findViewById(R.id.loadingIndicator)

        recyclerView = findViewById(R.id.recyclerView)
        prevButton = findViewById(R.id.prevButton)
        nextButton = findViewById(R.id.nextButton)

        recyclerView.layoutManager = LinearLayoutManager(this)
        readingsList = mutableListOf()
        adapter = ReadingsAdapter(readingsList)
        recyclerView.adapter = adapter

        database = FirebaseDatabase.getInstance().reference
        auth = FirebaseAuth.getInstance()

        handler = HandlerCompat.createAsync(mainLooper)

        fetchReadings()

        nextButton = findViewById(R.id.nextButton)
        nextButton.setOnClickListener {
            currentPage++
            updateRecyclerViewWithDelay(handler, 200)
            updatePageNumber()
        }

        prevButton = findViewById(R.id.prevButton)

        prevButton.setOnClickListener {
            if(currentPage > 1) {
                currentPage--
                updateRecyclerViewWithDelay(handler, 200)
                updatePageNumber()
            }
        }

        if (currentPage == 1) {
            prevButton.visibility = View.INVISIBLE
        }
    }

    private fun fetchReadings() {
        val userUid = auth.currentUser?.uid
        val readingsRef = database.child("UsersData").child("$userUid").child("readings")

        titles.visibility = View.GONE
        loadingIndicator.visibility = View.VISIBLE

        readingsRef.addValueEventListener(object : ValueEventListener{
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                readingsList.clear()
                for (readingSnapshot in dataSnapshot.children) {
                    val timestamp = readingSnapshot.child("timestamp").getValue(String::class.java) ?: ""
                    val internalTempString = readingSnapshot.child("internal_temperature").getValue(String::class.java) ?: " "
                    val internalTempFloat = internalTempString.toFloatOrNull()
                    val externalTempString = readingSnapshot.child("external_temperature").getValue(String::class.java) ?: " "
                    val externalTempFloat = externalTempString.toFloatOrNull()

                    val (date, time) = parseTimestamp(timestamp)

                    if (internalTempFloat != null && externalTempFloat != null) {
                        val readings = Readings(
                            date = date,
                            time = time,
                            internalTemperature = internalTempFloat,
                            externalTemperature = externalTempFloat
                        )
                        titles.visibility = View.VISIBLE
                        loadingIndicator.visibility = View.GONE
                        updatePageNumber()

                        readingsList.add(readings)
                    }
                }
                readingsList.sortWith(compareByDescending<Readings>{ it.date }
                    .thenByDescending { it.time })

                updateRecyclerViewWithDelay(handler, 200)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("TempTableActivity", "Failed to read data: ${error.toException()}")
            }
        })
    }

    private fun parseTimestamp(timestamp: String): Pair<String, String> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val date = dateFormat.parse(timestamp)

        val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val timeFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())

        return dateFormatter.format(date) to timeFormatter.format(date)
    }

    private fun updateRecyclerViewWithDelay(handler: Handler?, delayMillis: Long) {

        handler?.postDelayed({
            val startIndex = (currentPage - 1) * itemsPerPage
            val endIndex = kotlin.math.min(startIndex + itemsPerPage, readingsList.size)
            val paginatedList = readingsList.subList(startIndex, endIndex)
            adapter.updateList(paginatedList)

            if (currentPage == 1) {
                prevButton.visibility = View.INVISIBLE
            } else {
                prevButton.visibility = View.VISIBLE
            }

        }, delayMillis)
    }

    private fun updatePageNumber() {
        val totalPages = (readingsList.size + itemsPerPage - 1) / itemsPerPage
        pageNoTextView.text = "Page $currentPage / $totalPages"
    }

    fun goToMain(view: View) {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)

        val options = ActivityOptions.makeCustomAnimation(this,
            R.anim.slide_enter_left,
            R.anim.slide_exit_right
        )
        startActivity(intent, options.toBundle())
    }
}