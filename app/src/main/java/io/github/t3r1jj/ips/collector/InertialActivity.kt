package io.github.t3r1jj.ips.collector

import android.R
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
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
import io.github.t3r1jj.ips.collector.model.InertialSampler
import trikita.anvil.Anvil
import trikita.anvil.BaseDSL.MATCH
import trikita.anvil.BaseDSL.WRAP
import trikita.anvil.DSL
import trikita.anvil.DSL.*


class InertialActivity : AppCompatActivity() {
    var movementType = InertialActivity.InertialMovementType.WALKING
    lateinit var sampler: InertialSampler
    lateinit var chart: LineChart
    val visibleSampleCount = 120

    val renderInitiator = Thread({
        try {
            var lastIndex = 0
            while (true) {
                for (i in lastIndex until sampler.samples.size) {
                    lastIndex = i + 1
                    addEntry(sampler.samples[i][0])
//                    val leftAxis = chart.getAxisLeft();
//                    leftAxis.setAxisMaximum(sampler.maxOf(visibleSampleCount));
//                    leftAxis.setAxisMinimum(sampler.minOf(visibleSampleCount));
                }
                Thread.sleep(500)
            }
        } catch (e: InterruptedException) {
        }
    })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sampler = InertialSampler(this)
        val movementAdapter = ArrayAdapter<InertialMovementType>(this, R.layout.simple_spinner_item, InertialMovementType.values().toMutableList())
        val delayAdapter = ArrayAdapter<InertialSampler.InertialDelay>(this, R.layout.simple_spinner_item, InertialSampler.InertialDelay.values().toMutableList())
        createChart()
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
                                sampler.delay = a.selectedItem as InertialSampler.InertialDelay
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
                                sampler.startSampling()
                                renderInitiator.start()
                            }
                            weight(0.5f)
                        }
                        button {
                            size(0, WRAP)
                            text("Stop")
                            onClick {
                                sampler.stopSampling()
                                renderInitiator.interrupt()
                            }
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
                            if (chart.parent is ViewGroup) {
                                (chart.parent as ViewGroup).removeView(chart)
                            }
                            Anvil.currentView<ViewGroup>().addView(chart, LayoutParams(MATCH, MATCH))
                            size(MATCH, 0)
                            weight(1f)
                        }
                        weight(1f)
                    }

                    button {
                        size(MATCH, WRAP)
                        text("Submit")
                        onClick { }
                    }
                }

            }
        })

    }

    fun createChart() {

        chart = object : LineChart(this) {
            override fun onDraw(canvas: Canvas?) {
                try {
                    super.onDraw(canvas)
                } catch (ex: IndexOutOfBoundsException) {
                    Log.w("Charting", "IndexOutOfBoundsException")
                }
            }
        }
//        chart.setOnChartValueSelectedListener(this);

        // enable description text
        chart.getDescription().setEnabled(true);

        // enable touch gestures
        chart.setTouchEnabled(true);

        // enable scaling and dragging
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setDrawGridBackground(false);

        // if disabled, scaling can be done on x- and y-axis separately
        chart.setPinchZoom(true);

        // set an alternative background color
        chart.setBackgroundColor(Color.LTGRAY);

        val data = LineData()
        data.setValueTextColor(Color.WHITE);

        // add empty data
        chart.setData(data);

        // get the legend (only possible after setting data)
        val l = chart.getLegend();

        // modify the legend ...
        l.setForm(Legend.LegendForm.LINE);
        l.setTextColor(Color.WHITE);

        val xl = chart.getXAxis()
        xl.setDrawGridLines(false);
        xl.setAvoidFirstLastClipping(true);
        xl.setEnabled(true);

        val leftAxis = chart.getAxisLeft();
        leftAxis.setAxisMaximum(15f);
        leftAxis.setAxisMinimum(-15f);
        leftAxis.setDrawGridLines(true);

        val rightAxis = chart.getAxisRight();
        rightAxis.setEnabled(false);
    }

    fun addEntry(value: Float) {

        val data = chart.getData()

        if (data != null) {

            var set = data.getDataSetByIndex(0);
            // set.addEntry(...); // can be called as well

            if (set == null) {
                set = createSet()
                data.addDataSet(set)
            }

            data.addEntry(Entry(set.getEntryCount().toFloat(), value), 0);
            data.notifyDataChanged();

            // let the chart know it's data has changed
            chart.notifyDataSetChanged();

            // limit the number of visible entries
            chart.setVisibleXRangeMaximum(visibleSampleCount.toFloat());
            // mChart.setVisibleYRange(30, AxisDependency.LEFT);

            // move to the latest entry
            chart.moveViewToX(data.getEntryCount().toFloat());

            // this automatically refreshes the chart (calls invalidate())
            // mChart.moveViewTo(data.getXValCount()-7, 55f,
            // AxisDependency.LEFT);
        }
    }

    fun createSet(): LineDataSet {
        val set = LineDataSet(null, "Dynamic Data");
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setColor(ColorTemplate.getHoloBlue());
        set.setCircleColor(Color.WHITE);
        set.setLineWidth(1.5f);
        set.setCircleRadius(2.5f);
        set.setFillAlpha(65);
        set.setFillColor(ColorTemplate.getHoloBlue());
        set.setHighLightColor(Color.rgb(244, 117, 117));
        set.setValueTextColor(Color.WHITE);
        set.setValueTextSize(9f);
        set.setDrawValues(false);
        return set;
    }

    enum class InertialMovementType {
        WALKING, RUNNING, STAIRS
    }
}