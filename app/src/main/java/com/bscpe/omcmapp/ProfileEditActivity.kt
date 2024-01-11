package com.bscpe.omcmapp

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ProfileEditActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var profilePicImageView: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_edit)

        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)

        // Retrieve and set the saved user name to the EditText
        val savedUserName = sharedPreferences.getString("userName", "")
        val editTextName = findViewById<EditText>(R.id.name_text)
        editTextName.setText(savedUserName)

        // Retrieve and set the saved user name to the EditText
        val savedUserBio = sharedPreferences.getString("userBio", "")
        val editTextBio = findViewById<EditText>(R.id.bio_text)
        editTextBio.setText(savedUserBio)

        profilePicImageView = findViewById(R.id.profilePic)

        val uploadImageBtn: ImageButton = findViewById(R.id.uploadImageBtn)
        uploadImageBtn.setOnClickListener {
            showImageOptionsPopup()
        }

    }

    private fun showImageOptionsPopup() {
        val popupView = layoutInflater.inflate(R.layout.image_options_popup, null)
        val popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        // ImageButtons inside the popup and their onClickListener
        val pfp1Btn: ImageButton = popupView.findViewById(R.id.pfp1_btn)
        val pfp2Btn: ImageButton = popupView.findViewById(R.id.pfp2_btn)
        val pfp3Btn: ImageButton = popupView.findViewById(R.id.pfp3_btn)
        val pfp4Btn: ImageButton = popupView.findViewById(R.id.pfp4_btn)

        pfp1Btn.setOnClickListener {
            // Update profilePic ImageView with the desired image for option 1
            profilePicImageView.setImageResource(R.drawable.pfp_1)
            popupWindow.dismiss()
        }

        pfp2Btn.setOnClickListener {
            // Update profilePic ImageView with the desired image for option 2
            profilePicImageView.setImageResource(R.drawable.pfp_2)
            popupWindow.dismiss()
        }

        pfp3Btn.setOnClickListener {
            // Update profilePic ImageView with the desired image for option 1
            profilePicImageView.setImageResource(R.drawable.pfp_3)
            popupWindow.dismiss()
        }

        pfp4Btn.setOnClickListener {
            // Update profilePic ImageView with the desired image for option 2
            profilePicImageView.setImageResource(R.drawable.pfp_4)
            popupWindow.dismiss()
        }

        // Show the popup at the location of uploadImageBtn
        val uploadImageBtn: ImageButton = findViewById(R.id.uploadImageBtn)
        popupWindow.showAtLocation(uploadImageBtn, Gravity.CENTER, 0, 0)
    }

    fun goToProfile(view: View) {
        val intent = Intent(this, ProfileActivity::class.java)

        val options = ActivityOptions.makeCustomAnimation(this,
            R.anim.slide_enter_bottom, //Enter animation
            R.anim.slide_exit_top //Exit animation
        )

        startActivity(intent, options.toBundle())
    }

    fun saveEdit(view: View) {
        val editTextName = findViewById<EditText>(R.id.name_text)
        val editTextBio = findViewById<EditText>(R.id.bio_text)

        val userName = editTextName.text.toString()
        val userBio = editTextBio.text.toString()

        val editor = sharedPreferences.edit()
        editor.putString("userName", userName)
        editor.putString("userBio", userBio)
        editor.apply()

        Toast.makeText(this, "Changes Saved", Toast.LENGTH_SHORT).show()
    }

}