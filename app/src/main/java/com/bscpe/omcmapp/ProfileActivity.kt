package com.bscpe.omcmapp

import android.app.ActivityOptions
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso


class ProfileActivity : AppCompatActivity() {

    private val imageUrls = mutableListOf<String>()
    private val imageOrderList = mutableListOf<String>()

    private val sharedPreferences by lazy {
        getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Retrieve and set the saved user name to the EditText
        val savedUserName = sharedPreferences.getString("userName", "")
        val editTextName = findViewById<EditText>(R.id.name_text)
        editTextName.setText(savedUserName)

        // Retrieve and set the saved user name to the EditText
        val savedUserBio = sharedPreferences.getString("userBio", "")
        val editTextBio = findViewById<EditText>(R.id.bio_text)
        editTextBio.setText(savedUserBio)

        // Retrieve delete button
        val deleteButton = findViewById<Button>(R.id.Delete_btn)

        // Initial visibility of delete button
        deleteButton.visibility = View.GONE

        // Retrieve chosen Profile Picture
        val savedImageResId = sharedPreferences.getInt("selectedImageResId", R.drawable.pfp_1)
        val profilePicImageView = findViewById<ImageView>(R.id.profilePic)

        profilePicImageView.setImageResource(savedImageResId)

        val userUid = FirebaseAuth.getInstance().currentUser?.uid
        val folderReference = FirebaseStorage.getInstance().getReference().child("images/$userUid")
        val imageContainer = findViewById<LinearLayout>(R.id.imageContainer)

        folderReference.listAll()
            .addOnSuccessListener{ result ->
                val items = result.items.sortedByDescending { it.name }
                items.forEach { item ->
                    item.downloadUrl
                        .addOnSuccessListener { uri ->
                            val imageUrl = uri.toString()
                            imageUrls.add(imageUrl)

                            imageOrderList.add(imageUrl)

                            val imageView = ImageView(this)
                            val layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            )
                            layoutParams.width = 675
                            layoutParams.height = layoutParams.width
                            layoutParams.gravity = Gravity.CENTER_VERTICAL
                            imageView.layoutParams = layoutParams
                            imageView.scaleType = ImageView.ScaleType.CENTER_CROP
                            layoutParams.setMargins(5,10,5,0)

                            Picasso.get().load(uri).rotate(90f).into(imageView)
                            imageContainer.addView(imageView,0)

                            // open image using gallery app
                            imageView.setOnClickListener {
                                val galleryIntent = Intent(Intent.ACTION_VIEW, uri)
                                galleryIntent.setDataAndType(uri, "image/*")
                                startActivity(galleryIntent)
                            }

                            imageView.setOnLongClickListener{
                                deleteButton.visibility = View.VISIBLE
                                true
                            }

                            deleteButton.setOnClickListener {
                                // Show confirmation dialog and handle deletion
                                showDeleteConfirmationDialog(uri, imageView)
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Error downloading image: ${e.message}", e)
                        }
                }
                saveImageOrderToSharedPreferences()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error listing files in folder: ${e.message}", e)
            }

        val databaseReference = FirebaseDatabase.getInstance().getReference("UsersData/$userUid/Profile")

        data class Profile (
            val Bio: String = "",
            val Name: String = ""
        ) {
            constructor() : this("","")
        }

        databaseReference.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    val profile = dataSnapshot.getValue(Profile::class.java)
                    if (profile != null && profile.Bio.isNotEmpty()) {
                        editTextBio.setText(profile.Bio)
                    }
                    if (profile != null && profile.Name.isNotEmpty()) {
                        editTextName.setText(profile.Name)
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("Firebase", "Error getting data", databaseError.toException())
            }
        })


    }
    companion object {
        private const val TAG = "ProfileActivity"
    }

    private fun saveImageOrderToSharedPreferences() {
        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("imageOrder", imageOrderList.joinToString(separator = ","))
        editor.apply()
    }

    override fun onResume() {
        super.onResume()
        // Retrieve the image order list from SharedPreferences and restore the order
        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val imageContainer = findViewById<LinearLayout>(R.id.imageContainer)

        val imageOrder = sharedPreferences.getString("imageOrder", "")
        if (imageOrder != null && imageOrder.isNotEmpty()) {
            imageOrderList.clear()
            imageOrderList.addAll(imageOrder.split(","))
            // Reorder the images based on the retrieved order list
            imageOrderList.forEach { imageUrl ->
                // Find the corresponding ImageView and move it to the top of the LinearLayout
                val index = imageUrls.indexOf(imageUrl)
                if (index != -1) {
                    val imageView = imageContainer.getChildAt(index) as? ImageView
                    imageView?.let {
                        imageContainer.removeViewAt(index)
                        imageContainer.addView(it, 0)
                    }
                }
            }
        }
    }

    private fun showDeleteConfirmationDialog(imageUri: Uri, imageView: ImageView) {
        val deleteButton = findViewById<Button>(R.id.Delete_btn)

        AlertDialog.Builder(this)
            .setTitle("Delete Image")
            .setMessage("Are you sure you want to delete this image?")
            .setPositiveButton("Yes") { _, _ ->
                // User clicked Yes, delete the image
                deleteImage(imageUri, imageView)
            }
            .setNegativeButton("No", null)
            .show()
        deleteButton.visibility = View.GONE
    }

    private fun deleteImage(imageUri: Uri, imageView: ImageView) {
        val imageContainer = findViewById<LinearLayout>(R.id.imageContainer)
        val deleteButton = findViewById<Button>(R.id.Delete_btn)
        val storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(imageUri.toString())
        storageReference.delete()
            .addOnSuccessListener {
                imageUrls.remove(imageUri.toString())
                refreshActivity()

                imageContainer.removeView(imageView)
                deleteButton.visibility = View.GONE
                Toast.makeText(this, "Image deleted successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                // Failed to delete file
                Log.e(TAG, "Error deleting image: ${e.message}", e)
                Toast.makeText(this, "Failed to delete image", Toast.LENGTH_SHORT).show()
            }
    }

    private fun refreshActivity() {
        recreate()
    }

    fun goToMain(view: View) {
        val intent = Intent(this, MainActivity::class.java)

        val options = ActivityOptions.makeCustomAnimation(this,
            R.anim.slide_enter_right, //Entrance animation
            R.anim.slide_exit_left //Exit animation
        )

        startActivity(intent, options.toBundle())
    }

    fun goToProfileEdit(view: View) {
        val intent = Intent(this, ProfileEditActivity::class.java)

        val options = ActivityOptions.makeCustomAnimation(this,
            R.anim.slide_enter_top,
            R.anim.slide_exit_bottom
        )

        startActivity(intent, options.toBundle())
    }


}
