package io.github.t3r1jj.ips.research

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.BottomSheetDialog
import android.support.v4.widget.NestedScrollView
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import io.github.t3r1jj.ips.research.view.InfoView
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {
    companion object {
        val licenses = listOf(
                InfoView.Model("Weka", "University of Waikato", "GPL 3.0"),
                InfoView.Model("Anvil", "https://github.com/zserge/anvil", "MIT"),
                InfoView.Model("MPAndroidChart", "Philipp Jahoda", "Apache 2.0"),
                InfoView.Model("android-filepicker", "Angad Singh", "Apache 2.0"),
                InfoView.Model("couchbase-lite-android", "Couchbase", "Apache 2.0"),
                InfoView.Model("couchbase-lite-android", "Couchbase", "Apache 2.0"),
                InfoView.Model("Android + Kotlin"),
                InfoView.Model("Tested with JUnit + Mockito + Espresso"),
                InfoView.Model("Generated test cases can be reviewed in Weka (.arff) or Scilab (.sci)")
        )
    }

    private var bottomSheet: BottomSheetDialog? = null

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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.info_item -> showInfo()
        }
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        if (bottomSheet?.isShowing == true) {
            bottomSheet!!.dismiss()
        }
    }

    private fun showInfo() {
        bottomSheet = BottomSheetDialog(this)
        val scrollView = NestedScrollView(this)
        scrollView.addView(InfoView(this))
        bottomSheet!!.setContentView(scrollView)
        bottomSheet!!.show()
    }

}
