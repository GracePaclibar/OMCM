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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class ProfileEditActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var profilePicImageView: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_edit)

        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)

        val nameEditText = findViewById<EditText>(R.id.name_text)
        val bioEditText = findViewById<EditText>(R.id.bio_text)

        // Retrieve and set the saved user name to the EditText
        val savedUserName = sharedPreferences.getString("userName", "")
        val editTextName = findViewById<EditText>(R.id.name_text)
        editTextName.setText(savedUserName)

        // Retrieve and set the saved user bio to the EditText
        val savedUserBio = sharedPreferences.getString("userBio", "")
        val editTextBio = findViewById<EditText>(R.id.bio_text)
        editTextBio.setText(savedUserBio)

        profilePicImageView = findViewById(R.id.profilePic)

        // Retrieve the saved resource ID of the selected image
        val savedImageResId = sharedPreferences.getInt("selectedImageResId", R.drawable.pfp_1)
        profilePicImageView.setImageResource(savedImageResId)

        // Shows image options
        val uploadImageBtn: ImageButton = findViewById(R.id.uploadImageBtn)
        uploadImageBtn.setOnClickListener {
            showImageOptionsPopup()
        }

        val saveButton = findViewById<ImageButton>(R.id.save_btn)

        data class UserInfo (
            val Bio: String,
            val Name: String
        )

        saveButton.setOnClickListener{
            val currentUser = FirebaseAuth.getInstance().currentUser
            val userUid = currentUser?.uid

            val name = nameEditText.text.toString().trim()
            val bio = bioEditText.text.toString().trim()

            if (name.isEmpty()) {
                Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show()
            } else {
                val userInfo = UserInfo(bio, name)

                val capitalizedUserInfo = mapOf (
                    "Bio" to userInfo.Bio,
                    "Name" to userInfo.Name
                )

                // upload to realtime db
                val database = FirebaseDatabase.getInstance()
                val profileRef = database.getReference("UsersData/$userUid/Profile")
                profileRef.setValue(capitalizedUserInfo)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Changes saved", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Saving failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }

                val editor = sharedPreferences.edit()
                editor.putString("userName", name)
                editor.putString("userBio", bio)
                editor.apply()

                // going back to profile
                val intent = Intent(this, ProfileActivity::class.java)

                val options = ActivityOptions.makeCustomAnimation(this,
                    R.anim.slide_enter_bottom, //Enter animation
                    R.anim.slide_exit_top //Exit animation
                )

                startActivity(intent, options.toBundle())
            }
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
            // Save selected image resource ID to SharedPrefs
            saveSelectedImageResId(R.drawable.pfp_1)

            // Update profilePic ImageView with the desired image for option 1
            profilePicImageView.setImageResource(R.drawable.pfp_1)
            popupWindow.dismiss()
        }

        pfp2Btn.setOnClickListener {
            // Save selected image resource ID to SharedPrefs
            saveSelectedImageResId(R.drawable.pfp_2)

            // Update profilePic ImageView with the desired image for option 2
            profilePicImageView.setImageResource(R.drawable.pfp_2)
            popupWindow.dismiss()
        }

        pfp3Btn.setOnClickListener {
            // Save selected image resource ID to SharedPrefs
            saveSelectedImageResId(R.drawable.pfp_3)

            // Update profilePic ImageView with the desired image for option 1
            profilePicImageView.setImageResource(R.drawable.pfp_3)
            popupWindow.dismiss()
        }

        pfp4Btn.setOnClickListener {
            // Save selected image resource ID to SharedPrefs
            saveSelectedImageResId(R.drawable.pfp_4)

            // Update profilePic ImageView with the desired image for option 2
            profilePicImageView.setImageResource(R.drawable.pfp_4)
            popupWindow.dismiss()
        }

        // Show the popup at the location of uploadImageBtn
        val uploadImageBtn: ImageButton = findViewById(R.id.uploadImageBtn)
        popupWindow.showAtLocation(uploadImageBtn, Gravity.CENTER, 0, 0)
    }

    private fun saveSelectedImageResId(imageResId: Int) {
        val editor = sharedPreferences.edit()
        editor.putInt("selectedImageResId", imageResId)
        editor.apply()
    }

    fun goToProfile(view: View) {
        val intent = Intent(this, ProfileActivity::class.java)

        val options = ActivityOptions.makeCustomAnimation(this,
            R.anim.slide_enter_bottom, //Enter animation
            R.anim.slide_exit_top //Exit animation
        )

        startActivity(intent, options.toBundle())
    }

}