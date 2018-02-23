package io.github.t3r1jj.ips.collector.model

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.util.Log
import android.widget.Adapter
import io.github.t3r1jj.ips.collector.WifiActivity
import trikita.anvil.Anvil
import trikita.anvil.DSL
import trikita.anvil.RenderableAdapter
import java.util.*


class Sampler(val context: Context) {


    val fingerprints = mutableListOf<Fingerprint>()
    val wifiScanReceiver = WifiScanReceiver()
    var samplingRate = WifiActivity.SamplingRate._1000MS
    var sampleCount = 10
    var finished = false
    var started = false
    var sampleIndex = 0

    private val wifiManager: WifiManager
        get() {
            return context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        }

    fun startSampling() {
        if (!wifiManager.isWifiEnabled) {
            throw RuntimeException("WiFi not enabled")
        }
        fingerprints.clear()
        wifiManager.scanResults.clear()
        sampleIndex = 0
        context.registerReceiver(wifiScanReceiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
        if (!wifiManager.startScan()) {
            throw RuntimeException("startScan() fail")
        } else {
            started = true
            finished = false
        }
    }

    fun stopSampling() {
        try {
            context.unregisterReceiver(wifiScanReceiver)
        } catch (err: IllegalArgumentException) {
            Log.v(javaClass.name, "Could not unregister receiver since it was not registered")
        }
        started = false
    }

    private fun createFingerprint(it: ScanResult): Fingerprint {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
            Fingerprint(it.BSSID, it.level, it.timestamp)
        } else {
            Fingerprint(it.BSSID, it.level, Date().time)
        }
    }

    inner class WifiScanReceiver : BroadcastReceiver() {

        override fun onReceive(c: Context, intent: Intent) {
            sampleIndex++
            println("\nNumber of Wifi Connections: " + wifiManager.scanResults.size + "\n")
            wifiManager.scanResults.forEach {
                println(it.toString())
                val fingerprint = createFingerprint(it)
                fingerprints.add(fingerprint)
            }
            Thread.sleep(samplingRate.delay)
            updateAdapter()
            Anvil.render()
            if (sampleIndex < sampleCount) {
                wifiManager.scanResults.clear()
                if (!wifiManager.startScan()) {
                    throw RuntimeException("startScan() fail")
                }
            } else {
                stopSampling()
                finished = true
            }
        }
    }

    data class Fingerprint(val bssid: String, val rssi: Int, val timestamp: Long)

    var infoAdapter = createAdapter()

    fun updateAdapter() {
        infoAdapter = createAdapter()
    }

    private fun createAdapter(): RenderableAdapter {
        return RenderableAdapter.withItems(fingerprints, { i, item ->
            DSL.linearLayout {
                DSL.textView {
                    DSL.text(item.toString())
                }
            }
        })
    }
}