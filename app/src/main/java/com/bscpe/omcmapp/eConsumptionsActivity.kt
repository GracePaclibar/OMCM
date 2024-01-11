package com.bscpe.omcmapp

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity

class eConsumptionsActivity : AppCompatActivity( ){
    override fun onCreate(savedInstances: Bundle?) {
        super.onCreate(savedInstances)
        setContentView(R.layout.activity_e_consumptions)
    }

    fun goToMain(view: View) {
        val intent = Intent(this, MainActivity::class.java)

        val options = ActivityOptions.makeCustomAnimation(this,
            R.anim.slide_enter_top, //Entrance animation
            R.anim.slide_exit_bottom //Exit animation
        )

        startActivity(intent, options.toBundle())
    }

    fun goToWater(view: View) {
        val intent = Intent(this, wConsumptionsActivity::class.java)

        val options = ActivityOptions.makeCustomAnimation(this,
            R.anim.slide_enter_right,
            R.anim.slide_exit_left
        )

        startActivity(intent, options.toBundle())
    }
}