package com.bscpe.omcmapp

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class SystemConfigActivity: AppCompatActivity() {
    override fun onCreate(savedInstantState: Bundle?) {
        super.onCreate(savedInstantState)
        setContentView(R.layout.activity_system_config)

        val uidTextView = findViewById<TextView>(R.id.userUID_txt)

        // get uid from firebase
        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser != null) {
            val uid = currentUser.uid
            uidTextView.text = uid
        } else {
            uidTextView.text = "User not logged in"
        }

        // Initial visibility of save button
        val saveButton = findViewById<Button>(R.id.saveNtwk_btn)
        saveButton.visibility = View.GONE

        val userUid = FirebaseAuth.getInstance().currentUser?.uid
        val ssidEditText = findViewById<EditText>(R.id.wifiSSID)
        val passEditText = findViewById<EditText>(R.id.wifiPassword)

        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val wifiSSID = ssidEditText.text.toString()
                val wifiPass = passEditText.text.toString()

                if (wifiSSID.isNotEmpty() && wifiPass.isNotEmpty()) {
                    saveButton.visibility = View.VISIBLE
                } else {
                    saveButton.visibility = View.GONE
                }
            }
        }

        ssidEditText.addTextChangedListener(textWatcher)
        passEditText.addTextChangedListener(textWatcher)

        data class WifiInfo (
            val AUTH: String,
            val SSID: String,
            val Wifi_Detected: Boolean
        )

        saveButton.setOnClickListener {
            // Retrieve the current user UID from Firebase Authentication
            val currentUser = FirebaseAuth.getInstance().currentUser
            val userUid = currentUser?.uid

            // Retrieve the SSID and password from EditText fields
            val ssid = ssidEditText.text.toString().trim()
            val password = passEditText.text.toString().trim()

            // Check if SSID and password are not empty and user UID is not null
            if (ssid.isNotEmpty() && password.isNotEmpty() && userUid != null) {
                // Create a WifiInfo object
                val wifiInfo = WifiInfo(ssid, password, Wifi_Detected = false)

                val capitalizedWifiInfo = mapOf(
                    "AUTH" to wifiInfo.AUTH,
                    "SSID" to wifiInfo.SSID,
                    "Wifi_Detected" to wifiInfo.Wifi_Detected
                )

                // Upload WifiInfo object to Firebase Realtime Database
                val database = FirebaseDatabase.getInstance()
                val wifiRouterRef = database.getReference("UsersData/$userUid/WiFI_Router")
                wifiRouterRef.setValue(capitalizedWifiInfo)
                    .addOnSuccessListener {
                        // Upload successful
                        Toast.makeText(this, "Wi-Fi info saved", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        // Upload failed
                        Toast.makeText(this, "Failed to save: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                // Handle case where SSID, password, or user UID is null
                Toast.makeText(this, "Please enter both SSID and password", Toast.LENGTH_SHORT).show()
            }
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

}

