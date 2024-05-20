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

        if (userUid != null) {
            loadStatesFromFirebase(userUid, autoSwitch, waterSwitch, manualMode, waterControl)
        } else {
            setDefaultStates(autoSwitch, waterSwitch, manualMode, waterControl)
        }

        updateVisibility(autoSwitch.isChecked, manualMode, waterControl, waterSwitch)

        autoSwitch.setOnCheckedChangeListener { _, isChecked ->
            updateVisibility(isChecked, manualMode, waterControl, waterSwitch)
            updatePreferences("autoSwitchState", isChecked)
            updateFirebase("isAuto", isChecked)
            if (isChecked) {
                updatePreferences("waterSwitchState", false)
                updateFirebase("isWaterOn", false)
            } else {
                fetchAndSetWaterStateFromFirebase(waterSwitch)
            }
        }

        waterSwitch.setOnCheckedChangeListener { _, isChecked ->
            updatePreferences("waterSwitchState", isChecked)
            updateFirebase("isWaterOn", isChecked)
        }
    }

    private fun updateVisibility(
        isAuto: Boolean,
        manualMode: TextView,
        waterControl: TextView,
        waterSwitch: Switch
    ) {
        if (isAuto) {
            val fadeOutAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_out)
            manualMode.startAnimation(fadeOutAnimation)
            waterControl.startAnimation(fadeOutAnimation)
            waterSwitch.startAnimation(fadeOutAnimation)
            manualMode.visibility = View.INVISIBLE
            waterControl.visibility = View.INVISIBLE
            waterSwitch.visibility = View.INVISIBLE
        } else {
            val fadeInAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in)
            manualMode.startAnimation(fadeInAnimation)
            waterControl.startAnimation(fadeInAnimation)
            waterSwitch.startAnimation(fadeInAnimation)
            manualMode.visibility = View.VISIBLE
            waterControl.visibility = View.VISIBLE
            waterSwitch.visibility = View.VISIBLE
        }
    }

    private fun loadStatesFromFirebase(
        userUid: String,
        autoSwitch: Switch,
        waterSwitch: Switch,
        manualMode: TextView,
        waterControl: TextView
    ) {
        val database = FirebaseDatabase.getInstance()
        val powerRef = database.getReference("UsersData/$userUid/Control_Key/Manual")
        powerRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val isAuto = dataSnapshot.child("isAuto").getValue(Boolean::class.java) ?: true
                val isWaterOn = dataSnapshot.child("isWaterOn").getValue(Boolean::class.java) ?: false

                autoSwitch.isChecked = isAuto
                waterSwitch.isChecked = isWaterOn
                updateVisibility(isAuto, manualMode, waterControl, waterSwitch)
                updatePreferences("autoSwitchState", isAuto)
                updatePreferences("waterSwitchState", isWaterOn)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("Firebase", "Error fetching control key data", databaseError.toException())
                Toast.makeText(this@SystemConfigActivity, "Error fetching control key data", Toast.LENGTH_SHORT).show()
                setDefaultStates(autoSwitch, waterSwitch, manualMode, waterControl)
            }
        })
    }

    private fun setDefaultStates(
        autoSwitch: Switch,
        waterSwitch: Switch,
        manualMode: TextView,
        waterControl: TextView
    ) {
        autoSwitch.isChecked = true
        waterSwitch.isChecked = false
        updateVisibility(true, manualMode, waterControl, waterSwitch)
        updatePreferences("autoSwitchState", true)
        updatePreferences("waterSwitchState", false)
    }

    private fun updatePreferences(key: String, value: Boolean) {
        val sharedPrefs = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val editor = sharedPrefs.edit()
        editor.putBoolean(key, value)
        editor.apply()
    }

    private fun updateFirebase(key: String, value: Boolean) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val userUid = currentUser?.uid ?: return

        val database = FirebaseDatabase.getInstance()
        val powerRef = database.getReference("UsersData/$userUid/Control_Key/Manual")
        powerRef.child(key).setValue(value)
            .addOnFailureListener { e ->
                Log.e("Firebase", "Failed to update $key: ${e.message}")
                Toast.makeText(this, "Failed to update $key", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchAndSetWaterStateFromFirebase(waterSwitch: Switch) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val userUid = currentUser?.uid ?: return

        val database = FirebaseDatabase.getInstance()
        val powerRef = database.getReference("UsersData/$userUid/Control_Key/Manual")
        powerRef.child("isWaterOn").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val isWaterOn = dataSnapshot.getValue(Boolean::class.java) ?: false
                waterSwitch.isChecked = isWaterOn
                updatePreferences("waterSwitchState", isWaterOn)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("Firebase", "Error fetching isWaterOn state", databaseError.toException())
                Toast.makeText(this@SystemConfigActivity, "Error fetching isWaterOn state", Toast.LENGTH_SHORT).show()
            }
        })
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

