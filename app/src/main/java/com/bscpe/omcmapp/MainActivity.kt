package com.bscpe.omcmapp

import android.Manifest
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
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private val CAMERA_PERMISSION_REQUEST_CODE = 1001

    private val takePictureLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val imageBitmap = result.data?.extras?.get("data") as Bitmap?
                if (imageBitmap != null) {
                    // Save the image to external storage
                    val imageUri = saveImageToExternalStorage(imageBitmap)

                    // Save the image URI to SharedPreferences
                    saveImageUriToSharedPreferences(imageUri)

                    // Pass the image URI to the second activity
                    val intent = Intent(this, ProfileActivity::class.java)
                    intent.putExtra("imageUri", imageUri.toString())
                    startActivity(intent)
                }
            }
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

    // Connects activity_main.xml
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val cameraButton = findViewById<ImageButton>(R.id.scan_tab)

        cameraButton.setOnClickListener {
            openCamera()
        }
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

    private fun openCamera() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // The CAMERA permission is already granted, proceed with opening the camera.
            val captureImageIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (captureImageIntent.resolveActivity(packageManager) != null) {
                takePictureLauncher.launch(captureImageIntent)
            } else {
                // Handle the case where no camera app is available
                // You can display a message or take alternative actions
                Toast.makeText(this, "it didnt work", Toast.LENGTH_SHORT).show()
            }
        } else {
            // Request CAMERA permission at runtime
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST_CODE
            )
        }
    }

//   //Displays Menu Bar
//    override fun onCreateOptionsMenu(menu: Menu): Boolean {
//        val inflater: MenuInflater = menuInflater
//        inflater.inflate(R.menu.menu, menu)
//        return true
//    }
}
