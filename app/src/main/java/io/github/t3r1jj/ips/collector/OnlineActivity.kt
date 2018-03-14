package io.github.t3r1jj.ips.collector

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.LinearLayout
import android.widget.Toast
import io.github.t3r1jj.ips.collector.model.Dao
import io.github.t3r1jj.ips.collector.model.algorithm.WifiNavigator
import io.github.t3r1jj.ips.collector.model.data.DatasetType
import io.github.t3r1jj.ips.collector.model.data.WifiDataset
import io.github.t3r1jj.ips.collector.model.collector.WifiSampler
import io.github.t3r1jj.ips.collector.view.RenderableView
import trikita.anvil.Anvil
import trikita.anvil.BaseDSL
import trikita.anvil.DSL.*
import weka.classifiers.bayes.BayesNet
import java.lang.Thread.sleep

class OnlineActivity : AppCompatActivity() {

    private val navigator = WifiNavigator(BayesNet(), Regex("(eduroam|dziekanat|pb-guest|.*hotspot.*)", RegexOption.IGNORE_CASE))
    private var place = ""
    private lateinit var wifiSampler: WifiSampler
    private lateinit var testSet: WifiDataset
    private var pollerThread: Thread? = null

    private fun createPollerThread(): Thread {
        return Thread({
            try {
                while (!Thread.interrupted() && wifiSampler.running) {
                    sleep(1000)
                }
                testSet = WifiDataset("?", wifiSampler.fingerprints)
                testSet.iterations = wifiSampler.sampleIndex
                classify()
            } catch (iex: InterruptedException) {
            }
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        wifiSampler = WifiSampler(this)
        setContentView(R.layout.activity_main)
        setContentView(object : RenderableView(this) {
            override fun view() {
                linearLayout {
                    size(MATCH, MATCH)
                    orientation(LinearLayout.VERTICAL)

                    textView {
                        size(BaseDSL.MATCH, WRAP)
                        BaseDSL.padding(BaseDSL.dip(10))
                        text("Your location will be predicted based on collected data")
                    }
                    textView {
                        size(BaseDSL.MATCH, WRAP)
                        gravity(BaseDSL.CENTER_HORIZONTAL)
                        BaseDSL.padding(BaseDSL.dip(10))
                        text("Sampling iteration: " + wifiSampler.sampleIndex)
                    }

                    linearLayout {
                        size(MATCH, WRAP)
                        orientation(LinearLayout.HORIZONTAL)
                        textView {
                            size(WRAP, WRAP)
                            text("Place: ")
                        }
                        textView {
                            size(WRAP, WRAP)
                            text(place)
                        }
                    }

                    relativeLayout {
                        size(MATCH, 0)
                        weight(1f)
                    }

                    linearLayout {
                        size(MATCH, WRAP)
                        orientation(LinearLayout.HORIZONTAL)
                        button {
                            size(0, WRAP)
                            weight(1f)
                            text("Start")
                            enabled(!wifiSampler.running)
                            onClick {
                                wifiSampler.startSampling()
                                pollerThread = createPollerThread()
                                pollerThread!!.start()
                            }
                        }
                        button {
                            size(0, WRAP)
                            weight(1f)
                            enabled(wifiSampler.running)
                            text("Stop")
                            onClick {
                                wifiSampler.stopSampling()
                                pollerThread!!.join()
                            }
                        }
                    }
                }
            }
        })
    }

    override fun onPause() {
        super.onPause()
        wifiSampler.stopSampling()
        if (pollerThread != null) {
            pollerThread!!.interrupt()
        }
    }

    private fun classify() {
        if (testSet.fingerprints.isEmpty()) {
            place = "NONE"
            Anvil.render()
            return
        }
        try {
            val trainSet = Dao(this@OnlineActivity).findAll().values
                    .filter { it.device.contains("Combo") }
                    .filter { it.type == DatasetType.WIFI }
                    .map { it as WifiDataset }
            place = navigator.classify(
                    trainSet,
                    testSet)
            Anvil.render()
        } catch (ex: Exception) {
            runOnUiThread {
                Toast.makeText(this@OnlineActivity,
                        "Something went wrong, check collected data, log: " + ex.toString(),
                        Toast.LENGTH_LONG).show()
            }
            ex.printStackTrace()
        }
    }
}