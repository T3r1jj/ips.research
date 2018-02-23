package io.github.t3r1jj.ips.collector

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.InputType
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import trikita.anvil.BaseDSL
import trikita.anvil.BaseDSL.MATCH
import trikita.anvil.BaseDSL.WRAP
import trikita.anvil.DSL.*
import trikita.anvil.RenderableView


class WifiActivity : AppCompatActivity() {

    var place = ""
    var samplingRate = SamplingRate._1000MS
    var objectsNumber = 10

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                        textView {
                            size(WRAP, WRAP)
                            text("Number of objects: ")
                        }
                        editText {
                            size(MATCH, WRAP)
                            text(objectsNumber.toString())
                            inputType(InputType.TYPE_CLASS_NUMBER)
                            onTextChanged {
                                objectsNumber = it.toString().toInt()
                            }
                        }
                    }
                    linearLayout {
                        orientation(LinearLayout.HORIZONTAL)
                        textView {
                            size(WRAP, WRAP)
                            text("Sampling rate: ")
                        }
                        spinner {
                            size(MATCH, WRAP)
                            adapter(ArrayAdapter<SamplingRate>(this@WifiActivity, android.R.layout.simple_spinner_item, SamplingRate.values().toMutableList()))
                        }
                    }

                    button {
                        size(MATCH, WRAP)
                        text("Collect")
                        onClick { v -> finish() }
                    }

                    linearLayout {
                        orientation(LinearLayout.VERTICAL)
                        size(MATCH, 0)
                        weight(1f)
                    }

                    button {
                        size(MATCH, WRAP)
                        text("Submit")
                        onClick { v -> finish() }
                        weight(0f)
                    }
                }
            }
        })
    }

    enum class SamplingRate() {
        _1000MS, _2000MS, _5000MS
    }
}
