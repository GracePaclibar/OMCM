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
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class SystemConfigActivity : AppCompatActivity() {

    private lateinit var currentUserUid: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_system_config)

        val sharedPrefs = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val sharedPrefsWifi = getSharedPreferences("WifiInfo", Context.MODE_PRIVATE)

        val uidTextView = findViewById<TextView>(R.id.userUID_txt)
        val currentUser = FirebaseAuth.getInstance().currentUser

        currentUserUid = currentUser?.uid ?: "User not logged in"
        uidTextView.text = currentUserUid

        val saveButton = findViewById<Button>(R.id.saveNtwk_btn).apply { visibility = GONE }
        val visibilityButton = findViewById<ImageButton>(R.id.visibility_btn).apply { visibility = GONE }

        val databaseRef = FirebaseDatabase.getInstance().getReference("UsersData/$currentUserUid/WiFI_Router")
        val ssidEditText = findViewById<EditText>(R.id.wifiSSID)
        val passEditText = findViewById<EditText>(R.id.wifiPassword)

        ssidEditText.setText(sharedPrefsWifi.getString("SSID", ""))
        passEditText.setText(sharedPrefsWifi.getString("Pass", ""))

        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val wifiSSID = ssidEditText.text.toString()
                val wifiPass = passEditText.text.toString()
                val isVisible = wifiSSID.isNotEmpty() && wifiPass.isNotEmpty()
                saveButton.visibility = if (isVisible) VISIBLE else GONE
                visibilityButton.visibility = if (wifiPass.isNotEmpty()) VISIBLE else GONE

                visibilityButton.setOnClickListener {
                    seePassword(visibilityButton, passEditText)
                }
            }
        }

        ssidEditText.addTextChangedListener(textWatcher)
        passEditText.addTextChangedListener(textWatcher)

        databaseRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                dataSnapshot.value?.let {
                    val wifiInfoMap = it as Map<String, Any>
                    ssidEditText.setText(wifiInfoMap["SSID"] as? String ?: "")
                    passEditText.setText(wifiInfoMap["AUTH"] as? String ?: "")
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("Firebase", "Error getting data", databaseError.toException())
            }
        })

        saveButton.setOnClickListener {
            val ssid = ssidEditText.text.toString().trim()
            val password = passEditText.text.toString().trim()
            if (ssid.isNotEmpty() && password.isNotEmpty() && currentUserUid != "User not logged in") {
                saveWifiInfo(ssid, password)
            } else {
                Toast.makeText(this, "Please enter both SSID and password", Toast.LENGTH_SHORT).show()
            }
        }

        val autoSwitch = findViewById<SwitchCompat>(R.id.auto_switch)
        val manualMode = findViewById<TextView>(R.id.manual_state)
        val waterControl = findViewById<TextView>(R.id.water_control)
        val waterSwitch = findViewById<SwitchCompat>(R.id.water_switch)
        val autoElapsedTime = findViewById<TextView>(R.id.auto_elapsedTime)
        val manualElapsedTime = findViewById<TextView>(R.id.manual_elapsedTime)

        if (currentUserUid != "User not logged in") {
            loadStatesFromFirebase(currentUserUid, autoSwitch, waterSwitch, manualMode, waterControl, autoElapsedTime, manualElapsedTime)
            listenForWaterStateChanges(currentUserUid, autoElapsedTime, manualElapsedTime)
        } else {
            setDefaultStates(autoSwitch, waterSwitch, manualMode, waterControl, autoElapsedTime, manualElapsedTime)
        }

        updateVisibility(autoSwitch.isChecked, manualMode, waterControl, waterSwitch, autoElapsedTime, manualElapsedTime)

        autoSwitch.setOnCheckedChangeListener { _, isChecked ->
            updateVisibility(isChecked, manualMode, waterControl, waterSwitch, autoElapsedTime, manualElapsedTime)
            updatePreferences("autoSwitchState", if (isChecked) 1 else 0)
            updateFirebase("isAuto", if (isChecked) 1 else 0)
            if (!isChecked) {
                fetchAndSetWaterStateFromFirebase(waterSwitch)
            } else {
                updatePreferences("waterSwitchState", 0)
                updateFirebase("isWaterOn", 0)
            }
        }

        waterSwitch.setOnCheckedChangeListener { _, isChecked ->
            updatePreferences("waterSwitchState", if (isChecked) 1 else 0)
            updateFirebase("isWaterOn", if (isChecked) 1 else 0)
            handleManualElapsedTimeVisibility(manualElapsedTime, isChecked)
        }
    }

    private fun listenForWaterStateChanges(userUid: String, autoElapsedTime: TextView, manualElapsedTime: TextView) {
        val database = FirebaseDatabase.getInstance()
        val autoWaterRef = database.getReference("UsersData/$userUid/Control_Key/dripWater/isWaterOnAuto")
        autoWaterRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val isWaterOnAuto = dataSnapshot.getValue(Int::class.java) ?: 0
                if (isWaterOnAuto == 1) {
                    autoElapsedTime.text = "Water is on"
                } else {
                    autoElapsedTime.text = "Water is off"
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("Firebase", "Error fetching isWaterOnAuto state", databaseError.toException())
            }
        })

        val manualWaterRef = database.getReference("UsersData/$userUid/Control_Key/dripWater/isWaterOn")
        manualWaterRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val isWaterOn = dataSnapshot.getValue(Int::class.java) ?: 0
                if (isWaterOn == 1) {
                    manualElapsedTime.text = "Water is on"
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("Firebase", "Error fetching isWaterOn state", databaseError.toException())
            }
        })
    }


    private fun updateVisibility(
        isAuto: Boolean,
        manualMode: TextView,
        waterControl: TextView,
        waterSwitch: SwitchCompat,
        autoElapsedTime: TextView,
        manualElapsedTime: TextView
    ) {
        val fadeInAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        val fadeOutAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_out)
        val visibility = if (isAuto) View.INVISIBLE else View.VISIBLE

        manualMode.startAnimation(if (isAuto) fadeOutAnimation else fadeInAnimation)
        waterControl.startAnimation(if (isAuto) fadeOutAnimation else fadeInAnimation)
        waterSwitch.startAnimation(if (isAuto) fadeOutAnimation else fadeInAnimation)
        manualElapsedTime.startAnimation(if (isAuto) fadeOutAnimation else fadeInAnimation)

        manualMode.visibility = visibility
        waterControl.visibility = visibility
        waterSwitch.visibility = visibility
        manualElapsedTime.visibility = visibility

        if (isAuto) {
            autoElapsedTime.startAnimation(fadeInAnimation)
            autoElapsedTime.visibility = VISIBLE
        } else {
            autoElapsedTime.startAnimation(fadeOutAnimation)
            autoElapsedTime.visibility = INVISIBLE
        }
    }

    private fun handleManualElapsedTimeVisibility(manualElapsedTime: TextView, isWaterOn: Boolean) {
        val fadeInAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        val fadeOutAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_out)

        if (isWaterOn) {
            manualElapsedTime.startAnimation(fadeInAnimation)
            manualElapsedTime.visibility = VISIBLE
        } else {
            manualElapsedTime.startAnimation(fadeOutAnimation)
            manualElapsedTime.visibility = INVISIBLE
        }
    }

    private fun loadStatesFromFirebase(
        userUid: String,
        autoSwitch: SwitchCompat,
        waterSwitch: SwitchCompat,
        manualMode: TextView,
        waterControl: TextView,
        autoElapsedTime: TextView,
        manualElapsedTime: TextView
    ) {
        val database = FirebaseDatabase.getInstance()
        val powerRef = database.getReference("UsersData/$userUid/Control_Key/dripWater")
        powerRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val isAuto = dataSnapshot.child("isAuto").getValue(Int::class.java) ?: 1
                val isWaterOn = dataSnapshot.child("isWaterOn").getValue(Int::class.java) ?: 0

                autoSwitch.isChecked = isAuto == 1
                waterSwitch.isChecked = isWaterOn == 1
                updateVisibility(isAuto == 1, manualMode, waterControl, waterSwitch, autoElapsedTime, manualElapsedTime)
                updatePreferences("autoSwitchState", isAuto)
                updatePreferences("waterSwitchState", isWaterOn)
                handleAutoElapsedTime(autoElapsedTime, isAuto == 1, isWaterOn)
                handleManualElapsedTimeVisibility(manualElapsedTime, isWaterOn == 1)
            }
            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("Firebase", "Error fetching control key data", databaseError.toException())
                Toast.makeText(this@SystemConfigActivity, "Error fetching control key data", Toast.LENGTH_SHORT).show()
                setDefaultStates(autoSwitch, waterSwitch, manualMode, waterControl, autoElapsedTime, manualElapsedTime)
            }
        })
    }

    private fun handleAutoElapsedTime(autoElapsedTime: TextView, isAuto: Boolean, isWaterOn: Int) {
        if (isAuto) {
            autoElapsedTime.visibility = View.VISIBLE
            if (isWaterOn == 1) {
                autoElapsedTime.text = "Water is on"
            } else {
                autoElapsedTime.text = "Water is off"
            }
        } else {
            autoElapsedTime.visibility = View.GONE
        }
    }


    private fun setDefaultStates(
        autoSwitch: SwitchCompat,
        waterSwitch: SwitchCompat,
        manualMode: TextView,
        waterControl: TextView,
        autoElapsedTime: TextView,
        manualElapsedTime: TextView
    ) {
        autoSwitch.isChecked = true
        waterSwitch.isChecked = false
        updateVisibility(true, manualMode, waterControl, waterSwitch, autoElapsedTime, manualElapsedTime)
        updatePreferences("autoSwitchState", 1)
        updatePreferences("waterSwitchState", 0)
    }

    private fun updatePreferences(key: String, value: Int) {
        val sharedPrefs = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        with(sharedPrefs.edit()) {
            putInt(key, value)
            apply()
        }
    }

    private fun updateFirebase(key: String, value: Int) {
        if (currentUserUid == "User not logged in") return

        val powerRef = FirebaseDatabase.getInstance().getReference("UsersData/$currentUserUid/Control_Key/dripWater")
        powerRef.child(key).setValue(value)
            .addOnFailureListener { e ->
                Log.e("Firebase", "Failed to update $key: ${e.message}")
                Toast.makeText(this, "Failed to update $key", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchAndSetWaterStateFromFirebase(waterSwitch: SwitchCompat) {
        if (currentUserUid == "User not logged in") return

        val powerRef = FirebaseDatabase.getInstance().getReference("UsersData/$currentUserUid/Control_Key/dripWater")
        powerRef.child("isWaterOn").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val isWaterOn = dataSnapshot.getValue(Int::class.java) ?: 0
                waterSwitch.isChecked = isWaterOn == 1
                updatePreferences("waterSwitchState", isWaterOn)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("Firebase", "Error fetching isWaterOn state", databaseError.toException())
                Toast.makeText(this@SystemConfigActivity, "Error fetching isWaterOn state", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun saveWifiInfo(ssid: String, password: String) {
        val wifiInfo = mapOf("AUTH" to password, "SSID" to ssid, "Wifi_Detected" to false)
        val wifiRouterRef = FirebaseDatabase.getInstance().getReference("UsersData/$currentUserUid/WiFI_Router")
        wifiRouterRef.setValue(wifiInfo)
            .addOnSuccessListener {
                Toast.makeText(this, "Wi-Fi info saved", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to save: ${e.message}", Toast.LENGTH_SHORT).show()
            }

        val sharedPrefsWifi = getSharedPreferences("WifiInfo", Context.MODE_PRIVATE)
        with(sharedPrefsWifi.edit()) {
            putString("SSID", ssid)
            putString("Pass", password)
            apply()
        }
    }

    fun goToMain(view: View) {
        val intent = Intent(this, MainActivity::class.java)
        val options = ActivityOptions.makeCustomAnimation(this,
            R.anim.slide_enter_right,
            R.anim.slide_exit_left
        )
        startActivity(intent, options.toBundle())
    }

    fun seePassword(visibilityButton: ImageButton, passEditText: EditText) {
        val isVisible = passEditText.inputType != InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        visibilityButton.setImageResource(if (isVisible) R.drawable.ic_visibility else R.drawable.ic_visibility_off)
        passEditText.inputType = if (isVisible) {
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        } else {
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        }
        passEditText.setSelection(passEditText.text.length)
    }
}


