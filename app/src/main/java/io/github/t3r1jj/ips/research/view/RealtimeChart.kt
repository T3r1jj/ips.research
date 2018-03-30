package io.github.t3r1jj.ips.research.view

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Canvas
import android.graphics.Color
import android.util.Log
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.DefaultAxisValueFormatter
import com.github.mikephil.charting.formatter.IAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import trikita.anvil.Anvil

abstract class RealtimeChart(context: Context) : ContextWrapper(context) {
    var startTime = 0L
    var labels = arrayOf("X", "Y", "Z", "ABS")
    var chartingFrequency = 10f
    protected val chartingDelay
        get() = (1000 / chartingFrequency).toLong()

    private var renderInitiator: Thread? = null

    internal fun createChart(min: Float, max: Float): LineChart {
        val chart = object : LineChart(this) {
            override fun onDraw(canvas: Canvas?) {
                try {
                    super.onDraw(canvas)
                } catch (ex: IndexOutOfBoundsException) {
                    Log.w("Charting", "IndexOutOfBoundsException")
                } catch (ex: NegativeArraySizeException) {
                    Log.w("Charting", "NegativeArraySizeException")
                } catch (ex: NullPointerException) {
                    Log.w("Charting", "NullPointerException")
                }
            }
        }
        chart.description.isEnabled = true
        chart.setTouchEnabled(true)
        chart.isDragEnabled = true
        chart.setScaleEnabled(true)
        chart.setDrawGridBackground(false)
        chart.setPinchZoom(true)
        chart.setBackgroundColor(Color.WHITE)
        val data = LineData()
        data.setValueTextColor(Color.BLACK)
        chart.data = data
        val legend = chart.legend
        legend.form = Legend.LegendForm.LINE
        legend.textColor = Color.BLACK
        val xAxis = chart.xAxis
        xAxis.setDrawGridLines(false)
        xAxis.setAvoidFirstLastClipping(true)
        xAxis.isEnabled = true
        val defaultFormatter = DefaultAxisValueFormatter(1)
        xAxis.valueFormatter = IAxisValueFormatter { value, axis ->
            defaultFormatter.getFormattedValue(value, axis) + "s"
        }
        val leftAxis = chart.axisLeft
        leftAxis.axisMaximum = max
        leftAxis.axisMinimum = min
        leftAxis.setDrawGridLines(true)
        val rightAxis = chart.axisRight
        rightAxis.isEnabled = false
        return chart
    }

    internal fun clearChart(chart: LineChart) {
        val data = chart.data
        if (data != null) {
            data.dataSets.clear()
            data.notifyDataChanged()
        }
    }

    internal fun addChartEntry(chart: LineChart, values: FloatArray, x: Float) {
        val data = chart.data
        if (data != null) {
            for (index in 0 until values.size) {
                var set = data.getDataSetByIndex(index)
                if (set == null) {
                    set = createSet(index)
                    data.addDataSet(set)
                }
                data.addEntry(Entry(x, values[index]), index)
                data.notifyDataChanged()
                chart.notifyDataSetChanged()
                chart.setVisibleXRangeMaximum(10f)
                chart.moveViewToX(x)
            }
        }
    }

    internal fun addChartEntry(chart: LineChart, value: Float, x: Float) {
        val data = chart.data
        if (data != null) {
            var set = data.getDataSetByIndex(0)
            if (set == null) {
                set = createSet(3)
                data.addDataSet(set)
            }
            data.addEntry(Entry(x, value), 0)
            data.notifyDataChanged()
            chart.notifyDataSetChanged()
            chart.setVisibleXRangeMaximum(10f)
            chart.moveViewToX(x)
        }
    }

    private fun createSet(index: Int): LineDataSet {
        val set = LineDataSet(null, labels[index])
        set.axisDependency = YAxis.AxisDependency.LEFT
        set.color = ColorTemplate.MATERIAL_COLORS[index]
        set.setCircleColor(ColorTemplate.MATERIAL_COLORS[index])
        set.setCircleColorHole(ColorTemplate.MATERIAL_COLORS[index])
        set.fillColor = ColorTemplate.MATERIAL_COLORS[index]
        set.lineWidth = 1.5f
        set.circleRadius = 2.0f
        set.highLightColor = Color.rgb(255, 0, 0)
        set.valueTextColor = Color.BLACK
        set.valueTextSize = 9f
        set.setDrawValues(false)
        return set
    }

    abstract fun render(): Boolean

    fun startRendering() {
        stopRendering()
        renderInitiator = Thread({
            try {
                while (!Thread.interrupted() && render()) {
                    Thread.sleep(chartingDelay)
                }
                Anvil.render()
            } catch (e: InterruptedException) {
            }
        })
        renderInitiator!!.start()
    }

    fun stopRendering() {
        if (renderInitiator != null) {
            renderInitiator!!.interrupt()
            renderInitiator!!.join()
        }
        startTime = 0
    }
}