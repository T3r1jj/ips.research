package io.github.t3r1jj.ips.research.model.collector

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.util.Log
import io.github.t3r1jj.ips.research.WifiActivity
import io.github.t3r1jj.ips.research.model.data.Fingerprint
import trikita.anvil.Anvil
import java.util.*


class WifiSampler(val context: Context) {


    private val wifiScanReceiver = WifiScanReceiver()
    val fingerprints = mutableListOf<Fingerprint>()
    var samplingRate = WifiActivity.SamplingRate._500MS
    var sampleCount = 10
    var running = false
        set(value) {
            field = value
            Anvil.render()
        }
    var sampleIndex = 0
        set(value) {
            field = value
            Anvil.render()
        }
    var listener : WifiFingerprintListener? = null

    private val wifiManager: WifiManager
        get() {
            return context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        }

    fun startSampling() {
        stopSampling()
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
            running = true
        }
    }

    fun stopSampling() {
        try {
            context.unregisterReceiver(wifiScanReceiver)
        } catch (err: IllegalArgumentException) {
            Log.v(javaClass.name, "Could not unregister receiver since it was not registered")
        }
        running = false
    }

    private fun createFingerprint(it: ScanResult): Fingerprint {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
            Fingerprint(it.BSSID, it.level, it.timestamp, it.SSID)
        } else {
            Fingerprint(it.BSSID, it.level, Date().time, it.SSID)
        }
    }

    inner class WifiScanReceiver : BroadcastReceiver() {

        override fun onReceive(c: Context, intent: Intent) {
            sampleIndex++
            val currentFingerprints = wifiManager.scanResults.map {
                createFingerprint(it)
            }
            fingerprints.addAll(currentFingerprints)
            listener?.onFingerprintsReceived(currentFingerprints)
            Thread.sleep(samplingRate.delay)
            if (sampleIndex != sampleCount) {
                wifiManager.scanResults.clear()
                if (!wifiManager.startScan()) {
                    throw RuntimeException("startScan() fail")
                }
            } else {
                stopSampling()
            }
        }
    }

    interface WifiFingerprintListener {
        fun onFingerprintsReceived(fingerprints: List<Fingerprint>)
    }
}