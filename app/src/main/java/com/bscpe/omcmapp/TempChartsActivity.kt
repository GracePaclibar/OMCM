package com.bscpe.omcmapp

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity

class TempChartsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tempcharts)

        supportFragmentManager.beginTransaction()
            .replace(R.id.tempFragmentContainer, TempChartFragment())
            .replace(R.id.tempIntEnvFragmentContainer, TempIntEnvFragment())
            .replace(R.id.tempExtEnvFragmentContainer, TempExtEnvFragment())

            .commit()
    }

    fun goToMain(view: View) {
        val intent = Intent(this, MainActivity::class.java)

        val options = ActivityOptions.makeCustomAnimation(this,
            R.anim.slide_enter_left, //Entrance animation
            R.anim.slide_exit_right //Exit animation
        )

        startActivity(intent, options.toBundle())
    }

    fun goToTempTab(view: View) {
        val intent = Intent(this, TempTabularDataActivity::class.java)

        val options = ActivityOptions.makeCustomAnimation(this,
            R.anim.slide_enter_right, //Entrance animation
            R.anim.slide_exit_left //Exit animation
        )

        startActivity(intent, options.toBundle())
    }
}