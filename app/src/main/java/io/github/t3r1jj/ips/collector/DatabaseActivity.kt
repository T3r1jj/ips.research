package io.github.t3r1jj.ips.collector

import android.Manifest
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.widget.Adapter
import android.widget.LinearLayout.VERTICAL
import android.widget.Toast
import com.couchbase.lite.replicator.Replication
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.angads25.filepicker.model.DialogConfigs
import com.github.angads25.filepicker.model.DialogProperties
import com.github.angads25.filepicker.view.FilePickerDialog
import io.github.t3r1jj.ips.collector.model.Dao
import io.github.t3r1jj.ips.collector.model.data.Dataset
import io.github.t3r1jj.ips.collector.view.RenderableView
import trikita.anvil.Anvil
import trikita.anvil.BaseDSL.MATCH
import trikita.anvil.BaseDSL.WRAP
import trikita.anvil.DSL.*
import trikita.anvil.RenderableAdapter
import java.io.File
import java.util.*


class DatabaseActivity : AppCompatActivity() {
    companion object {
        private const val EXTERNAL_PERMISSION_CODE = 1
    }

    private var loadingCheckThread: Thread? = null
    private var uploadingCheckThread: Thread? = null
    private var dialog: FilePickerDialog? = null
    private lateinit var dbAdapter: Adapter

    val dao: Dao by lazy {
        Dao(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        recreateAdapter()
        setContentView(object : RenderableView(this) {
            override fun view() {

                linearLayout {
                    size(MATCH, MATCH)
                    orientation(VERTICAL)
                    listView {
                        size(MATCH, MATCH)
                        weight(1f)
                        adapter(dbAdapter)
                    }
                    button {
                        size(MATCH, WRAP)
                        text("Load")
                        onClick {
                            AlertDialog.Builder(this@DatabaseActivity)
                                    .setMessage("From where to load the data?")
                                    .setPositiveButton("Device", { _, which ->
                                        if (which == DialogInterface.BUTTON_POSITIVE) {
                                            showFileChooser()
                                        }
                                    }).setNegativeButton("Remote database", { _, which ->
                                if (which == DialogInterface.BUTTON_NEGATIVE) {
                                    loadFromRemote()
                                }
                            }).show()
                        }
                    }
                    button {
                        size(MATCH, WRAP)
                        text("Clear")
                        onClick {
                            AlertDialog.Builder(this@DatabaseActivity)
                                    .setMessage("Do you want to delete all of the records from the db?")
                                    .setPositiveButton("Yes", { _, which ->
                                        if (which == DialogInterface.BUTTON_POSITIVE) {
                                            dao.clear()
                                            recreateAdapter()
                                            Anvil.render()
                                        }
                                    }).setNegativeButton("No", { _, _ ->
                            }).show()
                        }
                    }

                    button {
                        size(MATCH, WRAP)
                        text("Save")
                        onClick {
                            AlertDialog.Builder(this@DatabaseActivity)
                                    .setMessage("Where to save the data?")
                                    .setPositiveButton("Device", { _, which ->
                                        if (which == DialogInterface.BUTTON_POSITIVE) {
                                            saveDataToDevice()
                                        }
                                    }).setNegativeButton("Remote database", { _, which ->
                                if (which == DialogInterface.BUTTON_NEGATIVE) {
                                    saveDataToRemote()
                                }
                            }).show()

                        }
                    }

                    button {
                        size(MATCH, WRAP)
                        text("To ARFF")
                        onClick {

                        }
                    }
                }
            }

            override fun onAttachedToWindow() {
                super.onAttachedToWindow()
                Toast.makeText(this@DatabaseActivity, "Long press on record for single removal", Toast.LENGTH_SHORT).show()
            }
        })

    }

    private fun recreateAdapter() {
        dbAdapter = RenderableAdapter.withItems(dao.findAll().map { it.key to it.value }, { _, item ->
            linearLayout {
                padding(dip(20))
                textView {
                    text(item.second.toString())
                    onLongClick {
                        openRemovalDialog(item)
                        true
                    }
                }
                onLongClick {
                    openRemovalDialog(item)
                    true
                }
            }
        })
    }

    private fun loadFromRemote() {
        checkIfDbReachable()
        try {
            val pull = dao.pull()
            loadingCheckThread = createAndStartCheckThread(loadingCheckThread, pull, "Loading finished")
        } catch (ex: RuntimeException) {
            Toast.makeText(this@DatabaseActivity, "Error: " + ex.toString(), Toast.LENGTH_LONG).show()
        }
    }

    private fun saveDataToRemote() {
        Toast.makeText(this@DatabaseActivity, "Started replication in the background", Toast.LENGTH_SHORT).show()
        checkIfDbReachable()
        try {
            val push = dao.push()
            uploadingCheckThread = createAndStartCheckThread(uploadingCheckThread, push, "Uploading finished")
        } catch (ex: RuntimeException) {
            Toast.makeText(this@DatabaseActivity, "Error: " + ex.toString(), Toast.LENGTH_LONG).show()
        }
    }

    private fun createAndStartCheckThread(oldThread: Thread?, replication: Replication, text: String): Thread {
        if (oldThread != null) {
            oldThread.interrupt()
            oldThread.join()
        }
        val thread = Thread({
            try {
                while (replication.isRunning) {
                    Thread.sleep(1000)
                }
                if (replication.isPull) {
                    recreateAdapter()
                    Anvil.render()
                }
                runOnUiThread {
                    Toast.makeText(this@DatabaseActivity, text, Toast.LENGTH_LONG).show()
                }
            } catch (iex: InterruptedException) {
            }
        })
        thread.start()
        return thread
    }

    private fun saveDataToDevice() {
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

    private fun openRemovalDialog(item: Pair<String, Dataset>) {
        AlertDialog.Builder(this)
                .setMessage("Do you want to delete record: " + item.second.toString() + "?")
                .setPositiveButton("Yes", { _, which ->
                    if (which == DialogInterface.BUTTON_POSITIVE) {
                        dao.delete(item.first)
                        Anvil.render()
                    }
                }).setNegativeButton("No", { _, _ ->
        }).show()
    }

    private fun isExternalStorageWritable(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                    PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), EXTERNAL_PERMISSION_CODE)
                return false
            }
        }
        return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
    }

    private fun getPublicDownloadStorageFile(fileName: String): File {
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

    private fun showFileChooser() {
        val properties = DialogProperties()
        properties.selection_mode = DialogConfigs.SINGLE_MODE
        properties.selection_type = DialogConfigs.FILE_SELECT
        properties.root = File(DialogConfigs.DEFAULT_DIR)
        properties.error_dir = File(DialogConfigs.DEFAULT_DIR)
        properties.offset = File(DialogConfigs.DEFAULT_DIR)
        properties.extensions = null
        dialog = FilePickerDialog(this, properties)
        dialog!!.setTitle("Select a IPS json data file")
        dialog!!.setDialogSelectionListener {
            dao.saveAll(File(it[0]))
            recreateAdapter()
            Anvil.render()
        }
        dialog!!.show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            EXTERNAL_PERMISSION_CODE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    saveDataToDevice()
                } else {
                    Toast.makeText(this@DatabaseActivity, "Please grant the permission to use external storage in order to save the data to json file", Toast.LENGTH_LONG).show()
                }
                return
            }

        }
    }

    override fun onPause() {
        super.onPause()
        if (loadingCheckThread != null) {
            loadingCheckThread!!.interrupt()
        }
        if (uploadingCheckThread != null) {
            uploadingCheckThread!!.interrupt()
        }
        if (dialog?.isShowing == true) {
            dialog!!.dismiss()
        }
    }

}