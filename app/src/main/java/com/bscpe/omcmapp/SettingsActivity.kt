package com.bscpe.omcmapp

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val versionLabel: TextView = findViewById(R.id.version_label)

        versionLabel.setOnClickListener{
            val version = getString(R.string.version)

            Toast.makeText(this, version, Toast.LENGTH_LONG).show()
        }
    }

    fun goToMain(view: View) {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)

        val options = ActivityOptions.makeCustomAnimation(this,
            R.anim.slide_enter_left, //Enter animation
            R.anim.slide_exit_right //Exit animation
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

}