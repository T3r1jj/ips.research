package io.github.t3r1jj.ips.research.view

import android.annotation.SuppressLint
import android.content.Context
import android.widget.ArrayAdapter
import android.widget.CompoundButton
import android.widget.LinearLayout
import io.github.t3r1jj.ips.research.DatabaseActivity
import io.github.t3r1jj.ips.research.R
import io.github.t3r1jj.ips.research.model.algorithm.ArffTransform
import io.github.t3r1jj.ips.research.model.data.WifiDataset
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
    private val trainAdapter = I18nArrayAdapter(context,
            ArffTransform.Processing.values())
    private val testAdapter = I18nArrayAdapter(context,
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
                .groupBy { "(*) " + it.device }
                .toSortedMap()
                .plus(arffActivity.wifiData().groupBy {
                    dateFormatter.format(it.timestamp)
                })
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
            size(MATCH, MATCH)
            orientation(LinearLayout.VERTICAL)
            textView {
                padding(dip(8))
                size(MATCH, WRAP)
                text(R.string.transform_arff_description)
            }
            textView {
                padding(dip(8))
                size(MATCH, WRAP)
                text(R.string.ssid_regex)
            }
            radioGroup {
                radioButton {
                    padding(dip(8))
                    size(WRAP, WRAP)
                    text(firstRegex)
                    checked(isFirstSwitchOn)
                    onCheckedChange { _: CompoundButton?, b: Boolean ->
                        isFirstSwitchOn = b
                    }
                }
                radioButton {
                    padding(dip(8))
                    size(WRAP, WRAP)
                    text(secondRegex)
                    checked(isSecondSwitchOn)
                    onCheckedChange { _: CompoundButton?, b: Boolean ->
                        isSecondSwitchOn = b
                    }
                }
                radioButton {
                    padding(dip(8))
                    size(WRAP, WRAP)
                    text(R.string.custom_regex)
                    checked(isThirdSwitchOn)
                    onCheckedChange { _: CompoundButton?, b: Boolean ->
                        isThirdSwitchOn = b
                    }
                }
                linearLayout {
                    size(MATCH, WRAP)
                    orientation(LinearLayout.HORIZONTAL)
                    editText {
                        padding(dip(8))
                        size(0, WRAP)
                        weight(1f)
                        onTextChanged {
                            customRegex = it.toString()
                        }
                    }
                }
            }
            linearLayout {
                padding(dip(8))
                size(MATCH, WRAP)
                orientation(LinearLayout.HORIZONTAL)
                textView {
                    size(MATCH, WRAP)
                    text(R.string.training_selection_description)
                }
            }
            spinner {
                size(MATCH, WRAP)
                adapter(groupAdapter)
                onItemSelected { a, _, _, _ ->
                    group = a.selectedItem.toString()
                }
            }
            linearLayout {
                padding(dip(8), 0, 0, 0)
                size(MATCH, WRAP)
                orientation(LinearLayout.HORIZONTAL)
                textView {
                    size(WRAP, WRAP)
                    text(R.string.attributes)
                }

                spinner {
                    size(MATCH, WRAP)
                    adapter(dataTypeAdapter)
                    onItemSelected { a, _, _, _ ->
                        opts.attributeDataType = a.selectedItem as ArffTransform.AttributeDataType
                    }
                }
            }
            linearLayout {
                padding(dip(8), 0, 0, 0)
                size(MATCH, WRAP)
                orientation(LinearLayout.HORIZONTAL)
                textView {
                    size(WRAP, WRAP)
                    text(R.string.train_processing)
                }
                spinner {
                    size(MATCH, WRAP)
                    adapter(trainAdapter)
                    onItemSelected { a, _, _, _ ->
                        opts.trainProcessing = a.selectedItem as ArffTransform.Processing
                    }
                }
            }
            linearLayout {
                padding(dip(8), 0, 0, dip(8))
                size(MATCH, WRAP)
                orientation(LinearLayout.HORIZONTAL)
                textView {
                    size(WRAP, WRAP)
                    text(R.string.test_processing)
                }
                spinner {
                    size(MATCH, WRAP)
                    adapter(testAdapter)
                    onItemSelected { a, _, _, _ ->
                        opts.testProcessing = a.selectedItem as ArffTransform.Processing
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
                        val regex = when {
                            isFirstSwitchOn -> firstRegex
                            isSecondSwitchOn -> secondRegex
                            else -> customRegex
                        }
                        arffActivity.generateArff(regex, opts, groups[group]!!)
                    }
                    weight(1f)
                }

                button {
                    size(WRAP, WRAP)
                    text(R.string.test)
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
                    size(0, WRAP)
                    text(R.string.cancel)
                    onClick {
                        arffActivity.userInputDialog?.dismiss()
                    }
                    weight(1f)
                }
            }
        }
    }

}