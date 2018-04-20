package io.github.t3r1jj.ips.research.view

import android.annotation.SuppressLint
import android.content.Context
import android.support.design.widget.BottomSheetDialog
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import io.github.t3r1jj.ips.research.DatabaseActivity
import io.github.t3r1jj.ips.research.R
import io.github.t3r1jj.ips.research.model.algorithm.filter.FilterFactory
import io.github.t3r1jj.ips.research.model.test.PedometerTester
import trikita.anvil.DSL.*
import java.io.ByteArrayOutputStream
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.*

@SuppressLint("ViewConstructor")
class PedometerDialog(context: Context, private val pedometer: DatabaseActivity) : RenderableView(context) {
    private val algorithmAdapter = I18nArrayAdapter(context, FilterFactory.FilterType.values())
    private val sensitivityTypeAdapter = I18nArrayAdapter(context, SensitivityType.values())
    private val sensitivityAdapter = ArrayAdapter<Float>(context,
            io.github.t3r1jj.ips.research.R.layout.support_simple_spinner_dropdown_item,
            IntRange(0, 12).map { 2 + it * 0.25f })
    private val rAdapter = ArrayAdapter<Float>(context,
            io.github.t3r1jj.ips.research.R.layout.support_simple_spinner_dropdown_item,
            arrayOf(1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f, 10f))
    private val averageAdapter = ArrayAdapter<Int>(context,
            io.github.t3r1jj.ips.research.R.layout.support_simple_spinner_dropdown_item,
            arrayOf(2, 3, 4, 5, 6, 7, 8, 9, 10))
    private var sensitivityType = sensitivityTypeAdapter.getItem(0)
    private var sensitivity = sensitivityAdapter.getItem(0)
    private val df = NumberFormat.getInstance(Locale.UK) as DecimalFormat

    init {
        df.applyPattern("#.##")
        df.roundingMode = RoundingMode.HALF_UP
    }

    override fun view() {
        linearLayout {
            padding(dip(8))
            size(MATCH, MATCH)
            orientation(LinearLayout.VERTICAL)
            textView {
                padding(dip(8))
                size(MATCH, WRAP)
                text(R.string.choose_pedometer_alg)
            }
            spinner {
                padding(dip(8))
                size(MATCH, MATCH)
                weight(1f)
                adapter(algorithmAdapter)
                onItemSelected { a, _, _, _ ->
                    pedometer.filterFactory.filterType = a.selectedItem as FilterFactory.FilterType
                }
            }
            if (pedometer.filterFactory.filterType == FilterFactory.FilterType.MOVING_AVERAGE_FILTER) {
                linearLayout {
                    padding(dip(8), dip(8), dip(16), dip(8))
                    size(MATCH, WRAP)
                    orientation(LinearLayout.HORIZONTAL)
                    textView {
                        size(WRAP, WRAP)
                        text(R.string.length_moving_avg)
                    }
                    spinner {
                        size(MATCH, MATCH)
                        weight(1f)
                        adapter(averageAdapter)
                        onItemSelected { a, _, _, _ ->
                            pedometer.filterFactory.parameter = a.selectedItem as Int
                        }
                    }
                }
            } else if (pedometer.filterFactory.filterType == FilterFactory.FilterType.KALMAN_FILTER) {
                linearLayout {
                    padding(dip(8), dip(8), 0, dip(8))
                    size(MATCH, WRAP)
                    orientation(LinearLayout.HORIZONTAL)
                    textView {
                        size(WRAP, WRAP)
                        text("Q = 1.0, R:")
                    }
                    spinner {
                        size(MATCH, MATCH)
                        weight(1f)
                        adapter(rAdapter)
                        onItemSelected { a, _, _, _ ->
                            pedometer.filterFactory.parameter = a.selectedItem as Float
                        }
                    }
                }
            }
            textView {
                padding(dip(8))
                size(MATCH, WRAP)
                text(R.string.choose_sensitivity)
            }
            spinner {
                padding(dip(8))
                size(MATCH, MATCH)
                weight(1f)
                adapter(sensitivityTypeAdapter)
                onItemSelected { a, _, _, _ ->
                    sensitivityType = a.selectedItem as SensitivityType
                }
            }
            if (sensitivityType == SensitivityType.CONST) {
                linearLayout {
                    padding(dip(8), dip(8), dip(16), dip(8))
                    size(MATCH, WRAP)
                    orientation(LinearLayout.HORIZONTAL)
                    textView {
                        size(WRAP, WRAP)
                        text(R.string.sensitivity)
                    }
                    spinner {
                        size(MATCH, MATCH)
                        weight(1f)
                        adapter(sensitivityAdapter)
                        onItemSelected { a, _, _, _ ->
                            sensitivity = a.selectedItem as Float
                        }
                    }
                }
            }
            linearLayout {
                size(MATCH, WRAP)
                orientation(LinearLayout.HORIZONTAL)
                button {
                    size(0, MATCH)
                    text(R.string.generate)
                    onClick {
                        pedometer.userInputDialog?.dismiss()
                        if (sensitivityType == SensitivityType.AUTO) {
                            pedometer.tester = PedometerTester(pedometer.filterFactory)
                        } else {
                            pedometer.tester = PedometerTester(pedometer.filterFactory, sensitivity)
                        }
                        pedometer.tester.i18n.loadI18n(context)
                        val data = pedometer.inertialData()
                        pedometer.tester.test(data)
                        val info = ByteArrayOutputStream()
                        pedometer.tester.saveOutputInfo(info)
                        val bottomSheet = BottomSheetDialog(context)
                        pedometer.outputDialog = bottomSheet
                        bottomSheet.setContentView(PedometerBottomSheetDialog(context, info.toString(), pedometer))
                        bottomSheet.show()
                    }
                    weight(1f)
                }
                button {
                    size(0, MATCH)
                    text("(*) [10 min +] " + context.getString(R.string.info_to_files))
                    onClick {
                        var time = System.currentTimeMillis()
                        pedometer.filterFactory.filterType = FilterFactory.FilterType.NO_FILTER
                        val x = mutableListOf<String>()
                        for (i in 0 until sensitivityAdapter.count) {
                            val sensitivity = sensitivityAdapter.getItem(i)
                            x.add(sensitivity.toString())
                        }
                        x.add((-1).toString())

                        val y = mutableListOf<String>()
                        time = infoToFilesSingleConfigMultiSens(y, time)
                        pedometer.filterFactory.filterType = FilterFactory.FilterType.MOVING_AVERAGE_FILTER
                        val yMA = mutableListOf<MutableList<String>>()
                        for (i in 0 until averageAdapter.count) {
                            Log.i("GENERATOR", (i).toString() + "/" + (averageAdapter.count + rAdapter.count).toString())
                            @Suppress("NAME_SHADOWING")
                            val y = mutableListOf<String>()
                            pedometer.filterFactory.parameter = averageAdapter.getItem(i)
                            time = infoToFilesSingleConfigMultiSens(y, time)
                            yMA.add(y)
                        }

                        pedometer.filterFactory.filterType = FilterFactory.FilterType.KALMAN_FILTER
                        val yKA = mutableListOf<MutableList<String>>()
                        for (i in 0 until rAdapter.count) {
                            Log.i("GENERATOR", (averageAdapter.count + 1 + i).toString() + "/" + (averageAdapter.count + rAdapter.count).toString())
                            @Suppress("NAME_SHADOWING")
                            val y = mutableListOf<String>()
                            pedometer.filterFactory.parameter = rAdapter.getItem(i)
                            time = infoToFilesSingleConfigMultiSens(y, time)
                            yKA.add(y)
                        }
                        val output = "x=" + x.joinToString(",", "[", "];") + "\n" +
                                "y=" + y.joinToString(",", "[", "];") + "\n" +
                                yMA.mapIndexed { index, list -> "yMA" + (index + 2).toString() + "=" + list.joinToString(",", "[", "];") + "\n" }
                                        .joinToString("") +
                                yKA.mapIndexed { index, list -> "yKA" + (index + 1).toString() + "=" + list.joinToString(",", "[", "];") + "\n" }
                                        .joinToString("")
                        println(output)
                        pedometer.onInfoOutputSave(time, output
                        )
                    }
                    weight(1f)
                }

                button {
                    size(0, MATCH)
                    text(R.string.cancel)
                    onClick {
                        pedometer.userInputDialog?.dismiss()
                    }
                    weight(1f)
                }
            }
        }
    }

    private fun infoToFilesSingleConfigMultiSens(y: MutableList<String>, _time: Long): Long {
        var time = _time
        for (i in 0..sensitivityAdapter.count) {
            if (i == sensitivityAdapter.count) {
                pedometer.tester = PedometerTester(pedometer.filterFactory)
            } else {
                val sensitivity = sensitivityAdapter.getItem(i)
                pedometer.tester = PedometerTester(pedometer.filterFactory, sensitivity)
            }
            pedometer.tester.i18n.loadI18n(context)
            val data = pedometer.inertialData()
            pedometer.tester.test(data)
            y.add(df.format(pedometer.tester.totalAccuracy(pedometer.tester.output)))
            pedometer.onPedometerInfoClick(time++)
        }
        return time
    }

    enum class SensitivityType {
        AUTO, CONST
    }
}