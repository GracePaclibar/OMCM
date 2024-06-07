package com.bscpe.omcmapp

import android.app.Activity
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.Gravity
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.FileProvider
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.github.lzyzsd.circleprogress.DonutProgress
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.time.LocalTime
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private var currentPhotoPath: String = ""

    private val calendar = Calendar.getInstance()
    private val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
    private lateinit var monView: View
    private lateinit var tuesView: View
    private lateinit var wedView: View
    private lateinit var thursView: View
    private lateinit var friView: View
    private lateinit var satView: View
    private lateinit var sunView: View
    private lateinit var temperatureProgress: DonutProgress
    private lateinit var humidityProgress: DonutProgress
    private lateinit var luxTextView: TextView
    private lateinit var valueEventListener: ValueEventListener
    private lateinit var userNameTextView: TextView
    private lateinit var lightLevelTextView: TextView
    private lateinit var latestTimestamp: String
    private lateinit var waterConsumedTextview: TextView
    private var latestTemperature: Double = 0.0
    private var latestHumidity: Double = 0.0
    private var latestLux: Double = 0.0
    private val imageUrls = mutableListOf<String>()
    private lateinit var firebaseDatabase: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var swipeRefresh : SwipeRefreshLayout
    private lateinit var auth: FirebaseAuth
    private lateinit var currentUser: FirebaseUser
    private lateinit var userUid : String
    private val sharedPreferences by lazy {
        getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
    }

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 100
    }

    private val takePictureLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {

                val imageFile = File(currentPhotoPath)
                if (imageFile.exists()) {

                    val imageUri = Uri.fromFile(imageFile)
                    uploadImageToFirebaseStorage(imageUri)
                } else {
                    Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Failed to capture image", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        FirebaseApp.initializeApp(this)

        auth = FirebaseAuth.getInstance()
        firebaseDatabase = FirebaseDatabase.getInstance()
        currentUser = auth.currentUser!!
        userUid = currentUser.uid

        Log.d("MainActivity", "User UID: $userUid")

        val scanTab: ImageButton = findViewById(R.id.scan_tab)

        scanTab.setOnClickListener { view ->
            showScannerPopupMenu(view)
        }

        val analyticsTab : ImageButton = findViewById(R.id.analytics_tab)

        analyticsTab.setOnClickListener { view ->
            showAnalyticsPopupMenu(view)
        }

        showGreeting()
        changeBackground()

        monView = findViewById(R.id.monDay)
        tuesView = findViewById(R.id.tuesDay)
        wedView = findViewById(R.id.wednesDay)
        thursView = findViewById(R.id.thursDay)
        friView = findViewById(R.id.friDay)
        satView = findViewById(R.id.satDay)
        sunView = findViewById(R.id.sunDay)

        val currentDateTextView: TextView = findViewById(R.id.currentDate)
        val currentDate = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(calendar.time)

        currentDateTextView.text = currentDate

        miniCalendar(dayOfWeek)

        temperatureProgress = findViewById(R.id.temperatureProgress)
        humidityProgress = findViewById(R.id.humidityProgress)
        luxTextView = findViewById(R.id.luxValue)
        lightLevelTextView = findViewById(R.id.lightLevel)

        val folderReference = FirebaseStorage.getInstance().getReference().child("images/$userUid")
        val imageContainer = findViewById<LinearLayout>(R.id.imageContainer)

        folderReference.listAll()
            .addOnSuccessListener{ result ->
                val items = result.items.reversed().take(3)
                items.forEach { item ->
                    item.downloadUrl
                        .addOnSuccessListener { uri ->
                            val imageUrl = uri.toString()
                            imageUrls.add(imageUrl)

                            val imageView = ImageView(this)
                            val layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            )
                            layoutParams.width = 300
                            layoutParams.height = layoutParams.width
                            layoutParams.gravity = Gravity.CENTER_VERTICAL
                            imageView.layoutParams = layoutParams
                            imageView.scaleType = ImageView.ScaleType.CENTER_CROP
                            layoutParams.setMargins(5,10,5,0)

                            Picasso.get().load(uri).rotate(90f).into(imageView)

                            if (imageContainer.childCount >= 3) {
                                imageContainer.removeViewAt(imageContainer.childCount - 1)
                            }

                            imageContainer.addView(imageView,0)

                            // open image using gallery app
                            imageView.setOnClickListener {
                                val galleryIntent = Intent(Intent.ACTION_VIEW, uri)
                                galleryIntent.setDataAndType(uri, "image/*")
                                startActivity(galleryIntent)

                            }
                        }
                }
            }

        waterConsumedTextview = findViewById(R.id.waterConsumed_TextView)

        if (userUid != null) {
            databaseReference = firebaseDatabase.reference.child("UsersData").child("$userUid").child("readings")

            valueEventListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var latestReading: Reading? = null
                    for (readingSnapshot in snapshot.children) {
                        latestReading = readingSnapshot.getValue(Reading::class.java)
                    }

                    val timestampTextView = findViewById<TextView>(R.id.timestampTextView)

                    latestReading?.let {
                        latestTemperature = it.internal_temperature.toDoubleOrNull() ?: 0.0
                        latestHumidity = it.internal_humidity.toDoubleOrNull() ?: 0.0
                        latestLux = it.lux.toDoubleOrNull() ?: 0.0
                        latestTimestamp = it.timestamp

                        val (latestDate, latestTime) = parseTimestamp(latestTimestamp)

                        updateProgress()
                        updateWaterConsumption()

                        timestampTextView.text = "As of: $latestDate at $latestTime"
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                }
            }
            databaseReference.addValueEventListener(valueEventListener)
        } else {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
        }

        // for sysConfig preview

        val modeTextView = findViewById<TextView>(R.id.mode_TextView)
        val modePreviewSwitch = findViewById<SwitchCompat>(R.id.modePreview)
        modePreviewSwitch.visibility = View.INVISIBLE

        val waterStateTextView = findViewById<TextView>(R.id.modeText)

        fetchData(userUid, modePreviewSwitch, modeTextView, waterStateTextView)

        swipeRefresh  = findViewById(R.id.swipeRefresh)

        swipeRefresh.setOnRefreshListener {
            updateMethod()
        }
    }

    private fun showScannerPopupMenu(view: View) {

        val popup = PopupMenu(this, view)

        val inflater: MenuInflater = popup.menuInflater
        inflater.inflate(R.menu.pop_up_scanners, popup.menu)

        popup.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                R.id.menu_item1 -> {

                    openCamera(view)
                    true

                }
                R.id.menu_item2 -> {

                    val intent = Intent(this, CameraDetectActivity::class.java)
                    startActivity(intent)
                    true

                }
                R.id.menu_item3 -> {

                    val intent = Intent(this, ImageSelectActivity::class.java)
                    startActivity(intent)
                    true

                }
                else -> false
            }
        }

        popup.show()
    }

    private fun showAnalyticsPopupMenu(view: View) {

        val popup = PopupMenu(this, view)

        val inflater: MenuInflater = popup.menuInflater
        inflater.inflate(R.menu.pop_up_analytics, popup.menu)

        popup.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                R.id.menu_item1 -> {

                    goToTempCharts(view)
                    true
                }
                R.id.menu_item2 -> {

                    goToHumidCharts(view)
                    true
                }
                R.id.menu_item3 -> {

                    goToLightCharts(view)

                    true
                }
                R.id.menu_item4 -> {

                    goToWaterCharts(view)

                    true
                }
                else -> false
            }
        }

        popup.show()
    }

    private fun updateMethod() {
        swipeRefresh.isRefreshing = false
        Handler(Looper.getMainLooper()).postDelayed({
            recreate()
        }, 500)
    }

    private fun fetchData(
        userUid: String?,
        modePreviewSwitch: SwitchCompat,
        modeTextView: TextView,
        waterStateTextView: TextView
    ) {
        val database = FirebaseDatabase.getInstance()
        val dataRef = database.getReference("UsersData/$userUid/Control_Key/dripWater")

        dataRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val isAuto = dataSnapshot.child("isAuto").getValue(Int::class.java) ?: 1
                val isWaterOn = dataSnapshot.child("isWaterOn").getValue(Int::class.java) ?: 0
                val isWaterOnAuto = dataSnapshot.child("isWaterOnAuto").getValue(Int::class.java) ?: 0

                updateStates(isAuto, isWaterOn, isWaterOnAuto, modePreviewSwitch, modeTextView, waterStateTextView)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Error fetching control key data", error.toException())
                Toast.makeText(this@MainActivity, "Error fetching control key data", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateStates(
        isAuto: Int,
        isWaterOn: Int,
        isWaterOnAuto: Int,
        modePreviewSwitch: SwitchCompat,
        modeTextView: TextView,
        waterStateTextView: TextView
    ) {
        if (isAuto == 1) {
            modePreviewSwitch.visibility = View.INVISIBLE
            modeTextView.text = "Automatic"

            if (isWaterOnAuto == 1) {
                waterStateTextView.text = "Water drip is on"
            } else {
                waterStateTextView.text = "Water drip is off"
            }
        } else {
            modePreviewSwitch.visibility = View.VISIBLE
            modeTextView.text = "Manual"
            modePreviewSwitch.isChecked = (isWaterOn == 1)

            modePreviewSwitch.setOnTouchListener { v, event -> true }
            modePreviewSwitch.setFocusable(false)
            modePreviewSwitch.setClickable(false)
        }
    }

    private fun updateWaterConsumption() {
        val waterConsumptionRef = firebaseDatabase.getReference("UsersData/$userUid/readings")

        valueEventListener = waterConsumptionRef
            .limitToLast(1008)
            .addValueEventListener(object : ValueEventListener {
                var lastProcessedDay: Int? = null
                val processedTimestamps = mutableSetOf<String>()
                var totalWaterFlowToday = 0f

                override fun onDataChange(dataSnapshot: DataSnapshot) {

                    Log.d("WaterConsumption", "Data changed")
                    val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

                    for (snapshot in dataSnapshot.children) {
                        for (childSnapshot in snapshot.children) {
                            val waterFlowString = snapshot.child("water_flow").getValue(String::class.java)
                            val waterFlowFloat = waterFlowString?.toFloatOrNull()
                            val waterFlowTimestamp = snapshot.child("timestamp").getValue(String::class.java)

                            if (waterFlowFloat != null && waterFlowTimestamp != null && !processedTimestamps.contains(waterFlowTimestamp)) {
                                processedTimestamps.add(waterFlowTimestamp)
                                val date = dateFormatter.parse(waterFlowTimestamp)
                                val cal = Calendar.getInstance().apply { time = date }
                                val day = cal.get(Calendar.DAY_OF_MONTH)

                                if (lastProcessedDay != null && day != lastProcessedDay) {
                                    totalWaterFlowToday = 0f
                                }

                                totalWaterFlowToday += waterFlowFloat
                                lastProcessedDay = day
                            }
                        }
                    }

                    val formattedTotalWaterFlowToday = String.format("%.2f", totalWaterFlowToday)
                    waterConsumedTextview.text = formattedTotalWaterFlowToday

                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e("WaterConsumption", "Database error: ${databaseError.message}")
                }
            })


    }

    private fun updateProgress() {
        temperatureProgress.progress = latestTemperature.toFloat()
        humidityProgress.progress = latestHumidity.toFloat()

        when {
            latestTemperature < 20 -> temperatureProgress.finishedStrokeColor = getColor(R.color.main) // Cold
            latestTemperature in 20.0..40.0 -> temperatureProgress.finishedStrokeColor = getColor(R.color.midMuted_red) // Normal
            latestTemperature > 40 -> temperatureProgress.finishedStrokeColor = getColor(R.color.muted_red) // Hot
        }

        when {
            latestHumidity < 30 -> humidityProgress.finishedStrokeColor = getColor(R.color.muted_red) // Low Humidity
            latestHumidity in 30.0..60.0 -> humidityProgress.finishedStrokeColor = getColor(R.color.mutedRed_blue) // Normal Humidity
            latestHumidity > 60 -> humidityProgress.finishedStrokeColor = getColor(R.color.muted_blue) // High Humidity
        }


        val lightValueText = if (latestLux > 500) "High" else "Low"
        lightLevelTextView.text = "$lightValueText"

        luxTextView.text = "${latestLux.toInt()}"

        val lightBulbImageView = findViewById<ImageView>(R.id.light_icon)
        if (latestLux > 500) {
            lightBulbImageView.setImageResource(R.drawable.ic_light_on_2)
        } else {
            lightBulbImageView.setImageResource(R.drawable.ic_light_off_2)
        }
    }

    private fun miniCalendar(dayOfWeek: Int) {
        when (dayOfWeek) {
            1 -> sunView.setBackgroundResource(R.drawable.current_day)
            2 -> monView.setBackgroundResource(R.drawable.current_day)
            3 -> tuesView.setBackgroundResource(R.drawable.current_day)
            4 -> wedView.setBackgroundResource(R.drawable.current_day)
            5 -> thursView.setBackgroundResource(R.drawable.current_day)
            6 -> friView.setBackgroundResource(R.drawable.current_day)
            7 -> satView.setBackgroundResource(R.drawable.current_day)
        }
    }

    fun goToTempCharts(view: View) {
        val intent = Intent(this, TempChartsActivity::class.java)

        val options = ActivityOptions.makeCustomAnimation(this,
            R.anim.slide_enter_right,
            R.anim.slide_exit_left
        )

        startActivity(intent, options.toBundle())
    }

    fun goToHumidCharts(view: View) {
        val intent = Intent(this, HumidChartsActivity::class.java)

        val options = ActivityOptions.makeCustomAnimation(this,
            R.anim.slide_enter_right,
            R.anim.slide_exit_left
        )

        startActivity(intent, options.toBundle())
    }

    fun goToLightCharts(view: View) {
        val intent = Intent(this, LightChartsActivity::class.java)

        val options = ActivityOptions.makeCustomAnimation(this,
            R.anim.slide_enter_right,
            R.anim.slide_exit_left
        )

        startActivity(intent, options.toBundle())
    }

    private fun openCamera(view: View) {
        try {
            val captureImageIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            captureImageIntent.resolveActivity(packageManager)?.also {

                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: IOException) {
                    // Error occurred while creating the File
                    Log.e("MainActivity", "Error creating image file: ${ex.message}")
                    null
                }
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        this,
                        "com.bscpe.omcmapp.provider",
                        it
                    )
                    captureImageIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    takePictureLauncher.launch(captureImageIntent)
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error opening camera: ${e.message}")
            e.printStackTrace()
        }
    }

    // create a file for captured image
    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath
        }
    }

    private fun uploadImageToFirebaseStorage(imageUri: Uri) {
        val storageRef = FirebaseStorage.getInstance().reference
        val imageName = "image_${System.currentTimeMillis()}.jpg"
        val imagesRef = storageRef.child("images/$userUid/$imageName")

        val uploadTask = imagesRef.putFile(imageUri)

        uploadTask.addOnSuccessListener { taskSnapshot ->
            Toast.makeText(this, "Image uploaded successfully", Toast.LENGTH_SHORT).show()

            imagesRef.downloadUrl.addOnSuccessListener { uri ->
                saveImagesUrlToSharedPreferences(uri.toString())

            }.addOnFailureListener { exception ->
                Toast.makeText(this, "Failed to get download URL: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { exception ->
            Toast.makeText(this, "Image upload failed: ${exception.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveImagesUrlToSharedPreferences(imageUrl: String)  {
        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)

        val imageCount = sharedPreferences.getInt("imageCount", 0)
        val newImageCount = imageCount + 1

        val editor = sharedPreferences.edit()
        editor.putInt("imageCount", newImageCount)
        editor.putString("imageUrl_$newImageCount", imageUrl)
        editor.apply()
    }

    fun goToWaterCharts(view: View) {
        val intent = Intent(this, WaterFlowActivity::class.java)

        val options = ActivityOptions.makeCustomAnimation(this,
            R.anim.slide_enter_bottom,
            R.anim.slide_exit_top
        )

        startActivity(intent, options.toBundle())
    }

    fun goToProfile(view: View) {
        val intent = Intent(this,ProfileActivity::class.java)

        val options = ActivityOptions.makeCustomAnimation(this,
            R.anim.slide_enter_left, //Enter animation
            R.anim.slide_exit_right //Exit animation
        )

        startActivity(intent, options.toBundle())
    }
    fun goToSettings(view: View) {
        val intent = Intent(this, SettingsActivity::class.java)
        val options = ActivityOptions.makeCustomAnimation(this,
            R.anim.slide_enter_right, //Enter animation
            R.anim.slide_exit_left //Exit animation
        )

        startActivity(intent, options.toBundle())
    }

    fun goToSensors(view: View) {
        val intent = Intent(this, SensorsActivity::class.java)

        val options = ActivityOptions.makeCustomAnimation(this,
            R.anim.slide_enter_bottom,
            R.anim.slide_exit_top
        )

        startActivity(intent, options.toBundle())
    }
    fun openSysConfig(view: View) {
        val intent = Intent(this, SystemConfigActivity::class.java)

        val options = ActivityOptions.makeCustomAnimation(this,
            R.anim.slide_enter_bottom,
            R.anim.slide_exit_top
        )

        startActivity(intent, options.toBundle())
    }
    fun goToImageSelect(view: View) {
        val intent = Intent(this, ImageSelectActivity::class.java)

        val options = ActivityOptions.makeCustomAnimation(this,
            R.anim.slide_enter_bottom,
            R.anim.slide_exit_top
        )

        startActivity(intent, options.toBundle())
    }

    fun showGreeting() {
        val currentTime = LocalTime.now()
        val hour = currentTime.hour

        val greeting = if (hour in 5..11) {
            "Magandang umaga,"

        } else if (hour in 12..17) {
            "Magandang hapon,"
        } else {
            "Magandang gabi,"
        }

        val greetingTextView = findViewById<TextView>(R.id.greetings)
        greetingTextView.text = greeting

        userNameTextView = findViewById(R.id.userName)

        val profileRef = firebaseDatabase.getReference("UsersData/$userUid/Profile")

        profileRef.child("Name").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val userName = dataSnapshot.getValue(String::class.java)

                userName?.let {
                    userNameTextView.text = "$it!"
                }
            }
            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(this@MainActivity, "Failed to retrieve username: ${databaseError.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    fun changeBackground(){
        val timeBackground = findViewById<ImageView>(R.id.lightBackground)
        val currentTime = LocalTime.now()
        val hour = currentTime.hour

        if (hour in 5..17) {
            timeBackground.setImageResource(R.drawable.day_background)
        } else {
            timeBackground.setImageResource(R.drawable.night_background)
        }
    }

    fun parseTimestamp(timestampString: String): Pair<String, String> {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val date = inputFormat.parse(timestampString)

        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH)
        val timeFormat = SimpleDateFormat("hh:mm:ss a")

        val formattedDate = dateFormat.format(date)
        val formattedTime = timeFormat.format(date)

        return Pair(formattedDate, formattedTime)
    }


}
data class Reading(
    val external_humidity: String = "",
    val external_temperature: String = "",
    val internal_temperature: String = "",
    val internal_humidity: String = "",
    val lux: String = "",
    val timestamp: String = "",
    val unixtimestamp: Int = 0
)