package com.bscpe.omcmapp

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
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

        // Find image container
        val imageView = findViewById<ImageView>(R.id.camTest)

        // Retrieve the image URI from SharedPreferences
        val imageUriString = sharedPreferences.getString("imageUri", null)
        if (imageUriString != null) {
            val imageUri = Uri.parse(imageUriString)
            imageView.setImageURI(imageUri)

            imageView.setOnClickListener {
                openImageInGallery(imageUri)
            }
        }
    }

    private fun openImageInGallery(imageUri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(imageUri, "image/*")
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        val fileProviderAuthority = "${packageName}.provider"
        val contentUri = FileProvider.getUriForFile(this, fileProviderAuthority, File(imageUri.path!!))
        intent.setDataAndType(contentUri, "image/*")
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        startActivity(intent)
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
