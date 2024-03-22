package com.bscpe.omcmapp

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class FeedbacksActivity : AppCompatActivity(){

    override fun onCreate(savedInstanceState : Bundle?){
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feedbacks)

        val sendFeedback: TextView = findViewById(R.id.send)
        val email: EditText = findViewById(R.id.feedbackEmail)
        val feedback: EditText = findViewById(R.id.feedback)

        sendFeedback.setOnClickListener {
            Toast.makeText(this, "Feedback sent!", Toast.LENGTH_LONG).show()
            email.text.clear()
            feedback.text.clear()
        }
    }

    fun goToSettings(view: View) {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)

        val options = ActivityOptions.makeCustomAnimation(this,
            R.anim.slide_enter_left,
            R.anim.slide_exit_right)
        startActivity(intent, options.toBundle())
    }


}