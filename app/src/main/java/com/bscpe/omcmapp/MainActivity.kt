package com.bscpe.omcmapp

import android.app.Activity
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.Gravity
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.github.lzyzsd.circleprogress.DonutProgress
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
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
    private lateinit var databaseReference: DatabaseReference
    private lateinit var valueEventListener: ValueEventListener
    private lateinit var userNameTextView: TextView
    private lateinit var lightLevelTextView: TextView
    private var latestTemperature: Double = 0.0
    private var latestHumidity: Double = 0.0
    private var latestLux: Double = 0.0
    private val imageUrls = mutableListOf<String>()
    private val sharedPreferences by lazy {
        getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
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

        val cameraButton = findViewById<ImageButton>(R.id.scan_tab)

        cameraButton.setOnClickListener {
            openCamera()
        }

        // Find the button by its ID
        val cameraScan = findViewById<ImageButton>(R.id.camera_scan_tab)

        // Set OnClickListener to the button
        cameraScan.setOnClickListener {
            // Create an Intent to start SecondActivity
            val intent = Intent(this, CameraDetectActivity::class.java)

            // Start SecondActivity
            startActivity(intent)
        }

        monView = findViewById(R.id.monDay)
        tuesView = findViewById(R.id.tuesDay)
        wedView = findViewById(R.id.wednesDay)
        thursView = findViewById(R.id.thursDay)
        friView = findViewById(R.id.friDay)
        satView = findViewById(R.id.satDay)
        sunView = findViewById(R.id.sunDay)

        val currentDateTextView: TextView = findViewById(R.id.currentDate)
        val currentDate = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(calendar.time)

        // setting text to textView
        currentDateTextView.text = currentDate

        // set mini calendar color
        miniCalendar(dayOfWeek)

        temperatureProgress = findViewById(R.id.temperatureProgress)
        humidityProgress = findViewById(R.id.humidityProgress)
        luxTextView = findViewById(R.id.luxValue)
        lightLevelTextView = findViewById(R.id.lightLevel)
        userNameTextView = findViewById(R.id.userName)

        // Get the UID of the currently logged-in user
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
        val userUid = FirebaseAuth.getInstance().currentUser?.uid
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

        if (currentUserUid != null) {
            databaseReference = FirebaseDatabase.getInstance().reference.child("UsersData").child(currentUserUid).child("readings")

            valueEventListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var latestReading: Reading? = null
                    for (readingSnapshot in snapshot.children) {
                        latestReading = readingSnapshot.getValue(Reading::class.java)
                    }

                    latestReading?.let {
                        latestTemperature = it.internal_temperature.toDoubleOrNull() ?: 0.0
                        latestHumidity = it.internal_humidity.toDoubleOrNull() ?: 0.0
                        latestLux = it.lux.toDoubleOrNull() ?: 0.0
                        updateProgress()
                        findViewById<TextView>(R.id.timestampTextView).text = "As of: ${it.timestamp}"
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                }
            }

            databaseReference.addValueEventListener(valueEventListener)
        }
    }
//    private fun deleteImage(imageUri: Uri, imageView: ImageView) {
//        val imageContainer = findViewById<LinearLayout>(R.id.imageContainer)
//        val deleteButton = findViewById<Button>(R.id.Delete_btn)
//        val storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(imageUri.toString())
//        storageReference.delete()
//            .addOnSuccessListener {
//            }
//            .addOnFailureListener {
//
//            }
//    }
//    override fun onDestroy() {
//        super.onDestroy()
//        databaseReference.removeEventListener(valueEventListener)
//    }

    private fun updateProgress() {
        temperatureProgress.progress = latestTemperature.toFloat()
        humidityProgress.progress = latestHumidity.toFloat()

        // Show lux as High or Low based on its value
        val lightValueText = if (latestLux > 500) "High" else "Low"
        lightLevelTextView.text = "$lightValueText"

        luxTextView.text = "${latestLux.toInt()}"

        val lightBulbImageView = findViewById<ImageView>(R.id.light_icon)
        if (latestLux > 500) {
            // Lux level is high, use light mode
            lightBulbImageView.setImageResource(R.drawable.ic_light_on)
        } else {
            // Lux level is low, use dark mode
            lightBulbImageView.setImageResource(R.drawable.ic_light_off)
        }
    }

    private fun updateGraph(temperature: Double, humidity: Double, lux: Double) {

        // graph.update(temperature, humidity, lux)
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

    private fun openCamera() {
        val captureImageIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        // Ensure there's a camera activity to handle the intent
        captureImageIntent.resolveActivity(packageManager)?.also {

            val photoFile: File? = try {
                createImageFile()
            } catch (ex: IOException) {
                // Error occurred while creating the File
                null
            }
            // Continue only if the File was successfully created
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
    }

    // create a file for captured image
    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
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
        val userUid = FirebaseAuth.getInstance().currentUser?.uid

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
    fun goToConsumptions(view: View) {
        val intent = Intent(this, wConsumptionsActivity::class.java)

        val options = ActivityOptions.makeCustomAnimation(this,
            R.anim.slide_enter_bottom,
            R.anim.slide_exit_top
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
data class Profile(
    val Name: String = ""
)
