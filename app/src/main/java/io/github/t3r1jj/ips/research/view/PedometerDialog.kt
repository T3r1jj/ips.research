package io.github.t3r1jj.ips.research.view

import android.annotation.SuppressLint
import android.content.Context
import android.support.design.widget.BottomSheetDialog
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import io.github.t3r1jj.ips.research.DatabaseActivity
import io.github.t3r1jj.ips.research.model.algorithm.filter.FilterFactory
import io.github.t3r1jj.ips.research.model.test.PedometerTester
import trikita.anvil.BaseDSL
import trikita.anvil.DSL
import trikita.anvil.DSL.*
import java.io.ByteArrayOutputStream

@SuppressLint("ViewConstructor")
class PedometerDialog(context: Context, private val pedometer: DatabaseActivity) : RenderableView(context) {
    private val algorithmAdapter = ArrayAdapter<FilterFactory.FilterType>(context,
            io.github.t3r1jj.ips.research.R.layout.support_simple_spinner_dropdown_item,
            FilterFactory.FilterType.values())
    private val averageAdapter = ArrayAdapter<Int>(context,
            io.github.t3r1jj.ips.research.R.layout.support_simple_spinner_dropdown_item,
            arrayOf(2, 3, 4, 5, 6, 7, 8, 9, 10))

    override fun view() {
        linearLayout {
            padding(dip(8))
            size(BaseDSL.MATCH, BaseDSL.MATCH)
            DSL.orientation(LinearLayout.VERTICAL)
            textView {
                padding(dip(8))
                size(BaseDSL.MATCH, BaseDSL.WRAP)
                DSL.text("Choose pedometer algorithm")
            }
            spinner {
                padding(dip(8))
                size(BaseDSL.MATCH, BaseDSL.MATCH)
                weight(1f)
                DSL.adapter(algorithmAdapter)
                onItemSelected { a, _, _, _ ->
                    pedometer.filterFactory.filterType = a.selectedItem as FilterFactory.FilterType
                }
            }
            if (pedometer.filterFactory.filterType == FilterFactory.FilterType.MOVING_AVERAGE_FILTER) {
                linearLayout {
                    padding(dip(8), dip(8), dip(16), dip(8))
                    BaseDSL.size(BaseDSL.MATCH, BaseDSL.WRAP)
                    DSL.orientation(LinearLayout.HORIZONTAL)
                    textView {
                        BaseDSL.size(BaseDSL.WRAP, BaseDSL.WRAP)
                        DSL.text("Length of averaging window")
                    }
                    spinner {
                        size(BaseDSL.MATCH, BaseDSL.MATCH)
                        weight(1f)
                        DSL.adapter(averageAdapter)
                        onItemSelected { a, _, _, _ ->
                            pedometer.filterFactory.averagingWindowLength = a.selectedItem as Int
                        }
                    }
                }
            }
            linearLayout {
                BaseDSL.size(BaseDSL.MATCH, BaseDSL.WRAP)
                DSL.orientation(LinearLayout.HORIZONTAL)
                button {
                    size(0, BaseDSL.WRAP)
                    DSL.text("Generate")
                    onClick {
                        pedometer.userInputDialog?.dismiss()
                        pedometer.tester = PedometerTester(pedometer.filterFactory)
                        val data = pedometer.inertialData()
                        pedometer.tester.test(data)
                        val info = ByteArrayOutputStream()
                        pedometer.tester.saveOutputInfo(info)
                        val bottomSheet = BottomSheetDialog(context)
                        pedometer.userInputDialog = bottomSheet
                        bottomSheet.setContentView(PedometerBottomSheetDialog(context, info.toString(), pedometer))
                        bottomSheet.show()
                    }
                    BaseDSL.weight(1f)
                }

                button {
                    size(0, BaseDSL.WRAP)
                    DSL.text("Cancel")
                    onClick {
                        pedometer.userInputDialog?.dismiss()
                    }
                    BaseDSL.weight(1f)
                }
            }
        }
    }

}