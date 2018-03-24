package io.github.t3r1jj.ips.research.view

import android.annotation.SuppressLint
import android.content.Context
import android.support.design.widget.BottomSheetDialog
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import io.github.t3r1jj.ips.research.DatabaseActivity
import io.github.t3r1jj.ips.research.R
import io.github.t3r1jj.ips.research.model.algorithm.filter.FilterFactory
import io.github.t3r1jj.ips.research.model.test.PedometerTester
import trikita.anvil.DSL.*
import java.io.ByteArrayOutputStream

@SuppressLint("ViewConstructor")
class PedometerDialog(context: Context, private val pedometer: DatabaseActivity) : RenderableView(context) {
    private val algorithmAdapter = I18nArrayAdapter(context, FilterFactory.FilterType.values())
    private val averageAdapter = ArrayAdapter<Int>(context,
            io.github.t3r1jj.ips.research.R.layout.support_simple_spinner_dropdown_item,
            arrayOf(2, 3, 4, 5, 6, 7, 8, 9, 10))

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
                            pedometer.filterFactory.averagingWindowLength = a.selectedItem as Int
                        }
                    }
                }
            }
            linearLayout {
                size(MATCH, WRAP)
                orientation(LinearLayout.HORIZONTAL)
                button {
                    size(0, WRAP)
                    text(R.string.generate)
                    onClick {
                        pedometer.userInputDialog?.dismiss()
                        pedometer.tester = PedometerTester(pedometer.filterFactory)
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
                    size(0, WRAP)
                    text(R.string.cancel)
                    onClick {
                        pedometer.userInputDialog?.dismiss()
                    }
                    weight(1f)
                }
            }
        }
    }

}