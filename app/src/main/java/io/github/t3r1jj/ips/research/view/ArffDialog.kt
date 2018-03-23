package io.github.t3r1jj.ips.research.view

import android.annotation.SuppressLint
import android.content.Context
import android.widget.ArrayAdapter
import android.widget.CompoundButton
import android.widget.LinearLayout
import io.github.t3r1jj.ips.research.DatabaseActivity
import io.github.t3r1jj.ips.research.model.algorithm.ArffTransform
import io.github.t3r1jj.ips.research.model.data.WifiDataset
import trikita.anvil.BaseDSL
import trikita.anvil.DSL
import trikita.anvil.DSL.*
import java.text.SimpleDateFormat
import java.util.*

@SuppressLint("ViewConstructor")
class ArffDialog(context: Context, private val arffActivity: DatabaseActivity) : RenderableView(context) {

    companion object {
        val dateFormatter = createDateFormat()

        @SuppressLint("SimpleDateFormat")
        private fun createDateFormat(): SimpleDateFormat {
            val dateFormatter = SimpleDateFormat("dd/MM")
            dateFormatter.timeZone = Calendar.getInstance().timeZone
            return dateFormatter
        }
    }

    private val groups = initGroups()

    private val groupAdapter = ArrayAdapter<String>(context,
            io.github.t3r1jj.ips.research.R.layout.support_simple_spinner_dropdown_item,
            groups.keys.toList())
    private val dataTypeAdapter = ArrayAdapter<ArffTransform.AttributeDataType>(context,
            io.github.t3r1jj.ips.research.R.layout.support_simple_spinner_dropdown_item,
            ArffTransform.AttributeDataType.values())
    private val trainAdapter = ArrayAdapter<ArffTransform.Processing>(context,
            io.github.t3r1jj.ips.research.R.layout.support_simple_spinner_dropdown_item,
            ArffTransform.Processing.values())
    private val testAdapter = ArrayAdapter<ArffTransform.Processing>(context,
            io.github.t3r1jj.ips.research.R.layout.support_simple_spinner_dropdown_item,
            ArffTransform.Processing.values())
    private val firstRegex = "(eduroam)"
    private val secondRegex = "(eduroam|dziekanat|pb-guest|.*hotspot.*)"
    private var isFirstSwitchOn = true
    private var isSecondSwitchOn = false
    private var isThirdSwitchOn = false
    private var customRegex = ""
    private var group = groupAdapter.getItem(0)
    private var opts = ArffTransform.Options(dataTypeAdapter.getItem(0),
            trainAdapter.getItem(0),
            testAdapter.getItem(0))

    private fun initGroups(): Map<String, List<WifiDataset>> {
        val groups = arffActivity.wifiData()
                .groupBy { "ALL " + it.device }
                .plus(arffActivity.wifiData().groupBy {
                    dateFormatter.format(it.timestamp) + " " + it.device
                })
        val pairs = mutableSetOf<Pair<String, String>>()
        for (key in groups.keys) {
            groups.keys
                    .filter { key != it }
                    .mapTo(pairs) { Pair(key, it) }
        }
        return groups.plus(
                pairs.map { Pair(it.first + " + " + it.second, groups[it.first]!!.plus(groups[it.second]!!)) }
        )
    }

    override fun view() {
        linearLayout {
            padding(dip(8))
            size(BaseDSL.MATCH, BaseDSL.MATCH)
            DSL.orientation(LinearLayout.VERTICAL)
            textView {
                padding(dip(8))
                size(BaseDSL.MATCH, BaseDSL.WRAP)
                DSL.text("Transform WiFi data into ARFF for Weka manual research")
            }
            textView {
                padding(dip(8))
                size(BaseDSL.MATCH, BaseDSL.WRAP)
                DSL.text("SSID regex:")
            }
            radioGroup {
                radioButton {
                    padding(dip(8))
                    size(BaseDSL.WRAP, BaseDSL.WRAP)
                    DSL.text(firstRegex)
                    DSL.checked(isFirstSwitchOn)
                    onCheckedChange { _: CompoundButton?, b: Boolean ->
                        isFirstSwitchOn = b
                    }
                }
                radioButton {
                    padding(dip(8))
                    size(BaseDSL.WRAP, BaseDSL.WRAP)
                    DSL.text(secondRegex)
                    DSL.checked(isSecondSwitchOn)
                    onCheckedChange { _: CompoundButton?, b: Boolean ->
                        isSecondSwitchOn = b
                    }
                }
                radioButton {
                    padding(dip(8))
                    size(BaseDSL.WRAP, BaseDSL.WRAP)
                    DSL.text("Custom (Java regex, fill in below):")
                    DSL.checked(isThirdSwitchOn)
                    onCheckedChange { _: CompoundButton?, b: Boolean ->
                        isThirdSwitchOn = b
                    }
                }
                linearLayout {
                    BaseDSL.size(BaseDSL.MATCH, BaseDSL.WRAP)
                    DSL.orientation(LinearLayout.HORIZONTAL)
                    editText {
                        padding(dip(8))
                        size(0, BaseDSL.WRAP)
                        BaseDSL.weight(1f)
                        onTextChanged {
                            customRegex = it.toString()
                        }
                    }
                }
            }
            linearLayout {
                padding(dip(8))
                BaseDSL.size(BaseDSL.MATCH, BaseDSL.WRAP)
                DSL.orientation(LinearLayout.HORIZONTAL)
                textView {
                    size(BaseDSL.MATCH, BaseDSL.WRAP)
                    DSL.text("Training dataset group (remaining will be tested):")
                }
            }
            spinner {
                size(BaseDSL.MATCH, BaseDSL.WRAP)
                DSL.adapter(groupAdapter)
                onItemSelected { a, _, _, _ ->
                    group = a.selectedItem.toString()
                }
            }
            linearLayout {
                BaseDSL.size(BaseDSL.MATCH, BaseDSL.WRAP)
                DSL.orientation(LinearLayout.HORIZONTAL)
                textView {
                    size(BaseDSL.WRAP, BaseDSL.WRAP)
                    DSL.text("Attributes:")
                }

                spinner {
                    size(BaseDSL.MATCH, BaseDSL.WRAP)
                    DSL.adapter(dataTypeAdapter)
                    onItemSelected { a, _, _, _ ->
                        opts.attributeDataType = a.selectedItem as ArffTransform.AttributeDataType
                    }
                }
            }
            linearLayout {
                BaseDSL.size(BaseDSL.MATCH, BaseDSL.WRAP)
                DSL.orientation(LinearLayout.HORIZONTAL)
                textView {
                    size(BaseDSL.WRAP, BaseDSL.WRAP)
                    DSL.text("Train processing:")
                }
                spinner {
                    size(BaseDSL.MATCH, BaseDSL.WRAP)
                    adapter(trainAdapter)
                    onItemSelected { a, _, _, _ ->
                        opts.trainProcessing = a.selectedItem as ArffTransform.Processing
                    }
                }
            }
            linearLayout {
                BaseDSL.size(BaseDSL.MATCH, BaseDSL.WRAP)
                DSL.orientation(LinearLayout.HORIZONTAL)
                textView {
                    size(BaseDSL.WRAP, BaseDSL.WRAP)
                    DSL.text("Test processing:")
                }
                spinner {
                    size(BaseDSL.MATCH, BaseDSL.WRAP)
                    adapter(testAdapter)
                    onItemSelected { a, _, _, _ ->
                        opts.testProcessing = a.selectedItem as ArffTransform.Processing
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
                        val regex = when {
                            isFirstSwitchOn -> firstRegex
                            isSecondSwitchOn -> secondRegex
                            else -> customRegex
                        }
                        arffActivity.generateArff(regex, opts, groups[group]!!)
                    }
                    BaseDSL.weight(1f)
                }

                button {
                    size(BaseDSL.WRAP, BaseDSL.WRAP)
                    DSL.text("Test")
                    onClick {
                        val regex = when {
                            isFirstSwitchOn -> firstRegex
                            isSecondSwitchOn -> secondRegex
                            else -> customRegex
                        }
                        arffActivity.testArff(regex, opts, groups[group]!!)
                    }
                }

                button {
                    size(0, BaseDSL.WRAP)
                    DSL.text("Cancel")
                    onClick {
                        arffActivity.userInputDialog?.dismiss()
                    }
                    BaseDSL.weight(1f)
                }
            }
        }
    }

}