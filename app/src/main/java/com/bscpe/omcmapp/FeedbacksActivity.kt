package com.bscpe.omcmapp

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseApp


class FeedbacksActivity : AppCompatActivity(){

    private lateinit var feedbackEmailEditText: EditText
    private lateinit var feedbackEditText: EditText

    override fun onCreate(savedInstanceState : Bundle?){
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feedbacks)

        feedbackEmailEditText = findViewById(R.id.feedbackEmail)
        feedbackEditText = findViewById(R.id.feedback)

        // Initializing Firebase
        FirebaseApp.initializeApp(this)
    }

    fun sendFeedback(view : View) {
        val email = feedbackEmailEditText.text.toString()
        val feedback = feedbackEditText.text.toString()

        if (feedback.isNotEmpty() && email.isNotEmpty()) {
            val subject = "Feedback from $email"
            val message = feedback

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "message/rfc822"
                putExtra(Intent.EXTRA_EMAIL, arrayOf("smarthydra16@gmail.com"))
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, message)

                feedbackEditText.text.clear()
                feedbackEmailEditText.text.clear()
            }

            try {
                startActivity(Intent.createChooser(intent, "Please select an email client"))

            } catch (e: Exception) {
                Toast.makeText(this, "Failed to open email client.", Toast.LENGTH_SHORT).show()
            }
        }
        else if (email.isEmpty()) {
            Toast.makeText(this, "Please enter your email.", Toast.LENGTH_SHORT).show()
        }
        else {
            Toast.makeText(this, "Please enter your feedback.", Toast.LENGTH_SHORT).show()
        }
    }

//    fun sendFeedback(view : View) {
//        val email = feedbackEmailEditText.text.toString()
//        val feedback = feedbackEditText.text.toString()
//
//        if (feedback.isNotEmpty() && email.isNotEmpty()) {
//            val databaseRef = FirebaseDatabase.getInstance().reference
//
//            // appending a child path for feedback and email to set value
//            val feedbackRef = databaseRef.child("Feedbacks").push() //generates UID for feedback
//
//            // hashmap for email and feedback
//            val feedbackData = HashMap<String, Any>()
//            feedbackData["feedback"] = feedback
//            feedbackData["email"] = email
//
//            feedbackRef.setValue(feedbackData)
//
//            feedbackEditText.text.clear()
//            feedbackEmailEditText.text.clear()
//
//            Toast.makeText(this, "Thank you for your feedback!", Toast.LENGTH_SHORT).show()
//        }
//        else if (email.isEmpty()) {
//            Toast.makeText(this, "Please enter your email.", Toast.LENGTH_SHORT).show()
//        }
//        else {
//            Toast.makeText(this, "Please enter your feedback.", Toast.LENGTH_SHORT).show()
//        }
//    }

    fun goToSettings(view: View) {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)

        val options = ActivityOptions.makeCustomAnimation(this,
            R.anim.slide_enter_left,
            R.anim.slide_exit_right)
        startActivity(intent, options.toBundle())
    }

}