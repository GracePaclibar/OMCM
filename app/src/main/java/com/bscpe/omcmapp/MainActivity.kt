package com.bscpe.omcmapp

import android.app.Activity
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
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

    // Connects activity_main.xml
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        FirebaseApp.initializeApp(this)

       val cameraButton = findViewById<ImageButton>(R.id.scan_tab)

        cameraButton.setOnClickListener {
            openCamera()
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
}
