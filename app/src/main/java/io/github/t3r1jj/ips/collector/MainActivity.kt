package io.github.t3r1jj.ips.collector

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
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
    }
}
