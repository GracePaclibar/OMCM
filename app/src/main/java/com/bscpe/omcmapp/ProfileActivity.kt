package com.bscpe.omcmapp

import android.app.ActivityOptions
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File

class ProfileActivity : AppCompatActivity() {

    private val sharedPreferences by lazy {
        getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Retrieve and set the saved user name to the EditText
        val savedUserName = sharedPreferences.getString("userName", "")
        val editTextName = findViewById<EditText>(R.id.name_text)
        editTextName.setText(savedUserName)

        // Retrieve and set the saved user name to the EditText
        val savedUserBio = sharedPreferences.getString("userBio", "")
        val editTextBio = findViewById<EditText>(R.id.bio_text)
        editTextBio.setText(savedUserBio)

        // Retrieve the count of saved images
        val imageCount = sharedPreferences.getInt("imageCount", 0)

        // Create a list to hold ImageViews
        val imageViews = mutableListOf<ImageView>()

        // Find the container layout for ImageViews
        val imageContainer = findViewById<LinearLayout>(R.id.imageContainer)

        // Retrieve delete button
        val deleteButton = findViewById<Button>(R.id.Delete_btn)

        // Initial visibility of delete button
        deleteButton.visibility = View.GONE

        // Iterate through the saved images and create ImageViews
        for (i in 1..imageCount) {
            val imageView = ImageView(this)
            val layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            layoutParams.width = 675
            layoutParams.height = layoutParams.width
            layoutParams.gravity = Gravity.CENTER_VERTICAL

            imageView.layoutParams = layoutParams
            imageView.scaleType = ImageView.ScaleType.CENTER_CROP

            layoutParams.setMargins(5,10,5,0)

            // Retrieve the image URI from SharedPreferences
            val imageUriString = sharedPreferences.getString("imageUri_$i", null)
            if (imageUriString != null) {
                val imageUri = Uri.parse(imageUriString)
                imageView.setImageURI(imageUri)

                imageView.setOnClickListener {
                    openImageInGallery(imageUri)
                }

                imageView.setOnLongClickListener{
                    deleteButton.visibility = View.VISIBLE
                    deleteButton.setOnClickListener{
                        showDeleteConfirmationDialog(imageUri, imageView)
                    }
                    true
                }

                // Add ImageView to the list and the container layout
                imageViews.add(imageView)
                imageContainer.addView(imageView)
            }
        }
    }

    private fun showDeleteConfirmationDialog(imageUri: Uri, imageView: ImageView){
        val deleteButton = findViewById<Button>(R.id.Delete_btn)

        AlertDialog.Builder(this)
            .setTitle("Delete Image")
            .setMessage("Are you sure you want to delete this image?")
            .setPositiveButton("Yes") { _, _ ->
                // User clicked Yes, delete the image
                deleteImage(imageUri, imageView)
            }
            .setNegativeButton("No", null)
            .show()
        deleteButton.visibility = View.GONE
    }

    private fun deleteImage(imageUri: Uri, imageView: ImageView) {
        // Remove the ImageView from the container
        val imageContainer = findViewById<LinearLayout>(R.id.imageContainer)

        imageContainer.removeView(imageView)

        // Update your data (Shared Preferences) to remove the image URI
        removeImageUriFromSharedPreferences(imageUri)
    }

    private fun removeImageUriFromSharedPreferences(imageUri: Uri) {
        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        // Retrieve the count of saved images
        val imageCount = sharedPreferences.getInt("imageCount", 0)

        // Create a new list to store the updated image URIs
        val updatedImageUris = mutableListOf<String>()

        // Iterate through the saved images and check if the current image URI matches
        for (i in 1..imageCount) {
            val storedUriString = sharedPreferences.getString("imageUri_$i", null)
            if (storedUriString != null && Uri.parse(storedUriString) != imageUri) {
                // Add the non-matching image URI to the updated list
                updatedImageUris.add(storedUriString)
            }
        }

        // Update the image URIs in SharedPreferences
        for (i in updatedImageUris.indices) {
            editor.putString("imageUri_${i + 1}", updatedImageUris[i])
        }

        // Decrease the image count
        editor.putInt("imageCount", updatedImageUris.size)

        // Apply changes
        editor.apply()
    }

    private fun openImageInGallery(imageUri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW)

        // Use FileProvider to get a content URI
        val fileProviderAuthority = "${packageName}.provider"
        val contentUri = FileProvider.getUriForFile(this, fileProviderAuthority, File(imageUri.path!!))

        intent.setDataAndType(contentUri, "image/*")
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        // Check if there's a suitable app to handle the intent
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            // Handle the case where no app can handle the intent
            Toast.makeText(this, "No app can handle this action", Toast.LENGTH_SHORT).show()
        }
    }

    fun goToMain(view: View) {
        val intent = Intent(this, MainActivity::class.java)

        val options = ActivityOptions.makeCustomAnimation(this,
            R.anim.slide_enter_right, //Entrance animation
            R.anim.slide_exit_left //Exit animation
        )

        startActivity(intent, options.toBundle())
    }

    fun goToProfileEdit(view: View) {
        val intent = Intent(this, ProfileEditActivity::class.java)

        val options = ActivityOptions.makeCustomAnimation(this,
            R.anim.slide_enter_top,
            R.anim.slide_exit_bottom
        )

        startActivity(intent, options.toBundle())
    }


}
