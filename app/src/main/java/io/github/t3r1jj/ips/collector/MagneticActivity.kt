package io.github.t3r1jj.ips.collector

import android.R
import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.InputType
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.LinearLayout.HORIZONTAL
import android.widget.LinearLayout.VERTICAL
import android.widget.Toast
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.utils.ColorTemplate
import io.github.t3r1jj.ips.collector.model.Dao
import io.github.t3r1jj.ips.collector.model.sampler.MagneticSampler
import io.github.t3r1jj.ips.collector.model.sampler.SensorDelay
import io.github.t3r1jj.ips.collector.model.data.MagneticDataset
import io.github.t3r1jj.ips.collector.view.RenderableView
import trikita.anvil.Anvil
import trikita.anvil.BaseDSL.MATCH
import trikita.anvil.BaseDSL.WRAP
import trikita.anvil.DSL
import trikita.anvil.DSL.*


class MagneticActivity : AppCompatActivity() {
    companion object {
        const val CHARTING_FREQUENCY = 10f
        val CHARTING_DELAY
            get() = (1000 / CHARTING_FREQUENCY).toLong()
    }

    var submitted = false
    var place = ""
    lateinit var sampler: MagneticSampler
    lateinit var magneticFieldChart: LineChart
    lateinit var magneticFingerprintChart: LineChart
    lateinit var rotationChart: LineChart
    private val visibleSampleCount = 50

    var renderInitiator = Thread(RenderRunnable())

    inner class RenderRunnable : Runnable {
        override fun run() {
            try {
                while (!Thread.interrupted() && sampler.isRunning) {
                    addChartEntry(magneticFieldChart, sampler.magneticField.lastOrNull()?.data
                            ?: arrayOf(0f, 0f, 0f).toFloatArray())
                    addChartEntry(magneticFingerprintChart, Math.sqrt((sampler.magneticField.lastOrNull()?.data
                            ?: arrayOf(0f, 0f, 0f).toFloatArray()).sumByDouble { (it * it).toDouble() }).toFloat())
                    addChartEntry(rotationChart, sampler.rotation.lastOrNull()?.data
                            ?: arrayOf(0f, 0f, 0f).toFloatArray())
                    Thread.sleep(CHARTING_DELAY)
                }
                Anvil.render()
            } catch (e: InterruptedException) {
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sampler = MagneticSampler(this)
        val delayAdapter = ArrayAdapter<SensorDelay>(this, R.layout.simple_spinner_item, SensorDelay.values().toMutableList())
        magneticFieldChart = createChart()
        magneticFieldChart.description.text = "Magnetic field (uT)"
        magneticFingerprintChart = createChart()
        magneticFingerprintChart.description.text = "Magnetic fingerprint abs(uT)"
        rotationChart = createChart()
        rotationChart.description.text = "Rotation (rad/s to s)"
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
                            text("Place name: ")
                        }
                        editText {
                            size(MATCH, WRAP)
                            text(place)
                            onTextChanged {
                                place = it.toString()
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
                                stopSampling()
                                clearChart(magneticFieldChart)
                                clearChart(magneticFingerprintChart)
                                submitted = false
                                sampler.startSampling()
                                renderInitiator = Thread(RenderRunnable())
                                renderInitiator.start()
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
                                customView(magneticFingerprintChart)
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
                            if (place.isBlank()) {
                                Toast.makeText(this@MagneticActivity, "Please provide a place name for classification", Toast.LENGTH_LONG).show()
                            } else {
                                Dao(this@MagneticActivity)
                                        .save(MagneticDataset(place, sampler.magneticField))
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
        renderInitiator.interrupt()
    }

    override fun onStop() {
        super.onStop()
        stopSampling()
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
        leftAxis.axisMaximum = 100f
        leftAxis.axisMinimum = -50f
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

    private fun addChartEntry(chart: LineChart, value: Float) {
        val data = chart.data
        if (data != null) {
            var set = data.getDataSetByIndex(0)
            if (set == null) {
                set = createSet(3)
                data.addDataSet(set)
            }
            val x = set.entryCount.toFloat() / CHARTING_FREQUENCY
            data.addEntry(Entry(x, value), 0)
            data.notifyDataChanged()
            chart.notifyDataSetChanged()
            chart.setVisibleXRangeMaximum(visibleSampleCount / CHARTING_FREQUENCY)
            chart.moveViewToX(x)
        }
    }

    private fun createSet(index: Int): LineDataSet {
        val label = when (index) {
            3 -> "ABS"
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
}