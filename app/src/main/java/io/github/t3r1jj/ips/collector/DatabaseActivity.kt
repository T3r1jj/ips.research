package io.github.t3r1jj.ips.collector

import android.graphics.Typeface
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.widget.LinearLayout.HORIZONTAL
import android.widget.LinearLayout.VERTICAL
import android.widget.Toast
import io.github.t3r1jj.ips.collector.model.Dao
import trikita.anvil.BaseDSL.MATCH
import trikita.anvil.BaseDSL.WRAP
import trikita.anvil.DSL
import trikita.anvil.DSL.*
import trikita.anvil.RenderableAdapter
import android.content.DialogInterface
import android.os.Environment
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.t3r1jj.ips.collector.view.RenderableView
import trikita.anvil.Anvil
import java.io.File
import java.util.*


class DatabaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dao = Dao(this)

        setContentView(object : RenderableView(this) {
            override fun view() {

                linearLayout {
                    size(MATCH, MATCH)
                    orientation(VERTICAL)
                    textView {
                        text("Data collected:")
                        gravity(CENTER_HORIZONTAL)
                        typeface(null, Typeface.BOLD)
                    }
                    listView {
                        size(MATCH, MATCH)
                        weight(1f)
                        adapter(RenderableAdapter.withItems(dao.findAll().map { it.key to it.value }, { i, item ->
                            DSL.linearLayout {
                                DSL.textView {
                                    DSL.text(item.second.toString())
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
                            text("Sync")
                            weight(0.5f)
                        }
                        button {
                            size(0, WRAP)
                            text("Download")
                            weight(0.5f)
                            onClick {
                                if (!isExternalStorageWritable()) {
                                    Toast.makeText(this@DatabaseActivity, "External storage not available", Toast.LENGTH_LONG).show()
                                } else {
                                    val fileName = "ips.data." + Date().time.toString() + ".json"
                                    val file = getPublicDownloadStorageDir(fileName)
                                    ObjectMapper().writeValue(file.outputStream(), dao.findAll().values)
                                    Toast.makeText(this@DatabaseActivity, "Saved json file to: " + file.absolutePath, Toast.LENGTH_LONG).show()
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

    fun getPublicDownloadStorageDir(fileName: String): File {
        return File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS), fileName)
    }


}