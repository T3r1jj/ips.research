package io.github.t3r1jj.ips.research

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.LinearLayout
import android.widget.Toast
import com.github.mikephil.charting.charts.LineChart
import io.github.t3r1jj.ips.research.model.Dao
import io.github.t3r1jj.ips.research.model.algorithm.Pedometer
import io.github.t3r1jj.ips.research.model.algorithm.WifiNavigator
import io.github.t3r1jj.ips.research.model.algorithm.filter.KalmanFilter
import io.github.t3r1jj.ips.research.model.collector.InertialSampler
import io.github.t3r1jj.ips.research.model.collector.InertialSampler.InertialSampleListener
import io.github.t3r1jj.ips.research.model.collector.SensorSample
import io.github.t3r1jj.ips.research.model.collector.WifiSampler
import io.github.t3r1jj.ips.research.model.data.DatasetType
import io.github.t3r1jj.ips.research.model.data.Fingerprint
import io.github.t3r1jj.ips.research.model.data.WifiDataset
import io.github.t3r1jj.ips.research.view.RealtimeChart
import io.github.t3r1jj.ips.research.view.RenderableView
import trikita.anvil.Anvil
import trikita.anvil.BaseDSL
import trikita.anvil.DSL.*
import weka.classifiers.bayes.BayesNet

class OnlineActivity : AppCompatActivity() {

    private val navigator = WifiNavigator(BayesNet(), Regex("(eduroam|dziekanat|pb-guest|.*hotspot.*)", RegexOption.IGNORE_CASE))
    private var place = "?"
    private var steps = 0
    private var totalScans = 0
    private lateinit var wifiSampler: WifiSampler
    lateinit var inertialSampler: InertialSampler
    lateinit var pedometerChart: LineChart
    lateinit var sensitivityChart: LineChart
    lateinit var pedometer: Pedometer
    private lateinit var chartRenderer: RealtimeChartRenderer
    private val pedometerChartLabels = arrayOf("aNorm", "min", "max", "threshold")
    private val sensitivityChartLabels = arrayOf("sensitivity", "max-min")
    private lateinit var trainSet: List<WifiDataset>

    private inner class RealtimeChartRenderer(context: Context) : RealtimeChart(context) {

        var lastRenderIndex = 0

        override fun render(): Boolean {
            try {
                for (i in lastRenderIndex + 1 until pedometer.t.size) {
                    val pedometerData = FloatArray(4)
                    pedometerData[0] = pedometer.aNormalizedMagnitudes[i]
                    pedometerData[1] = pedometer.min[i]
                    pedometerData[2] = pedometer.max[i]
                    pedometerData[3] = (pedometerData[1] + pedometerData[2]) / 2f
                    val time = (pedometer.t[i] - pedometer.t.first()) / 1000000000f
                    chartRenderer.labels = pedometerChartLabels
                    chartRenderer.addChartEntry(pedometerChart, pedometerData, time)
                    val pedometerSensitivity = FloatArray(2)
                    pedometerSensitivity[0] = pedometer.sensitivities[i]
                    pedometerSensitivity[1] = pedometer.max[i] - pedometer.min[i]
                    chartRenderer.labels = sensitivityChartLabels
                    chartRenderer.addChartEntry(sensitivityChart, pedometerSensitivity, time)
                    lastRenderIndex = i
                }
            } catch (ioobe: IndexOutOfBoundsException) {
                Log.w("Charting", "IndexOutOfBoundsException")
                lastRenderIndex = 0
            }
            return inertialSampler.isRunning
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        trainSet = Dao(this).findAll().values
                .filter { it.type == DatasetType.WIFI }
                .map { it as WifiDataset }
        try {
            navigator.train(trainSet)
        } catch (rte: RuntimeException) {
            Toast.makeText(this@OnlineActivity, "Error: " + rte.toString(), Toast.LENGTH_LONG).show()
            place = "null"
        }
        wifiSampler = WifiSampler(this)
        chartRenderer = RealtimeChartRenderer(this)
        inertialSampler = InertialSampler(this)
        pedometerChart = chartRenderer.createChart(0.5f, 1.5f)
        pedometerChart.description.text = "Pedometer algorithm (m/s^2 to s)"
        sensitivityChart = chartRenderer.createChart(0f, 1.5f)
        sensitivityChart.description.text = "Pedometer sensitivity (m/s^2 to s)"
        setContentView(object : RenderableView(this) {
            override fun view() {
                linearLayout {
                    size(MATCH, MATCH)
                    orientation(LinearLayout.VERTICAL)
                    textView {
                        size(MATCH, WRAP)
                        text("WiFi Position")
                        textSize(sip(16f))
                        textColor(ColorStateList.valueOf(Color.BLACK))
                        gravity(CENTER_HORIZONTAL)
                    }
                    textView {
                        size(MATCH, WRAP)
                        padding(dip(8))
                        text("Your location will be predicted based on collected WiFi data\nand last " +
                                navigator.fingerprints.size + " WiFi scans (total: " + totalScans + ")" )
                        gravity(CENTER_HORIZONTAL)
                    }

                    linearLayout {
                        size(MATCH, WRAP)
                        orientation(LinearLayout.HORIZONTAL)
                        padding(dip(8), 0, 0, 0)
                        textView {
                            size(WRAP, WRAP)
                            text("Place: ")
                        }
                        textView {
                            size(WRAP, WRAP)
                            text(getFormattedPlace())
                            textColor(ColorStateList.valueOf(Color.BLACK))
                        }
                    }
                    textView {
                        padding(0, dip(16), 0, 0)
                        size(MATCH, WRAP)
                        text("Pedometer")
                        textSize(sip(16f))
                        textColor(ColorStateList.valueOf(Color.BLACK))
                        gravity(CENTER_HORIZONTAL)
                    }
                    linearLayout {
                        size(MATCH, WRAP)
                        orientation(LinearLayout.HORIZONTAL)
                        padding(dip(8), 0, 0, 8)
                        textView {
                            size(WRAP, WRAP)
                            text("Steps: ")
                        }
                        textView {
                            size(WRAP, WRAP)
                            text(steps.toString())
                            textColor(ColorStateList.valueOf(Color.BLACK))
                        }
                    }

                    linearLayout {
                        size(BaseDSL.MATCH, 0)
                        orientation(LinearLayout.VERTICAL)
                        textView {
                            text("Real time data:")
                            size(BaseDSL.MATCH, BaseDSL.WRAP)
                            gravity(BaseDSL.CENTER_HORIZONTAL)
                        }
                        linearLayout {
                            orientation(LinearLayout.VERTICAL)
                            size(BaseDSL.MATCH, 0)
                            customView(pedometerChart)
                            weight(1f)
                        }
                        linearLayout {
                            orientation(LinearLayout.VERTICAL)
                            size(BaseDSL.MATCH, 0)
                            customView(sensitivityChart)
                            weight(1f)
                        }
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
                                try {
                                    stopSampling()
                                    pedometer = Pedometer(KalmanFilter())
                                    inertialSampler.samplerListener = object : InertialSampleListener {
                                        override fun onSampleReceived(sensorSample: SensorSample) {
                                            pedometer.processSample(sensorSample)
                                            if (steps != pedometer.stepCount) {
                                                steps = pedometer.stepCount
                                                Anvil.render()
                                            }
                                        }
                                    }
                                    wifiSampler.listener = object : WifiSampler.WifiFingerprintListener {
                                        override fun onFingerprintsReceived(fingerprints: List<Fingerprint>) {
                                            navigator.addFingerprints(fingerprints)
                                            Thread({
                                                place = navigator.classify()
                                                totalScans++
                                                Anvil.render()
                                            }).start()
                                        }
                                    }
                                    steps = 0
                                    totalScans = 0
                                    wifiSampler.fingerprints.clear()
                                    inertialSampler.startSampling()
                                    chartRenderer.startRendering()
                                    wifiSampler.startSampling()
                                } catch (ex: Exception) {
                                    Toast.makeText(this@OnlineActivity, "Error: " + ex.toString(), Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                        button {
                            size(0, WRAP)
                            weight(1f)
                            enabled(wifiSampler.running || inertialSampler.isRunning)
                            text("Stop")
                            onClick {
                                stopSampling()
                            }
                        }
                    }
                }
            }
        })
    }

    private fun getFormattedPlace() = when (place) {
        "?" -> "no data from wifi scan"
        "null" -> "no data has been collected"
        else -> place
    }


    override fun onPause() {
        super.onPause()
        stopSampling()
    }

    private fun stopSampling() {
        wifiSampler.stopSampling()
        inertialSampler.stopSampling()
        chartRenderer.stopRendering()
        chartRenderer.lastRenderIndex = 0
    }

}