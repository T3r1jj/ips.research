package io.github.t3r1jj.ips.collector

import android.R
import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.LinearLayout.HORIZONTAL
import android.widget.LinearLayout.VERTICAL
import android.widget.Toast
import com.github.mikephil.charting.charts.LineChart
import io.github.t3r1jj.ips.collector.model.Dao
import io.github.t3r1jj.ips.collector.model.data.MagneticDataset
import io.github.t3r1jj.ips.collector.model.sampler.MagneticSampler
import io.github.t3r1jj.ips.collector.model.sampler.SensorDelay
import io.github.t3r1jj.ips.collector.view.RealtimeChart
import io.github.t3r1jj.ips.collector.view.RenderableView
import trikita.anvil.Anvil
import trikita.anvil.BaseDSL.MATCH
import trikita.anvil.BaseDSL.WRAP
import trikita.anvil.DSL
import trikita.anvil.DSL.*


class MagneticActivity : AppCompatActivity() {
    var submitted = false
    var route = ""
    lateinit var sampler: MagneticSampler
    lateinit var magneticFieldChart: LineChart
    lateinit var magneticFingerprintChart: LineChart
    lateinit var gravityChart: LineChart
    lateinit var chartRenderer: RealtimeChart

    private inner class RealtimeChartRenderer(context: Context) : RealtimeChart(context) {
        override fun render(): Boolean {
            chartRenderer.addChartEntry(magneticFieldChart, sampler.magneticField.lastOrNull()?.data
                    ?: arrayOf(0f, 0f, 0f).toFloatArray())
            chartRenderer.addChartEntry(magneticFingerprintChart, Math.sqrt((sampler.magneticField.lastOrNull()?.data
                    ?: arrayOf(0f, 0f, 0f).toFloatArray()).sumByDouble { (it * it).toDouble() }).toFloat())
            chartRenderer.addChartEntry(gravityChart, sampler.gravity.lastOrNull()?.data
                    ?: arrayOf(0f, 0f, 0f).toFloatArray())
            Thread.sleep(CHARTING_DELAY)
            return sampler.isRunning
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        chartRenderer = RealtimeChartRenderer(this)
        sampler = MagneticSampler(this)
        magneticFieldChart = chartRenderer.createChart(-75f, 75f)
        magneticFieldChart.description.text = "Magnetic field (uT)"
        magneticFingerprintChart = chartRenderer.createChart(0f, 150f)
        magneticFingerprintChart.description.text = "Magnetic field magnitude (uT)"
        gravityChart = chartRenderer.createChart(-15f, 15f)
        gravityChart.description.text = "Gravity (m/s2 to s)"
        val delayAdapter = ArrayAdapter<SensorDelay>(this, R.layout.simple_spinner_item, SensorDelay.values().toMutableList())

        setContentView(object : RenderableView(this) {
            override fun view() {

                linearLayout {
                    size(MATCH, MATCH)
                    padding(dip(8))
                    orientation(LinearLayout.VERTICAL)

                    linearLayout {
                        orientation(LinearLayout.HORIZONTAL)
                        textView {
                            size(WRAP, WRAP)
                            text("Route name: ")
                        }
                        editText {
                            size(MATCH, WRAP)
                            text(route)
                            onTextChanged {
                                route = it.toString()
                            }
                        }
                    }

                    linearLayout {
                        orientation(LinearLayout.HORIZONTAL)
                        size(MATCH, WRAP)
                        textView {
                            size(WRAP, WRAP)
                            text("Number of samples: ")
                        }
                        editText {
                            size(MATCH, WRAP)
                            text(sampler.sampleCount.toString())
                            inputType(InputType.TYPE_CLASS_NUMBER)
                            onTextChanged {
                                try {
                                    sampler.sampleCount = it.toString().toInt()
                                } catch (nfe: NumberFormatException) {
                                }
                            }
                        }
                    }
                    linearLayout {
                        size(MATCH, WRAP)
                        DSL.orientation(HORIZONTAL)
                        textView {
                            text("Sensor delay:")
                            size(WRAP, WRAP)
                        }
                        spinner {
                            size(0, WRAP)
                            weight(1f)
                            adapter(delayAdapter)
                            onItemSelected { a, _, _, _ ->
                                sampler.delay = a.selectedItem as SensorDelay
                            }
                        }
                    }

                    linearLayout {
                        orientation(LinearLayout.HORIZONTAL)
                        size(MATCH, WRAP)
                        button {
                            size(0, WRAP)
                            text("Sample")
                            onClick {
                                stopSampling()
                                chartRenderer.clearChart(magneticFieldChart)
                                chartRenderer.clearChart(magneticFingerprintChart)
                                submitted = false
                                sampler.startSampling()
                                chartRenderer.startRendering()
                            }
                            weight(0.5f)
                        }
                        button {
                            size(0, WRAP)
                            text("Stop")
                            onClick {
                                stopSampling()
                            }
                            enabled(sampler.isRunning)
                            weight(0.5f)
                        }
                    }

                    linearLayout {
                        size(MATCH, 0)
                        orientation(VERTICAL)
                        textView {
                            text("Real time data:")
                            size(WRAP, WRAP)
                        }
                        linearLayout {
                            orientation(VERTICAL)
                            linearLayout {
                                orientation(VERTICAL)
                                weight(0.5f)
                                size(MATCH, 0)
                                customView(magneticFieldChart)
                            }
                            linearLayout {
                                orientation(VERTICAL)
                                weight(0.5f)
                                size(MATCH, 0)
                                customView(gravityChart)
                            }
                            linearLayout {
                                orientation(VERTICAL)
                                weight(0.5f)
                                size(MATCH, 0)
                                customView(magneticFingerprintChart)
                            }
                            size(MATCH, 0)
                            weight(1f)
                        }
                        weight(1f)
                    }

                    button {
                        size(MATCH, WRAP)
                        text("Submit")
                        onClick {
                            if (route.isBlank()) {
                                Toast.makeText(this@MagneticActivity, "Please provide a route name for classification", Toast.LENGTH_LONG).show()
                            } else {
                                val data = MagneticDataset(route, sampler.magneticField, sampler.gravity)
                                data.sensors = sampler.sensorsInfo
                                Dao(this@MagneticActivity).save(data)
                                submitted = true
                            }
                        }
                        enabled(!sampler.isEmpty && !sampler.isRunning && !submitted)
                    }
                }

            }

            private fun customView(chart: View) {
                if (chart.parent is ViewGroup) {
                    (chart.parent as ViewGroup).removeView(chart)
                }
                Anvil.currentView<ViewGroup>().addView(chart, LayoutParams(MATCH, MATCH))
            }
        })

    }

    private fun stopSampling() {
        sampler.stopSampling()
        chartRenderer.stopRendering()
    }

    override fun onStop() {
        super.onStop()
        stopSampling()
    }
}