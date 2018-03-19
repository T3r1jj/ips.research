package io.github.t3r1jj.ips.research.view

import android.annotation.SuppressLint
import android.content.Context
import android.widget.ArrayAdapter
import android.widget.CompoundButton
import android.widget.LinearLayout
import io.github.t3r1jj.ips.research.DatabaseActivity
import io.github.t3r1jj.ips.research.model.algorithm.ArffTransform
import trikita.anvil.BaseDSL
import trikita.anvil.DSL
import trikita.anvil.DSL.*

@SuppressLint("ViewConstructor")
class ArffDialog(context: Context, val arffActivity: DatabaseActivity) : RenderableView(context) {
    private val deviceAdapter = ArrayAdapter<String>(context,
            io.github.t3r1jj.ips.research.R.layout.support_simple_spinner_dropdown_item,
            arffActivity.dao.findAll().values.map { it.device }.distinct().sorted())
    private val dataTypeAdapter = ArrayAdapter<ArffTransform.AttributeDataType>(context,
            io.github.t3r1jj.ips.research.R.layout.support_simple_spinner_dropdown_item,
            ArffTransform.AttributeDataType.values())
    private val firstRegex = "(eduroam)"
    private val secondRegex = "(eduroam|dziekanat|pb-guest|.*hotspot.*)"
    private var isFirstSwitchOn = true
    private var isSecondSwitchOn = false
    private var isThirdSwitchOn = false
    private var customRegex = ""
    private var device = deviceAdapter.getItem(0)
    private var attributeDataType = dataTypeAdapter.getItem(0)
    private var averageTests = false

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
                    onCheckedChange { c: CompoundButton?, b: Boolean ->
                        isFirstSwitchOn = b
                    }
                }
                radioButton {
                    padding(dip(8))
                    size(BaseDSL.WRAP, BaseDSL.WRAP)
                    DSL.text(secondRegex)
                    DSL.checked(isSecondSwitchOn)
                    onCheckedChange { c: CompoundButton?, b: Boolean ->
                        isSecondSwitchOn = b
                    }
                }
                radioButton {
                    padding(dip(8))
                    size(BaseDSL.WRAP, BaseDSL.WRAP)
                    DSL.text("Custom (Java regex, fill in below):")
                    DSL.checked(isThirdSwitchOn)
                    onCheckedChange { c: CompoundButton?, b: Boolean ->
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
                    DSL.text("Training dataset from device (the rest will be testing):")
                }
            }
            spinner {
                padding(dip(8))
                size(BaseDSL.MATCH, BaseDSL.WRAP)
                DSL.adapter(deviceAdapter)
                onItemSelected { a, _, _, _ ->
                    device = a.selectedItem.toString()
                }
            }
            linearLayout {
                padding(dip(8))
                BaseDSL.size(BaseDSL.MATCH, BaseDSL.WRAP)
                DSL.orientation(LinearLayout.HORIZONTAL)
                textView {
                    size(BaseDSL.WRAP, BaseDSL.WRAP)
                    DSL.text("Attributes:")
                }

                spinner {
                    padding(dip(8))
                    size(BaseDSL.MATCH, BaseDSL.WRAP)
                    DSL.adapter(dataTypeAdapter)
                    onItemSelected { a, _, _, _ ->
                        attributeDataType = a.selectedItem as ArffTransform.AttributeDataType
                    }
                }
            }
            linearLayout {
                padding(dip(8))
                BaseDSL.size(BaseDSL.MATCH, BaseDSL.WRAP)
                DSL.orientation(LinearLayout.HORIZONTAL)
                textView {
                    size(BaseDSL.WRAP, BaseDSL.WRAP)
                    DSL.text("Average test sets:")
                }
                switchView {
                    size(BaseDSL.MATCH, BaseDSL.WRAP)
                    DSL.checked(averageTests)
                    onCheckedChange { _: CompoundButton?, b: Boolean ->
                        averageTests = b
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
                        arffActivity.generateArff(regex, attributeDataType, averageTests, device)
                    }
                    BaseDSL.weight(1f)
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