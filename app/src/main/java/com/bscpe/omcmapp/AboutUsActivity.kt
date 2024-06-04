package com.bscpe.omcmapp

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity

class AboutUsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about_us)
    }

    fun goToSettings(view: View) {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)

        val options = ActivityOptions.makeCustomAnimation(this,
            R.anim.slide_enter_left,
            R.anim.slide_exit_right
        )
        startActivity(intent, options.toBundle())
    }
}
