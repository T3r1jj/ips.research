package io.github.t3r1jj.ips.collector

import android.R
import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.widget.Adapter
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.LinearLayout.HORIZONTAL
import android.widget.LinearLayout.VERTICAL
import com.github.mikephil.charting.charts.LineChart
import io.github.t3r1jj.ips.collector.model.Dao
import io.github.t3r1jj.ips.collector.model.data.InertialDataset
import io.github.t3r1jj.ips.collector.model.sampler.InertialSampler
import io.github.t3r1jj.ips.collector.model.sampler.SensorDelay
import io.github.t3r1jj.ips.collector.view.RealtimeChart
import io.github.t3r1jj.ips.collector.view.RenderableView
import trikita.anvil.Anvil
import trikita.anvil.BaseDSL.MATCH
import trikita.anvil.BaseDSL.WRAP
import trikita.anvil.DSL
import trikita.anvil.DSL.*


class InertialActivity : AppCompatActivity() {
    var movementType = InertialDataset.InertialMovementType.WALKING
    var submitted = false
    var stepsCount = 20
    var displacement = 0f
    lateinit var sampler: InertialSampler
    lateinit var accelerationChart: LineChart
    lateinit var accelerationMagnitudeChart: LineChart
    lateinit var linearAccelerationChart: LineChart
    lateinit var chartRenderer: RealtimeChart

    private inner class RealtimeChartRenderer(context: Context) : RealtimeChart(context) {
        override fun render(): Boolean {
            chartRenderer.addChartEntry(accelerationChart, sampler.acceleration.lastOrNull()?.data
                    ?: arrayOf(0f, 0f, 0f).toFloatArray())
            chartRenderer.addChartEntry(accelerationMagnitudeChart, Math.sqrt((sampler.acceleration.lastOrNull()?.data
                    ?: arrayOf(0f, 0f, 0f).toFloatArray()).sumByDouble { (it * it).toDouble() }).toFloat())
            chartRenderer.addChartEntry(linearAccelerationChart, sampler.linearAcceleration.lastOrNull()?.data
                    ?: arrayOf(0f, 0f, 0f).toFloatArray())
            return sampler.isRunning
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        chartRenderer = RealtimeChartRenderer(this)
        sampler = InertialSampler(this)
        accelerationChart = chartRenderer.createChart(-15f, 15f)
        accelerationChart.description.text = "Acceleration (m/s^2 to s)"
        accelerationMagnitudeChart = chartRenderer.createChart(0f, 30f)
        accelerationMagnitudeChart.description.text = "Acceleration magnitude (m/s^2 to s)"
        linearAccelerationChart = chartRenderer.createChart(-15f, 15f)
        linearAccelerationChart.description.text = "Linear acceleration (m/s^2 to s)"
        val movementAdapter: Adapter = ArrayAdapter<InertialDataset.InertialMovementType>(this,
                R.layout.simple_spinner_item, InertialDataset.InertialMovementType.values().toMutableList())
        val delayAdapter: Adapter = ArrayAdapter<SensorDelay>(this, R.layout.simple_spinner_item, SensorDelay.values().toMutableList())
        setContentView(object : RenderableView(this) {
            override fun view() {

                linearLayout {
                    size(MATCH, MATCH)
                    DSL.orientation(VERTICAL)
                    textView {
                        DSL.text("Data collected:")
                        DSL.gravity(CENTER_HORIZONTAL)
                        typeface(null, Typeface.BOLD)
                    }
                    linearLayout {
                        size(MATCH, WRAP)
                        DSL.orientation(HORIZONTAL)
                        textView {
                            text("Movement type:")
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
                        DSL.orientation(HORIZONTAL)
                        textView {
                            text("Steps:")
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
                            text("Displacement [m]:")
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
                                chartRenderer.clearChart(accelerationChart)
                                chartRenderer.clearChart(accelerationMagnitudeChart)
                                chartRenderer.clearChart(linearAccelerationChart)
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
                                customView(accelerationChart)
                            }
                            linearLayout {
                                orientation(VERTICAL)
                                weight(0.5f)
                                size(MATCH, 0)
                                customView(accelerationMagnitudeChart)
                            }
                            linearLayout {
                                orientation(VERTICAL)
                                weight(0.5f)
                                size(MATCH, 0)
                                customView(linearAccelerationChart)
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
                            val data = InertialDataset(movementType, sampler.acceleration, sampler.linearAcceleration, sampler.magneticField, sampler.gravity)
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

            private fun customView(chart: View) {
                if (chart.parent is ViewGroup) {
                    (chart.parent as ViewGroup).removeView(chart)
                }
                Anvil.currentView<ViewGroup>().addView(chart, ViewGroup.LayoutParams(MATCH, MATCH))
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