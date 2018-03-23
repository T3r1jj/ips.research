package io.github.t3r1jj.ips.research

import android.Manifest
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Environment
import android.support.design.widget.BottomSheetDialog
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.widget.Adapter
import android.widget.LinearLayout.HORIZONTAL
import android.widget.LinearLayout.VERTICAL
import android.widget.Toast
import com.couchbase.lite.replicator.Replication
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.angads25.filepicker.model.DialogConfigs
import com.github.angads25.filepicker.model.DialogProperties
import com.github.angads25.filepicker.view.FilePickerDialog
import io.github.t3r1jj.ips.research.model.Dao
import io.github.t3r1jj.ips.research.model.algorithm.ArffTransform
import io.github.t3r1jj.ips.research.model.algorithm.filter.FilterFactory
import io.github.t3r1jj.ips.research.model.data.Dataset
import io.github.t3r1jj.ips.research.model.data.DatasetType
import io.github.t3r1jj.ips.research.model.data.InertialDataset
import io.github.t3r1jj.ips.research.model.data.WifiDataset
import io.github.t3r1jj.ips.research.model.test.PedometerTester
import io.github.t3r1jj.ips.research.model.test.WekaPreTester
import io.github.t3r1jj.ips.research.view.ArffDialog
import io.github.t3r1jj.ips.research.view.PedometerBottomSheetDialog
import io.github.t3r1jj.ips.research.view.PedometerDialog
import io.github.t3r1jj.ips.research.view.RenderableView
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
    private var filePickerDialog: FilePickerDialog? = null
    private lateinit var dbAdapter: Adapter
    internal var userInputDialog: Dialog? = null
    internal var outputDialog: Dialog? = null
    internal var filterFactory = FilterFactory(FilterFactory.FilterType.NO_FILTER)
    internal lateinit var tester: PedometerTester

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
                    linearLayout {
                        size(MATCH, WRAP)
                        orientation(HORIZONTAL)
                        button {
                            size(0, WRAP)
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
                            weight(0.5f)
                        }
                        button {
                            size(0, WRAP)
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
                            weight(0.5f)
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


                    linearLayout {
                        size(MATCH, WRAP)
                        orientation(HORIZONTAL)
                        button {
                            size(0, WRAP)
                            text("WiFi to ARFF")
                            onClick {
                                if (!isExternalStorageWritable()) {
                                    Toast.makeText(this@DatabaseActivity, "External storage not available", Toast.LENGTH_LONG).show()
                                } else if (dao.findAll().isEmpty()) {
                                    Toast.makeText(this@DatabaseActivity, "No data collected", Toast.LENGTH_LONG).show()
                                } else {
                                    userInputDialog = AlertDialog.Builder(context).setView(ArffDialog(this@DatabaseActivity, this@DatabaseActivity)).show()
                                }
                            }
                            weight(0.5f)
                        }
                        button {
                            size(0, WRAP)
                            text("Pedometer test")
                            onClick {
                                userInputDialog = AlertDialog.Builder(context).setView(PedometerDialog(this@DatabaseActivity, this@DatabaseActivity)).show()
                            }
                            weight(0.5f)
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


    override fun onDestroy() {
        super.onDestroy()
        if (userInputDialog?.isShowing == true) {
            userInputDialog?.dismiss()
        }
        if (outputDialog?.isShowing == true) {
            outputDialog?.dismiss()
        }
    }

    private fun recreateAdapter() {
        dbAdapter = RenderableAdapter.withItems(dao.findAll()
                .toList()
                .sortedBy { it.second.timestamp }
                .map { it.first to it.second }, { _, item ->
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
                        recreateAdapter()
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
        filePickerDialog = FilePickerDialog(this, properties)
        filePickerDialog!!.setTitle("Select a IPS json data file")
        filePickerDialog!!.setDialogSelectionListener {
            dao.saveAll(File(it[0]))
            recreateAdapter()
            Anvil.render()
        }
        filePickerDialog!!.show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            EXTERNAL_PERMISSION_CODE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    Toast.makeText(this@DatabaseActivity, "Please repeat the last action", Toast.LENGTH_LONG).show()
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
        if (filePickerDialog?.isShowing == true) {
            filePickerDialog!!.dismiss()
        }
    }

    fun onPedometerDebugClick() {
        if (!isExternalStorageWritable()) {
            Toast.makeText(this, "External storage not available", Toast.LENGTH_LONG).show()
        } else {
            val fileName = "ips.inertial.test.debug." + System.currentTimeMillis().toString() + "." + getFormattedFilterType() + ".sce"
            val file = getPublicDownloadStorageFile(fileName)
            tester.generateDebug(inertialData(), file.outputStream())
            Toast.makeText(this, "Saved file to: " + file.absolutePath, Toast.LENGTH_LONG).show()
        }
    }

    fun onPedometerOutputClick() {
        if (!isExternalStorageWritable()) {
            Toast.makeText(this, "External storage not available", Toast.LENGTH_LONG).show()
        } else {
            val fileName = "ips.inertial.test.output." + System.currentTimeMillis().toString() + "." + getFormattedFilterType() + ".txt"
            val file = getPublicDownloadStorageFile(fileName)
            tester.saveOutput(file.outputStream())
            Toast.makeText(this, "Saved file to: " + file.absolutePath, Toast.LENGTH_LONG).show()
        }
    }

    fun onPedometerInfoClick() {
        if (!isExternalStorageWritable()) {
            Toast.makeText(this, "External storage not available", Toast.LENGTH_LONG).show()
        } else {
            val fileName = "ips.inertial.test.info." + System.currentTimeMillis().toString() + "." + getFormattedFilterType() + ".txt"
            val file = getPublicDownloadStorageFile(fileName)
            tester.saveOutputInfo(file.outputStream())
            Toast.makeText(this, "Saved file to: " + file.absolutePath, Toast.LENGTH_LONG).show()
        }
    }

    private fun getFormattedFilterType(): String {
        return if (filterFactory.filterType == FilterFactory.FilterType.MOVING_AVERAGE_FILTER) {
            filterFactory.filterType.toString() + "-" + filterFactory.averagingWindowLength
        } else {
            filterFactory.filterType.toString()
        }
    }

    internal fun generateArff(regex: String, opts: ArffTransform.Options, trainData: List<WifiDataset>) {
        val aff = ArffTransform(Regex(regex, RegexOption.IGNORE_CASE), opts)
        val wifiData = wifiData()
        val testData = wifiData.filter { superIt -> trainData.firstOrNull { it.timestamp == superIt.timestamp } == null }
        aff.apply(trainData, testData)
        try {
            var file: File?
            val filePaths = mutableListOf<String>()
            val time = System.currentTimeMillis()
            for (dev in aff.testDevices) {
                val fileName = "ips.wifi.test." + formatFileName(dev, time) + ".arff"
                file = getPublicDownloadStorageFile(fileName)
                aff.writeToFile(file.outputStream(), dev)
                filePaths.add(file.absolutePath)
            }
            val fileName = "ips.wifi." + formatFileName(aff.trainDevices, time) + ".arff"
            file = getPublicDownloadStorageFile(fileName)
            aff.writeToFile(file.outputStream(), aff.trainDevices)
            filePaths.add(file.absolutePath)
            Toast.makeText(this@DatabaseActivity, "Generated ARFF files to: " + filePaths.joinToString("\n", "\n"), Toast.LENGTH_LONG).show()
        } catch (ex: Exception) {
            Toast.makeText(this@DatabaseActivity, "Error: " + ex.toString(), Toast.LENGTH_LONG).show()
        }
    }

    internal fun testArff(regex: String, opts: ArffTransform.Options, trainData: List<WifiDataset>) {
        val aff = ArffTransform(Regex(regex, RegexOption.IGNORE_CASE), opts)
        val wifiData = wifiData()
        val testData = wifiData.filter { superIt -> trainData.firstOrNull { it.timestamp == superIt.timestamp } == null }
        aff.apply(trainData, testData)
        val tester = WekaPreTester(aff)
        val text = "Weka pre test accuracy:\n" + tester.knnTest().joinToString("%\n\t", "kNN:\n\t", "%\n") +
                tester.customTest().joinToString("%\n\t", "Custom:\n\t", "%\n\n")
        val bottomSheet = BottomSheetDialog(this)
        outputDialog = bottomSheet
        bottomSheet.setContentView(PedometerBottomSheetDialog(this, this, text, false))
        bottomSheet.show()
    }

    internal fun wifiData(): List<WifiDataset> {
        return dao.findAll().values
                .filter { it.type == DatasetType.WIFI }
                .map { it as WifiDataset }
    }

    private fun formatFileName(name: String, time: Long): String {
        return name
                .replace(" ", "-")
                .replace(",", "_")
                .substring(0, Math.min(50, name.length))
                .plus(".")
                .plus(time.toString())
    }

    fun inertialData(): Iterable<InertialDataset> {
        return dao.findAll().values
                .filter { it.type == DatasetType.INERTIAL }
                .map { it as InertialDataset }
    }

}