package io.github.t3r1jj.ips.collector

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import io.github.t3r1jj.ips.collector.model.Dao
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        inertial_button.setOnClickListener {
            startActivity(
                    Intent(this, InertialActivity::class.java)
            )
        }
        magnetic_button.setOnClickListener {
            startActivity(
                    Intent(this, MagneticActivity::class.java)
            )
        }
        wifi_button.setOnClickListener {
            startActivity(
                    Intent(this, WifiActivity::class.java)
            )
        }
        show_db_button.setOnClickListener {
            startActivity(
                    Intent(this, DatabaseActivity::class.java)
            )
        }
        test_button.setOnClickListener {
            startActivity(
                    Intent(this, OnlineActivity::class.java)
            )
        }
    }
}
