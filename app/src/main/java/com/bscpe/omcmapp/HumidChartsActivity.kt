package com.bscpe.omcmapp

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity

class HumidChartsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_humidcharts)

        supportFragmentManager.beginTransaction()
            .replace(R.id.humidFragmentContainer, HumidChartFragment())
            .replace(R.id.humidIntEnvFragmentContainer, HumidIntEnvFragment())
            .replace(R.id.humidExtEnvFragmentContainer, HumidExtEnvFragment())

            .commit()
    }

    fun goToMain(view: View) {
        val intent = Intent(this, MainActivity::class.java)

        val options = ActivityOptions.makeCustomAnimation(this,
            R.anim.slide_enter_left,
            R.anim.slide_exit_right
        )

        startActivity(intent, options.toBundle())
    }

    fun goToHumidData(view: View) {
        val intent = Intent(this, HumidTableActivity::class.java)

        val options = ActivityOptions.makeCustomAnimation(this,
            R.anim.slide_enter_right,
            R.anim.slide_exit_left
        )

        startActivity(intent, options.toBundle())
    }
}