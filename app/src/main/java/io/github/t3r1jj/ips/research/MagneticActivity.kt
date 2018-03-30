package io.github.t3r1jj.ips.research

import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.InputType
import android.widget.LinearLayout
import android.widget.LinearLayout.HORIZONTAL
import android.widget.LinearLayout.VERTICAL
import android.widget.Toast
import com.github.mikephil.charting.charts.LineChart
import io.github.t3r1jj.ips.research.model.Dao
import io.github.t3r1jj.ips.research.model.collector.MagneticSampler
import io.github.t3r1jj.ips.research.model.collector.SensorDelay
import io.github.t3r1jj.ips.research.model.data.MagneticDataset
import io.github.t3r1jj.ips.research.view.I18nArrayAdapter
import io.github.t3r1jj.ips.research.view.RealtimeChart
import io.github.t3r1jj.ips.research.view.RenderableView
import trikita.anvil.BaseDSL.MATCH
import trikita.anvil.BaseDSL.WRAP
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
            if (startTime == 0L) {
                startTime = System.currentTimeMillis()
            }
            chartRenderer.addChartEntry(magneticFieldChart, sampler.magneticField.lastOrNull()?.data
                    ?: arrayOf(0f, 0f, 0f).toFloatArray(),
                    ((System.currentTimeMillis() - startTime).toFloat() / 1000))
            chartRenderer.addChartEntry(magneticFingerprintChart, Math.sqrt((sampler.magneticField.lastOrNull()?.data
                    ?: arrayOf(0f, 0f, 0f).toFloatArray()).sumByDouble { (it * it).toDouble() }).toFloat(),
                    ((System.currentTimeMillis() - startTime).toFloat() / 1000))
            chartRenderer.addChartEntry(gravityChart, sampler.gravity.lastOrNull()?.data
                    ?: arrayOf(0f, 0f, 0f).toFloatArray(),
                    ((System.currentTimeMillis() - startTime).toFloat() / 1000))
            Thread.sleep(chartingDelay)
            return sampler.isRunning
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        chartRenderer = RealtimeChartRenderer(this)
        sampler = MagneticSampler(this)
        magneticFieldChart = chartRenderer.createChart(-75f, 75f)
        magneticFieldChart.description.text = getString(R.string.magnetic_field_label)
        magneticFingerprintChart = chartRenderer.createChart(0f, 150f)
        magneticFingerprintChart.description.text = getString(R.string.magnetic_field_magnitude_label)
        gravityChart = chartRenderer.createChart(-15f, 15f)
        gravityChart.description.text = getString(R.string.gravity_label)
        val delayAdapter = I18nArrayAdapter(this, SensorDelay.values())

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
                            text(R.string.route_name)
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
                            text(R.string.number_of_samples)
                        }
                        editText {
                            size(MATCH, WRAP)
                            text(sampler.sampleLimit.toString())
                            inputType(InputType.TYPE_CLASS_NUMBER)
                            onTextChanged {
                                try {
                                    sampler.sampleLimit = it.toString().toInt()
                                } catch (nfe: NumberFormatException) {
                                }
                            }
                        }
                    }
                    linearLayout {
                        size(MATCH, WRAP)
                        orientation(HORIZONTAL)
                        textView {
                            text(R.string.sensor_delay)
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
                            text(R.string.sample)
                            onClick {
                                stopSampling()
                                chartRenderer.clearChart(magneticFieldChart)
                                chartRenderer.clearChart(gravityChart)
                                chartRenderer.clearChart(magneticFingerprintChart)
                                submitted = false
                                sampler.startSampling()
                                chartRenderer.startRendering()
                            }
                            weight(0.5f)
                        }
                        button {
                            size(0, WRAP)
                            text(R.string.stop)
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
                            text(R.string.real_time_data)
                            size(MATCH, WRAP)
                            gravity(CENTER_HORIZONTAL)
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
                        text(R.string.submit)
                        onClick {
                            if (route.isBlank()) {
                                Toast.makeText(this@MagneticActivity, R.string.route_name_required, Toast.LENGTH_LONG).show()
                            } else {
                                val data = MagneticDataset(route, sampler.magneticField, sampler.gravity)
                                data.sensorDelay = sampler.delay
                                data.sensors = sampler.sensorsInfo
                                Dao(this@MagneticActivity).save(data)
                                submitted = true
                            }
                        }
                        enabled(!sampler.isEmpty && !sampler.isRunning && !submitted)
                    }
                }

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