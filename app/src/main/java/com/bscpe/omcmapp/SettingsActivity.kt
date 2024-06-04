package com.bscpe.omcmapp

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class SettingsActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val versionLabel: TextView = findViewById(R.id.version_label)

        versionLabel.setOnClickListener{
            val version = getString(R.string.version)

            Toast.makeText(this, version, Toast.LENGTH_LONG).show()
        }

        val logoutButton = findViewById<Button>(R.id.logoutButton)
        logoutButton.setOnClickListener {
            logoutUser()
        }
    }

    fun logoutUser() {
        auth.signOut()
        goToLogin()
        clearSharedPrefs()
    }

    fun clearSharedPrefs() {
        val sharedPrefs = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val wifiInfoPrefs = getSharedPreferences("WifiInfo", Context.MODE_PRIVATE)

        val editor1 = sharedPrefs.edit()
        val editor2 = wifiInfoPrefs.edit()

        editor1.clear()
        editor2.clear()

        editor1.apply()
        editor2.apply()
    }

    fun goToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)

        val options = ActivityOptions.makeCustomAnimation(this,
            R.anim.slide_enter_top,
            R.anim.slide_exit_bottom
        )
        startActivity(intent, options.toBundle())
    }

    fun goToMain(view: View) {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)

        val options = ActivityOptions.makeCustomAnimation(this,
            R.anim.slide_enter_left,
            R.anim.slide_exit_right
        )
        startActivity(intent, options.toBundle())
    }

    fun goToFeedbacks(view: View) {
        val intent = Intent(this, FeedbacksActivity::class.java)
        startActivity(intent)

        val options = ActivityOptions.makeCustomAnimation(this,
            R.anim.slide_enter_right,
            R.anim.slide_exit_left)
        startActivity(intent, options.toBundle())

    }

    fun goToAboutUs(view: View) {
        val intent = Intent(this, AboutUsActivity::class.java)
        startActivity(intent)

        val options = ActivityOptions.makeCustomAnimation( this,
            R.anim.slide_enter_right,
            R.anim.slide_exit_left)
        startActivity(intent, options.toBundle())
    }

}