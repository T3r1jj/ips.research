package io.github.t3r1jj.ips.collector

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.os.Environment
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.widget.LinearLayout.HORIZONTAL
import android.widget.LinearLayout.VERTICAL
import android.widget.Toast
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.t3r1jj.ips.collector.model.Dao
import io.github.t3r1jj.ips.collector.view.RenderableView
import trikita.anvil.Anvil
import trikita.anvil.BaseDSL.MATCH
import trikita.anvil.BaseDSL.WRAP
import trikita.anvil.DSL.*
import trikita.anvil.RenderableAdapter
import java.io.File
import java.util.*
import android.net.ConnectivityManager
import java.net.InetAddress


class DatabaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dao = Dao(this)

        setContentView(object : RenderableView(this) {
            override fun view() {

                linearLayout {
                    size(MATCH, MATCH)
                    orientation(VERTICAL)
                    listView {
                        size(MATCH, MATCH)
                        weight(1f)
                        adapter(RenderableAdapter.withItems(dao.findAll().map { it.key to it.value }, { i, item ->
                            linearLayout {
                                padding(dip(20))
                                textView {
                                    text(item.second.toString())
                                    onLongClick {
                                        val builder = AlertDialog.Builder(context)
                                        builder.setMessage("Delete record for created on: " + item.second.timestamp)
                                                .setPositiveButton("Yes", { dialog, which ->
                                                    if (which == DialogInterface.BUTTON_POSITIVE) {
                                                        dao.delete(item.first)
                                                        Anvil.render()
                                                    }
                                                }).setNegativeButton("No", { dialog, which ->
                                        }).show()
                                        true
                                    }
                                }
                            }
                        }))
                    }
                    button {
                        size(MATCH, WRAP)
                        text("Clear")
                        onClick {
                            dao.clear()
                        }
                    }
                    linearLayout {
                        size(MATCH, WRAP)
                        orientation(HORIZONTAL)
                        button {
                            size(0, WRAP)
                            text("Download")
                            weight(0.5f)
                            onClick {
                                if (!isExternalStorageWritable()) {
                                    Toast.makeText(this@DatabaseActivity, "External storage not available", Toast.LENGTH_LONG).show()
                                } else {
                                    val fileName = "ips.data." + Date().time.toString() + ".json"
                                    val file = getPublicDownloadStorageFile(fileName)
                                    try {
                                        ObjectMapper().writeValue(file.outputStream(), dao.findAll().values)
                                        Toast.makeText(this@DatabaseActivity, "Saved json file to: " + file.absolutePath, Toast.LENGTH_LONG).show()
                                    } catch (ex: RuntimeException) {
                                        Toast.makeText(this@DatabaseActivity, "Error: " + ex.toString(), Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        }
                        button {
                            size(0, WRAP)
                            text("Push to remote db")
                            weight(0.5f)
                            onClick {
                                Toast.makeText(this@DatabaseActivity, "Started replication in the background", Toast.LENGTH_SHORT).show()
                                checkIfDbReachable()
                                try {
                                    dao.replicate()
                                } catch (ex: RuntimeException) {
                                    Toast.makeText(this@DatabaseActivity, "Error: " + ex.toString(), Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                }
            }

        })

        Toast.makeText(this, "Long press on record for single removal", Toast.LENGTH_SHORT).show()
    }

    fun isExternalStorageWritable(): Boolean {
        return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
    }

    fun getPublicDownloadStorageFile(fileName: String): File {
        val file = File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS), fileName)
        file.createNewFile()
        return file
    }

    private fun checkIfDbReachable() {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        val command = "ping -c 1 " + Dao.DB_ROOT_URL
        Thread({
            if (activeNetworkInfo == null || !activeNetworkInfo.isConnected || Runtime.getRuntime().exec(command).waitFor() != 0) {
                runOnUiThread {
                    Toast.makeText(this@DatabaseActivity, "Error: Remote DB is not reachable, check internet connection > try again; if fails > notify app author", Toast.LENGTH_LONG).show()
                }
            }
        }).start()
    }

}