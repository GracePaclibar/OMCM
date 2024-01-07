package com.bscpe.omcmapp

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ProfileEditActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_edit)

        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)

        // Retrieve and set the saved user name to the EditText
        val savedUserName = sharedPreferences.getString("userName", "")
        val editTextName = findViewById<EditText>(R.id.name_text)
        editTextName.setText(savedUserName)

        // Retrieve and set the saved user name to the EditText
        val savedUserBio = sharedPreferences.getString("userBio", "")
        val editTextBio = findViewById<EditText>(R.id.bio_text)
        editTextBio.setText(savedUserBio)

    }

    fun goToProfile(view: View) {
        val intent = Intent(this, ProfileActivity::class.java)
        startActivity(intent)

        val options = ActivityOptions.makeCustomAnimation(this,
            R.anim.slide_enter_bottom, //Enter animation
            R.anim.slide_exit_top //Exit animation
        )

        startActivity(intent, options.toBundle())
    }

    fun saveEdit(view: View) {
        val editTextName = findViewById<EditText>(R.id.name_text)
        val editTextBio = findViewById<EditText>(R.id.bio_text)

        val userName = editTextName.text.toString()
        val userBio = editTextBio.text.toString()

        val editor = sharedPreferences.edit()
        editor.putString("userName", userName)
        editor.putString("userBio", userBio)
        editor.apply()

        Toast.makeText(this, "Changes Saved", Toast.LENGTH_SHORT).show()
    }

}