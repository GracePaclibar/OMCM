package com.bscpe.omcmapp

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity

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
