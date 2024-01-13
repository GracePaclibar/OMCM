package com.bscpe.omcmapp

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.ImageButton
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private val takePictureLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val imageBitmap = result.data?.extras?.get("data") as Bitmap?
                if (imageBitmap != null) {
                    // Save the image to external storage
                    val imageUri = saveImageToExternalStorage(imageBitmap)

                    // Save the image URI to SharedPreferences
                    val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                    val editor = sharedPreferences.edit()
                    editor.putString("imageUri", imageUri.toString())
                    editor.apply()

                    // Pass the image URI to the second activity
                    val intent = Intent(this, ProfileActivity::class.java)
                    intent.putExtra("imageUri", imageUri.toString())
                    startActivity(intent)
                }
            }
        }

    private fun saveImageToExternalStorage(image: Bitmap): Uri {
        val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val imageFile = File(imagesDir, "image_${System.currentTimeMillis()}.jpg")

        FileOutputStream(imageFile).use { fos ->
            image.compress(Bitmap.CompressFormat.JPEG, 100, fos)
        }

        return Uri.fromFile(imageFile)
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

    }

    private fun openCamera() {
        val captureImageIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        if (captureImageIntent.resolveActivity(packageManager) != null) {
            takePictureLauncher.launch(captureImageIntent)
        } else {
            // Handle the case where no camera app is available
            // You can display a message or take alternative actions
        }
    }

//   //Displays Menu Bar
//    override fun onCreateOptionsMenu(menu: Menu): Boolean {
//        val inflater: MenuInflater = menuInflater
//        inflater.inflate(R.menu.menu, menu)
//        return true
//    }
}
