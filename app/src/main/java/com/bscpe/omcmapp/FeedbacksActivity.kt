package com.bscpe.omcmapp

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


class FeedbacksActivity : AppCompatActivity(){

    private lateinit var feedbackEmailEditText: TextView
    private lateinit var feedbackEditText: EditText
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firebaseDatabase: FirebaseDatabase
    private lateinit var userEmail: String
    private lateinit var userName: String

    override fun onCreate(savedInstanceState : Bundle?){
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feedbacks)

        FirebaseApp.initializeApp(this)
        firebaseAuth = FirebaseAuth.getInstance()
        val userUid = firebaseAuth.currentUser?.uid
        firebaseDatabase = FirebaseDatabase.getInstance()

        feedbackEmailEditText = findViewById(R.id.feedbackEmail)
        feedbackEditText = findViewById(R.id.feedback)

        userEmail = firebaseAuth.currentUser?.email.toString()
        feedbackEmailEditText.text = userEmail

        val userNameRef = firebaseDatabase.getReference("UsersData/$userUid/Profile/Name")

        userNameRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                userName = dataSnapshot.getValue(String::class.java).toString()
            }
            override fun onCancelled(databaseError: DatabaseError) {
                println("Database error: ${databaseError.message}")
            }
        })
    }

    fun sendFeedback(view: View) {
        val email = userEmail
        val feedback = feedbackEditText.text.toString()

        if (feedback.isNotEmpty() && email.isNotEmpty()) {
            val subject = "Feedback from: $userName"
            val message = feedback

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_EMAIL, arrayOf("omcm.system@gmail.com"))
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, message)
            }

            try {
                startActivity(Intent.createChooser(intent, "Send feedback via"))
                feedbackEditText.text.clear()

            } catch (e: Exception) {
                Toast.makeText(this, "Failed to open email client.", Toast.LENGTH_SHORT).show()
            }
        } else if (email.isEmpty()) {
            Toast.makeText(this, "Please enter your email.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Please enter your feedback.", Toast.LENGTH_SHORT).show()
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