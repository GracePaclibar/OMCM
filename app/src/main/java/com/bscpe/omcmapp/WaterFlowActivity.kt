package com.bscpe.omcmapp

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity

class WaterFlowActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_waterflow)

        supportFragmentManager.beginTransaction()
            .replace(R.id.waterFlowFragmentContainer, WaterChartFragment())
            .replace(R.id.waterEnvFragmentContainer, WaterEnvFragment())

            .commit()
    }

    fun goToMain(view : View) {
        val intent = Intent(this, MainActivity::class.java)

        val options = ActivityOptions.makeCustomAnimation(this,
            R.anim.slide_enter_left,
            R.anim.slide_exit_right
        )

        startActivity(intent, options.toBundle())
    }

}