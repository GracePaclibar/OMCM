package com.bscpe.omcmapp

import android.content.ContentValues.TAG
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.bscpe.omcmapp.databinding.ActivityLoginBinding
import com.bscpe.omcmapp.databinding.ActivitySignUpBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var firebaseDatabase: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = Firebase.auth
        firebaseDatabase = FirebaseDatabase.getInstance()
        databaseReference = firebaseDatabase.reference.child("users")

        binding.loginButton.setOnClickListener{
            val loginUsername = binding.loginUsername.text.toString()
            val loginPassword = binding.loginPassword.text.toString()

            if (loginUsername.isNotEmpty() && loginPassword.isNotEmpty()){
                //loginUser(loginUsername, loginPassword)
                auth.signInWithEmailAndPassword(loginUsername, loginPassword)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithEmail:success")
                            val user = auth.currentUser
                            Toast.makeText(this@LoginActivity, "Login Successful!", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                            finish()
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithEmail:failure", task.exception)
                            Toast.makeText(this@LoginActivity, "Login Failed!", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                Toast.makeText(this@LoginActivity, "All fields are required.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.signupRedirect.setOnClickListener {
            startActivity(Intent(this@LoginActivity, SignUpActivity::class.java))
            finish()
        }
    }

    public override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = auth.currentUser
        if (currentUser != null) {
            Toast.makeText(this@LoginActivity, "Login Successful!", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this@LoginActivity, MainActivity::class.java))
            finish()
        }
    }

    private fun loginUser(username: String, password: String){
        databaseReference.orderByChild("username").equalTo(username).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()){
                    for (userSnapshot in dataSnapshot.children){
                        val userData = userSnapshot.getValue(UserData::class.java)

                        if (userData != null && userData.password == password){
                            Toast.makeText(this@LoginActivity, "Login Successful!", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                            finish()
                            return
                        }
                    }
                }
                Toast.makeText(this@LoginActivity, "Login Failed!", Toast.LENGTH_SHORT).show()
            }
            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(this@LoginActivity, "Database Error: ${databaseError.message}!", Toast.LENGTH_SHORT).show()
            }
        })
    }
}