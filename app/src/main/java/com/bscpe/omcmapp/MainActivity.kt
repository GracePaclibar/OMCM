package com.bscpe.omcmapp

import android.Manifest
import android.app.Activity
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private var currentPhotoPath: String = ""

    private val takePictureLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // Image was captured successfully
                val imageFile = File(currentPhotoPath)
                if (imageFile.exists()) {
                    // Upload the image to Firebase Storage
                    val imageUri = Uri.fromFile(imageFile)
                    uploadImageToFirebaseStorage(imageUri)
                } else {
                    Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show()
                }
            } else {
                // Image capture failed or was canceled
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

        // current date view
        val currentDateTextView: TextView = findViewById(R.id.currentDate)

        // getting year and month
        val calendar = Calendar.getInstance()

        val currentDate = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(calendar.time)
        // setting text to textView
        currentDateTextView.text = currentDate

    }

    private fun saveImageToExternalStorage(image: Bitmap): Uri? {
        val imagesDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        imagesDir?.let {
            val imageFile = File(it, "image_${System.currentTimeMillis()}.jpg")

            FileOutputStream(imageFile).use { fos ->
                image.compress(Bitmap.CompressFormat.JPEG, 100, fos)
            }

            return Uri.fromFile(imageFile)
        }
        return null
    }

    private fun saveImageUriToSharedPreferences(imageUri: Uri?) {
        if (imageUri != null) {
            val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)

            // Retrieve the current count of saved images
            val imageCount = sharedPreferences.getInt("imageCount", 0)

            // Increment the count
            val newImageCount = imageCount + 1

            // Save the new count
            val editor = sharedPreferences.edit()
            editor.putInt("imageCount", newImageCount)

            // Save the new image URI
            editor.putString("imageUri_$newImageCount", imageUri.toString())

            // Apply the changes
            editor.apply()
        }
    }

    private fun openCamera() {
        val captureImageIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        // Ensure there's a camera activity to handle the intent
        captureImageIntent.resolveActivity(packageManager)?.also {
            // Create the File where the photo should go
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
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath
        }
    }

    private fun uploadImageToFirebaseStorage(imageUri: Uri) {
        val userUid = FirebaseAuth.getInstance().currentUser?.uid

        val storageRef = FirebaseStorage.getInstance().reference
        val imagesRef = storageRef.child("images/$userUid/${imageUri.lastPathSegment}")

        val uploadTask = imagesRef.putFile(imageUri)

        uploadTask.addOnSuccessListener { taskSnapshot ->
            // Image uploaded successfully
            Toast.makeText(this, "Image uploaded successfully", Toast.LENGTH_SHORT).show()

            imagesRef.downloadUrl.addOnSuccessListener { uri ->

            }.addOnFailureListener { exception ->

                Toast.makeText(this, "Failed to get download URL: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { exception ->

            Toast.makeText(this, "Image upload failed: ${exception.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveImageToFile(imageBitmap: Bitmap): File? {
        val imageFile = File.createTempFile(
            "JPEG_${System.currentTimeMillis()}_",
            ".jpg",
            getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        )
        return try {
            FileOutputStream(imageFile).use { fos ->
                imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
                fos.flush()
            }
            imageFile
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    private fun saveImageUrlToDatabase(imageUrl: String) {
        val userUid = FirebaseAuth.getInstance().currentUser?.uid

        val database = FirebaseDatabase.getInstance()
        val userRef = database.getReference("Users").child("$userUid")
        userRef.child("imageUrl").setValue(imageUrl)
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
