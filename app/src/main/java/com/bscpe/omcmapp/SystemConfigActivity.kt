package com.bscpe.omcmapp

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.View.GONE
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class SystemConfigActivity: AppCompatActivity() {
    override fun onCreate(savedInstantState: Bundle?) {
        super.onCreate(savedInstantState)
        setContentView(R.layout.activity_system_config)

        val sharedPrefs = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val sharedPrefsWifi = getSharedPreferences("WifiInfo", Context.MODE_PRIVATE)

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

        // Initial visibility of pass visibility button
        val visibilityButton = findViewById<ImageButton>(R.id.visibility_btn)
        visibilityButton.visibility = View.GONE

        val userUid = FirebaseAuth.getInstance().currentUser?.uid

        val databaseRef = FirebaseDatabase.getInstance().getReference("UsersData/$userUid/WiFI_Router")
        val ssidEditText = findViewById<EditText>(R.id.wifiSSID)
        val passEditText = findViewById<EditText>(R.id.wifiPassword)

        val savedSSID = sharedPrefsWifi.getString("SSID", "")
        val savedPass = sharedPrefsWifi.getString("Pass", "")
        ssidEditText.setText(savedSSID)
        passEditText.setText(savedPass)

        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val wifiSSID = ssidEditText.text.toString()
                val wifiPass = passEditText.text.toString()

                if (wifiSSID.isNotEmpty() && wifiPass.isNotEmpty()) {
                    saveButton.visibility = VISIBLE
                    if (wifiPass.isNotEmpty()) {
                        visibilityButton.visibility = VISIBLE

                        visibilityButton.setOnClickListener {
                            seePassword(visibilityButton, passEditText)
                        }
                    }
                } else {
                    saveButton.visibility = GONE
                    visibilityButton.visibility = GONE
                }
            }
        }

        ssidEditText.addTextChangedListener(textWatcher)
        passEditText.addTextChangedListener(textWatcher)

        data class WifiInfo (
            val AUTH: String = "",
            val SSID: String = "",
            val Wifi_Detected: Boolean = false
        ) {
            constructor() : this("", "")
        }

        fun mapToWifiInfo(map: Map<String, Any>): WifiInfo {
            val auth = map["AUTH"] as? String ?: ""
            val ssid = map["SSID"] as? String ?: ""
            val wifiDetectedString = map["Wifi_Detected"] as? String ?: "false"
            val wifiDetected = wifiDetectedString.toBoolean()
            return WifiInfo(auth, ssid, wifiDetected)
        }

        databaseRef.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if(dataSnapshot.exists()) {
                    val wifiInfoMap = dataSnapshot.value as? Map<String, Any>
                    if (wifiInfoMap != null) {
                        val wifiInfo = mapToWifiInfo(wifiInfoMap)
                        if (wifiInfo.AUTH.isNotEmpty()) {
                            passEditText.setText(wifiInfo.AUTH)
                        }
                        if (wifiInfo.SSID.isNotEmpty()) {
                            ssidEditText.setText(wifiInfo.SSID)
                        }
                    }
                }
            }
            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("Firebase", "Error getting data", databaseError.toException())
            }
        })

        saveButton.setOnClickListener {
            val currentUser = FirebaseAuth.getInstance().currentUser
            val userUid = currentUser?.uid

            val ssid = ssidEditText.text.toString().trim()
            val password = passEditText.text.toString().trim()

            if (ssid.isNotEmpty() && password.isNotEmpty() && userUid != null) {
                val wifiInfo = WifiInfo(password, ssid, Wifi_Detected = false)

                val capitalizedWifiInfo = mapOf(
                    "AUTH" to wifiInfo.AUTH,
                    "SSID" to wifiInfo.SSID,
                    "Wifi_Detected" to wifiInfo.Wifi_Detected
                )

                // Upload WifiInfo to Realtime Database
                val database = FirebaseDatabase.getInstance()
                val wifiRouterRef = database.getReference("UsersData/$userUid/WiFI_Router")
                wifiRouterRef.setValue(capitalizedWifiInfo)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Wi-Fi info saved", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to save: ${e.message}", Toast.LENGTH_SHORT).show()
                    }

                val sharedPrefsWifi = getSharedPreferences("WifiInfo", Context.MODE_PRIVATE)
                val editor = sharedPrefsWifi.edit()
                editor.putString("SSID", ssid)
                editor.putString("Pass", password)
                editor.apply()

            } else {
                Toast.makeText(this, "Please enter both SSID and password", Toast.LENGTH_SHORT).show()
            }
        }

        val autoSwitch = findViewById<Switch>(R.id.auto_switch)
        val manualMode = findViewById<TextView>(R.id.manual_state)
        val waterControl = findViewById<TextView>(R.id.water_control)
        val waterSwitch = findViewById<Switch>(R.id.water_switch)

        val autoSwitchState = sharedPrefs.getBoolean("autoSwitchState", true)
        autoSwitch.isChecked = autoSwitchState

        val waterSwitchState = sharedPrefs.getBoolean("waterSwitchState", false)
        waterSwitch.isChecked = waterSwitchState

        if (autoSwitch.isChecked) {
            manualMode.visibility = View.INVISIBLE
            waterControl.visibility = View.INVISIBLE
            waterSwitch.visibility = View.INVISIBLE
        } else {
            manualMode.visibility = View.VISIBLE
            waterControl.visibility = View.VISIBLE
            waterSwitch.visibility = View.VISIBLE
        }

        autoSwitch.setOnCheckedChangeListener { _, isChecked ->
            val sharedPrefs = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
            val editor = sharedPrefs.edit()
            editor.putBoolean("autoSwitchState", isChecked)
            editor.apply()

            val currentUser = FirebaseAuth.getInstance().currentUser
            val userUid = currentUser?.uid

            if (isChecked) {

                val isWaterOn = false

                val fadeOutAnimation = AnimationUtils.loadAnimation(this@SystemConfigActivity, R.anim.fade_out)
                manualMode.startAnimation(fadeOutAnimation)
                waterControl.startAnimation(fadeOutAnimation)
                waterSwitch.startAnimation(fadeOutAnimation)

                manualMode.visibility = INVISIBLE
                waterControl.visibility = INVISIBLE
                waterSwitch.visibility = INVISIBLE

                editor.putBoolean("waterSwitchState", isWaterOn)
                editor.apply()

                val database = FirebaseDatabase.getInstance()
                val powerRef = database.getReference("UsersData/$userUid/Control_Key/Manual")

                powerRef.child("isAuto").setValue(isChecked)
                powerRef.child("isWaterOn").setValue(isWaterOn)
            } else {

                val fadeInAnimation = AnimationUtils.loadAnimation(this@SystemConfigActivity, R.anim.fade_in)
                manualMode.startAnimation(fadeInAnimation)
                waterControl.startAnimation(fadeInAnimation)
                waterSwitch.startAnimation(fadeInAnimation)

                manualMode.visibility = VISIBLE
                waterControl.visibility = VISIBLE
                waterSwitch.visibility = VISIBLE

                val database = FirebaseDatabase.getInstance()
                val powerRef = database.getReference("UsersData/$userUid/Control_Key/Manual")

                powerRef.child("isAuto").setValue(isChecked)
                powerRef.child("isWaterOn").addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        val isWaterOn = dataSnapshot.getValue(Boolean::class.java) ?: false
                        powerRef.child("isWaterOn").setValue(isWaterOn)

                        waterSwitch.isChecked = isWaterOn

                        editor.putBoolean("waterSwitchState", isWaterOn)
                        editor.apply()
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        Log.e("Firebase", "Error getting data", databaseError.toException())
                    }
                })
            }
        }

        waterSwitch.setOnCheckedChangeListener { _, isChecked ->
            val currentUser = FirebaseAuth.getInstance().currentUser
            val userUid = currentUser?.uid

            if (userUid != null) {
                val database = FirebaseDatabase.getInstance()
                val waterRef = database.getReference("UsersData/$userUid/Control_Key/Manual")

                waterRef.child("isWaterOn").setValue(isChecked)
                    .addOnSuccessListener {
                        if (isChecked) {
                            Toast.makeText(this@SystemConfigActivity, "Water Switch On", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@SystemConfigActivity, "Water Switch Off", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this@SystemConfigActivity, "Failed to apply change: ${e.message}", Toast.LENGTH_SHORT).show()
                    }

                val sharedPrefs = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                val editor = sharedPrefs.edit()
                editor.putBoolean("waterSwitchState", isChecked)
                editor.apply()
            } else {
                Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
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

    fun seePassword(visibilityButton: ImageButton, passEditText: EditText) {
        val isVisible = passEditText.inputType != InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

        if (isVisible) {
            visibilityButton.setImageResource(R.drawable.ic_visibility)
            passEditText.inputType =
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        } else {
            visibilityButton.setImageResource(R.drawable.ic_visibility_off)
            passEditText.inputType =
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        }
        passEditText.setSelection(passEditText.text.length)
    }
}

