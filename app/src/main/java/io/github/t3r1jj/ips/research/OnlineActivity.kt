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
import io.github.t3r1jj.ips.research.model.LimitedQueue
import io.github.t3r1jj.ips.research.model.algorithm.Pedometer
import io.github.t3r1jj.ips.research.model.algorithm.WifiNavigator
import io.github.t3r1jj.ips.research.model.algorithm.filter.FilterFactory
import io.github.t3r1jj.ips.research.model.algorithm.filter.MovingAverageFilter
import io.github.t3r1jj.ips.research.model.collector.InertialSampler
import io.github.t3r1jj.ips.research.model.collector.InertialSampler.InertialSampleListener
import io.github.t3r1jj.ips.research.model.collector.SensorSample
import io.github.t3r1jj.ips.research.model.collector.WifiSampler
import io.github.t3r1jj.ips.research.model.data.DatasetType
import io.github.t3r1jj.ips.research.model.data.Fingerprint
import io.github.t3r1jj.ips.research.model.data.WifiDataset
import io.github.t3r1jj.ips.research.view.I18nUtils
import io.github.t3r1jj.ips.research.view.RealtimeChart
import io.github.t3r1jj.ips.research.view.RenderableView
import trikita.anvil.Anvil
import trikita.anvil.DSL.*
import weka.classifiers.trees.RandomForest

class OnlineActivity : AppCompatActivity() {

    private val navigator = WifiNavigator(RandomForest(), Regex("(eduroam|dziekanat|pb-guest|.*hotspot.*)", RegexOption.IGNORE_CASE))
    private val fingerprintScans = LimitedQueue<List<Fingerprint>>(10)
    private var place = "?"
    private var steps = 0
    private var totalScans = 0
    private lateinit var wifiSampler: WifiSampler
    lateinit var inertialSampler: InertialSampler
    lateinit var pedometerChart: LineChart
    lateinit var sensitivityChart: LineChart
    lateinit var pedometer: Pedometer
    private lateinit var chartRenderer: RealtimeChartRenderer
    private lateinit var pedometerChartLabels: Array<String>
    private lateinit var sensitivityChartLabels: Array<String>
    private lateinit var trainSet: List<WifiDataset>

    private inner class RealtimeChartRenderer(context: Context) : RealtimeChart(context) {

        override fun render(): Boolean {
            try {
                if (startTime == 0L) {
                    startTime = pedometer.t[0]
                }
                val i = pedometer.t.lastIndex
                val pedometerData = FloatArray(4)
                pedometerData[0] = pedometer.aNormalizedMagnitudes[i]
                pedometerData[1] = pedometer.min[i]
                pedometerData[2] = pedometer.max[i]
                pedometerData[3] = (pedometerData[1] + pedometerData[2]) / 2f
                val time = (pedometer.t[i] - startTime) / 1000000000f
                chartRenderer.labels = pedometerChartLabels
                chartRenderer.addChartEntry(pedometerChart, pedometerData, time)
                val pedometerSensitivity = FloatArray(2)
                pedometerSensitivity[0] = pedometer.sensitivities[i]
                pedometerSensitivity[1] = pedometer.max[i] - pedometer.min[i]
                chartRenderer.labels = sensitivityChartLabels
                chartRenderer.addChartEntry(sensitivityChart, pedometerSensitivity, time)
            } catch (ioobe: IndexOutOfBoundsException) {
                Log.w("Charting", "IndexOutOfBoundsException")
            }
            return inertialSampler.isRunning
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pedometerChartLabels = arrayOf("aNorm", "min", "max", getString(R.string.threshold))
        sensitivityChartLabels = arrayOf(getString(R.string.sensitivity), "max-min")
        trainSet = Dao(this).findAll().values
                .filter { it.type == DatasetType.WIFI }
                .map { it as WifiDataset }
        try {
            navigator.train(trainSet)
        } catch (rte: RuntimeException) {
            Toast.makeText(this@OnlineActivity, getString(R.string.error) + I18nUtils.tryI18nException(this, rte),
                    Toast.LENGTH_LONG).show()
            place = "null"
        }
        wifiSampler = WifiSampler(this, true)
        chartRenderer = RealtimeChartRenderer(this)
        inertialSampler = InertialSampler(this)
        pedometerChart = chartRenderer.createChart(0.5f, 1.5f)
        pedometerChart.description.text = getString(R.string.pedometer_alg_label)
        sensitivityChart = chartRenderer.createChart(0f, 1.5f)
        sensitivityChart.description.text = getString(R.string.pedometer_sens_label)
        setContentView(object : RenderableView(this) {
            override fun view() {
                linearLayout {
                    padding(dip(8))
                    size(MATCH, MATCH)
                    orientation(LinearLayout.VERTICAL)
                    textView {
                        size(MATCH, WRAP)
                        text(getString(R.string.wifi_position) + " [" + getString(R.string.random_forest) + "]")
                        textSize(sip(16f))
                        textColor(ColorStateList.valueOf(Color.BLACK))
                        gravity(CENTER_HORIZONTAL)
                    }
                    textView {
                        size(MATCH, WRAP)
                        padding(dip(8))
                        text(getString(R.string.wifi_online_test_description, fingerprintScans.size, totalScans))
                        gravity(CENTER_HORIZONTAL)
                    }

                    linearLayout {
                        size(MATCH, WRAP)
                        orientation(LinearLayout.HORIZONTAL)
                        padding(dip(8), 0, 0, 0)
                        textView {
                            size(WRAP, WRAP)
                            text(R.string.place)
                        }
                        textView {
                            size(WRAP, WRAP)
                            padding(dip(8), 0, 0, 0)
                            text(getFormattedPlace())
                            textColor(ColorStateList.valueOf(Color.BLACK))
                        }
                    }
                    textView {
                        padding(0, dip(16), 0, 0)
                        size(MATCH, WRAP)
                        text(getString(R.string.pedometer) + " [" +
                                getStringResourceByName(FilterFactory.FilterType.MOVING_AVERAGE_FILTER.toString())
                                + " (3), " + getString(R.string.sampling_rate) + ": " +
                                getStringResourceByName(inertialSampler.delay.toString()) + " ]")
                        textSize(sip(16f))
                        textColor(ColorStateList.valueOf(Color.BLACK))
                        gravity(CENTER_HORIZONTAL)
                    }
                    linearLayout {
                        size(MATCH, WRAP)
                        orientation(LinearLayout.HORIZONTAL)
                        padding(dip(8), dip(8), 0, 8)
                        textView {
                            size(WRAP, WRAP)
                            text(R.string.steps)
                        }
                        textView {
                            size(WRAP, WRAP)
                            padding(dip(8), 0, 0, 0)
                            text(steps.toString())
                            textColor(ColorStateList.valueOf(Color.BLACK))
                        }
                    }

                    linearLayout {
                        size(MATCH, 0)
                        orientation(LinearLayout.VERTICAL)
                        textView {
                            text(R.string.real_time_data)
                            size(MATCH, WRAP)
                            gravity(CENTER_HORIZONTAL)
                        }
                        linearLayout {
                            orientation(LinearLayout.VERTICAL)
                            size(MATCH, 0)
                            customView(pedometerChart)
                            weight(1f)
                        }
                        linearLayout {
                            orientation(LinearLayout.VERTICAL)
                            size(MATCH, 0)
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
                            text(R.string.start)
                            enabled(!wifiSampler.running)
                            onClick {
                                try {
                                    stopSampling()
                                    pedometer = Pedometer(MovingAverageFilter(3), false)
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
                                            fingerprintScans.add(fingerprints)
                                            Thread({
                                                navigator.predictionSet = WifiDataset("?", fingerprintScans.flatten())
                                                navigator.predictionSet!!.iterations = fingerprintScans.size
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
                                    Toast.makeText(this@OnlineActivity, getString(R.string.error) +
                                            I18nUtils.tryI18nException(this@OnlineActivity, ex),
                                            Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                        button {
                            size(0, WRAP)
                            weight(1f)
                            enabled(wifiSampler.running || inertialSampler.isRunning)
                            text(R.string.stop)
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
        "?" -> getString(R.string.no_data_wifi_scan)
        "null" -> getString(R.string.no_data_wifi_db)
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
    }

    private fun getStringResourceByName(aString: String): String {
        val packageName = packageName
        val resId = resources.getIdentifier(aString, "string", packageName)
        return getString(resId)
    }

}