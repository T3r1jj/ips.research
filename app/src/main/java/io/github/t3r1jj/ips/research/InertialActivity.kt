package io.github.t3r1jj.ips.research

import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.InputType
import android.widget.LinearLayout
import android.widget.LinearLayout.HORIZONTAL
import android.widget.LinearLayout.VERTICAL
import com.github.mikephil.charting.charts.LineChart
import io.github.t3r1jj.ips.research.model.Dao
import io.github.t3r1jj.ips.research.model.collector.InertialSampler
import io.github.t3r1jj.ips.research.model.collector.SensorDelay
import io.github.t3r1jj.ips.research.model.data.InertialDataset
import io.github.t3r1jj.ips.research.view.I18nArrayAdapter
import io.github.t3r1jj.ips.research.view.RealtimeChart
import io.github.t3r1jj.ips.research.view.RenderableView
import trikita.anvil.BaseDSL.MATCH
import trikita.anvil.BaseDSL.WRAP
import trikita.anvil.DSL.*


class InertialActivity : AppCompatActivity() {

    var submitted = false
    var stepsCount = 20
    var displacement = 0f
    lateinit var movementType: InertialDataset.InertialMovementType
    lateinit var sampler: InertialSampler
    lateinit var accelerationChart: LineChart
    lateinit var accelerationMagnitudeChart: LineChart
    lateinit var chartRenderer: RealtimeChart

    private inner class RealtimeChartRenderer(context: Context) : RealtimeChart(context) {

        override fun render(): Boolean {
            if (startTime == 0L) {
                startTime = System.currentTimeMillis()
            }
            chartRenderer.addChartEntry(accelerationChart, sampler.acceleration.lastOrNull()?.data
                    ?: arrayOf(0f, 0f, 0f).toFloatArray(),
                    ((System.currentTimeMillis() - startTime).toFloat() / 1000))
            chartRenderer.addChartEntry(accelerationMagnitudeChart, Math.sqrt((sampler.acceleration.lastOrNull()?.data
                    ?: arrayOf(0f, 0f, 0f).toFloatArray()).sumByDouble { (it * it).toDouble() }).toFloat(),
                    ((System.currentTimeMillis() - startTime).toFloat() / 1000))
            return sampler.isRunning
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val movementAdapter = I18nArrayAdapter(this, InertialDataset.InertialMovementType.values())
        val delayAdapter = I18nArrayAdapter(this, SensorDelay.values())
        movementType = movementAdapter.getItem(0)
        chartRenderer = RealtimeChartRenderer(this)
        sampler = InertialSampler(this)
        accelerationChart = chartRenderer.createChart(-15f, 15f)
        accelerationChart.description.text = getString(R.string.acceleration_label)
        accelerationMagnitudeChart = chartRenderer.createChart(0f, 30f)
        accelerationMagnitudeChart.description.text = getString(R.string.acceleration_magnitude_label)
        setContentView(object : RenderableView(this) {
            override fun view() {

                linearLayout {
                    padding(dip(8))
                    size(MATCH, MATCH)
                    orientation(VERTICAL)
                    textView {
                        text(R.string.collected_data)
                        gravity(CENTER_HORIZONTAL)
                        typeface(null, Typeface.BOLD)
                    }
                    linearLayout {
                        size(MATCH, WRAP)
                        orientation(HORIZONTAL)
                        textView {
                            text(R.string.movement_type)
                            size(WRAP, WRAP)
                        }
                        spinner {
                            size(0, WRAP)
                            weight(1f)
                            adapter(movementAdapter)
                            onItemSelected { a, _, _, _ ->
                                movementType = a.selectedItem as InertialDataset.InertialMovementType
                            }
                        }
                    }
                    linearLayout {
                        size(MATCH, WRAP)
                        orientation(HORIZONTAL)
                        textView {
                            text(R.string.steps)
                            size(WRAP, WRAP)
                        }
                        editText {
                            weight(0.5f)
                            size(0, WRAP)
                            text(stepsCount.toString())
                            inputType(InputType.TYPE_CLASS_NUMBER)
                            onTextChanged {
                                stepsCount = try {
                                    it.toString().toInt()
                                } catch (nfe: NumberFormatException) {
                                    0
                                }
                            }
                        }
                        textView {
                            text(R.string.displacement)
                            size(WRAP, WRAP)
                        }
                        editText {
                            weight(0.5f)
                            size(0, WRAP)
                            text(displacement.toString())
                            inputType(InputType.TYPE_CLASS_NUMBER)
                            onTextChanged {
                                displacement = try {
                                    it.toString().toFloat()
                                } catch (nfe: NumberFormatException) {
                                    0f
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
                                chartRenderer.clearChart(accelerationChart)
                                chartRenderer.clearChart(accelerationMagnitudeChart)
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
                                customView(accelerationChart)
                            }
                            linearLayout {
                                orientation(VERTICAL)
                                weight(0.5f)
                                size(MATCH, 0)
                                customView(accelerationMagnitudeChart)
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
                            val data = InertialDataset(movementType, sampler.acceleration)
                            data.sensorDelay = sampler.delay
                            data.steps = stepsCount
                            data.displacement = displacement
                            data.sensors = sampler.sensorsInfo
                            Dao(this@InertialActivity).save(data)
                            submitted = true
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