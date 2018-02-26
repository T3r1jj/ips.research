package io.github.t3r1jj.ips.collector

import android.R
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.LinearLayout.HORIZONTAL
import android.widget.LinearLayout.VERTICAL
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.utils.ColorTemplate
import io.github.t3r1jj.ips.collector.model.Dao
import io.github.t3r1jj.ips.collector.model.data.InertialDataset
import io.github.t3r1jj.ips.collector.model.sampler.InertialSampler
import io.github.t3r1jj.ips.collector.model.sampler.SensorDelay
import io.github.t3r1jj.ips.collector.view.RenderableView
import trikita.anvil.Anvil
import trikita.anvil.BaseDSL.MATCH
import trikita.anvil.BaseDSL.WRAP
import trikita.anvil.DSL
import trikita.anvil.DSL.*


class InertialActivity : AppCompatActivity() {
    companion object {
        const val CHARTING_FREQUENCY = 10f
        val CHARTING_DELAY
            get() = (1000 / CHARTING_FREQUENCY).toLong()
    }

    var movementType = InertialActivity.InertialMovementType.WALKING
    var submitted = false
    lateinit var sampler: InertialSampler
    lateinit var accelerationChart: LineChart
    lateinit var linearAccelerationChart: LineChart
    lateinit var rotationChart: LineChart
    private val visibleSampleCount = 100

    val renderInitiator = Thread({
        try {
            while (!Thread.interrupted() && sampler.isRunning) {
                addChartEntry(accelerationChart, sampler.acceleration.lastOrNull()?.data
                        ?: arrayOf(0f, 0f, 0f).toFloatArray())
                addChartEntry(linearAccelerationChart, sampler.linearAcceleration.lastOrNull()?.data
                        ?: arrayOf(0f, 0f, 0f).toFloatArray())
                addChartEntry(rotationChart, sampler.rotation.lastOrNull()?.data
                        ?: arrayOf(0f, 0f, 0f).toFloatArray())
                Thread.sleep(CHARTING_DELAY)
            }
        } catch (e: InterruptedException) {
        }
    })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sampler = InertialSampler(this)
        val movementAdapter = ArrayAdapter<InertialMovementType>(this, R.layout.simple_spinner_item, InertialMovementType.values().toMutableList())
        val delayAdapter = ArrayAdapter<SensorDelay>(this, R.layout.simple_spinner_item, SensorDelay.values().toMutableList())
        accelerationChart = createChart()
        accelerationChart.description.text = "Acceleration (m/s^2 to s)"
        linearAccelerationChart = createChart()
        linearAccelerationChart.description.text = "Linear acceleration (m/s^2 to s)"
        rotationChart = createChart()
        rotationChart.description.text = "Rotation (rad/s to s)"
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
                            onItemSelected { a, v, pos, id ->
                                movementType = a.selectedItem as InertialMovementType
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
                            onItemSelected { a, v, pos, id ->
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
                                sampler.stopSampling()
                                clearChart(accelerationChart)
                                clearChart(linearAccelerationChart)
                                clearChart(rotationChart)
                                submitted = false
                                sampler.startSampling()
                                if (!renderInitiator.isAlive) {
                                    renderInitiator.start()
                                }
                            }
                            weight(0.5f)
                        }
                        button {
                            size(0, WRAP)
                            text("Stop")
                            onClick {
                                sampler.stopSampling()
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
                                customView(linearAccelerationChart)
                            }
                            linearLayout {
                                orientation(VERTICAL)
                                weight(0.5f)
                                size(MATCH, 0)
                                customView(rotationChart)
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
                            Dao(this@InertialActivity)
                                    .save(InertialDataset(movementType, sampler.acceleration, sampler.linearAcceleration, sampler.rotation))
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
                Anvil.currentView<ViewGroup>().addView(chart, LayoutParams(MATCH, MATCH))
            }
        })

    }

    override fun onStop() {
        super.onStop()
        sampler.stopSampling()
    }

    private fun createChart(): LineChart {
        val chart = object : LineChart(this) {
            override fun onDraw(canvas: Canvas?) {
                try {
                    super.onDraw(canvas)
                } catch (ex: IndexOutOfBoundsException) {
                    Log.w("Charting", "IndexOutOfBoundsException")
                } catch (ex: NegativeArraySizeException) {
                    Log.w("Charting", "NegativeArraySizeException")
                }
            }
        }
        chart.description.isEnabled = true
        chart.setTouchEnabled(true)
        chart.isDragEnabled = true
        chart.setScaleEnabled(true)
        chart.setDrawGridBackground(false)
        chart.setPinchZoom(true)
        chart.setBackgroundColor(Color.LTGRAY)
        val data = LineData()
        data.setValueTextColor(Color.WHITE)
        chart.data = data
        val legend = chart.legend
        legend.form = Legend.LegendForm.LINE
        legend.textColor = Color.WHITE
        val xAxis = chart.xAxis
        xAxis.setDrawGridLines(false)
        xAxis.setAvoidFirstLastClipping(true)
        xAxis.isEnabled = true
        val leftAxis = chart.axisLeft
        leftAxis.axisMaximum = 15f
        leftAxis.axisMinimum = -15f
        leftAxis.setDrawGridLines(true)
        val rightAxis = chart.axisRight
        rightAxis.isEnabled = false
        return chart
    }

    private fun clearChart(chart: LineChart) {
        val data = chart.data
        if (data != null) {
            data.dataSets.clear()
            data.notifyDataChanged()
        }
    }

    private fun addChartEntry(chart: LineChart, values: FloatArray) {
        val data = chart.data
        if (data != null) {
            for (index in 0 until values.size) {
                var set = data.getDataSetByIndex(index)
                if (set == null) {
                    set = createSet(index)
                    data.addDataSet(set)
                }
                val x = set.entryCount.toFloat() / CHARTING_FREQUENCY
                data.addEntry(Entry(x, values[index]), index)
                data.notifyDataChanged()
                chart.notifyDataSetChanged()
                chart.setVisibleXRangeMaximum(visibleSampleCount / CHARTING_FREQUENCY)
                chart.moveViewToX(x)
            }
        }
    }

    private fun createSet(index: Int): LineDataSet {
        val label = when (index) {
            0 -> "X"
            1 -> "Y"
            else -> "Z"
        }

        val set = LineDataSet(null, label)
        set.axisDependency = YAxis.AxisDependency.LEFT
        set.color = ColorTemplate.MATERIAL_COLORS[index]
        set.setCircleColor(ColorTemplate.MATERIAL_COLORS[index])
        set.lineWidth = 1.5f
        set.circleRadius = 2.0f
        set.fillAlpha = 65
        set.fillColor = ColorTemplate.MATERIAL_COLORS[index]
        set.highLightColor = Color.rgb(255, 0, 0)
        set.valueTextColor = Color.BLACK
        set.valueTextSize = 9f
        set.setDrawValues(false)
        return set
    }

    enum class InertialMovementType {
        WALKING, RUNNING, STAIRS
    }
}