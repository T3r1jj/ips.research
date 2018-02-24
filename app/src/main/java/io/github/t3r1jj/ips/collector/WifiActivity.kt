package io.github.t3r1jj.ips.collector

import android.Manifest
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.text.InputType
import android.widget.Adapter
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Toast
import io.github.t3r1jj.ips.collector.model.Dao
import io.github.t3r1jj.ips.collector.model.Fingerprint
import io.github.t3r1jj.ips.collector.model.Sampler
import io.github.t3r1jj.ips.collector.model.WifiDataset
import trikita.anvil.Anvil
import trikita.anvil.BaseDSL.MATCH
import trikita.anvil.BaseDSL.WRAP
import trikita.anvil.DSL
import trikita.anvil.DSL.*
import trikita.anvil.RenderableAdapter
import java.util.*


class WifiActivity : AppCompatActivity() {

    var place = ""
    lateinit var sampler: Sampler
    lateinit var spinnerAdapter: Adapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sampler = Sampler(this)
        spinnerAdapter = ArrayAdapter<SamplingRate>(this, android.R.layout.simple_spinner_item, SamplingRate.values().toMutableList())
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
                            enabled(editingEnabled())
                        }
                    }

                    linearLayout {
                        orientation(LinearLayout.HORIZONTAL)
                        size(MATCH, WRAP)
                        textView {
                            size(WRAP, WRAP)
                            text("Number of objects: ")
                        }
                        editText {
                            size(MATCH, WRAP)
                            text(sampler.sampleCount.toString())
                            inputType(InputType.TYPE_CLASS_NUMBER)
                            onTextChanged {
                                sampler.sampleCount = it.toString().toInt()
                                println("SASSASASSAASSASAASASASASASASAS")
                            }
                            enabled(editingEnabled())
                        }
                    }
                    linearLayout {
                        orientation(LinearLayout.HORIZONTAL)
                        size(MATCH, WRAP)
                        textView {
                            size(WRAP, WRAP)
                            text("Sampling rate: ")
                        }
                        spinner {
                            size(MATCH, WRAP)
                            adapter(spinnerAdapter)
                            onItemSelected { a, v, pos, id ->
                                sampler.samplingRate = a.selectedItem as SamplingRate
                            }
                        }
                        enabled(editingEnabled())
                    }

                    linearLayout {
                        orientation(LinearLayout.HORIZONTAL)
                        size(MATCH, WRAP)
                        button {
                            size(0, WRAP)
                            text("Sample")
                            onClick {
                                try {
                                    sampler.startSampling()
                                } catch (ex: RuntimeException) {
                                    Toast.makeText(this@WifiActivity, ex.localizedMessage, Toast.LENGTH_LONG).show()
                                }
                            }
                            weight(0.5f)
                            enabled(editingEnabled())
                        }
                        button {
                            size(0, WRAP)
                            text("Stop")
                            onClick {
                                sampler.stopSampling()
                            }
                            weight(0.5f)
                            enabled(!editingEnabled())
                        }
                    }

                    listView {
                        orientation(LinearLayout.VERTICAL)
                        size(MATCH, 0)
                        weight(1f)
                        adapter(RenderableAdapter.withItems(sampler.fingerprints, { i, item ->
                            DSL.linearLayout {
                                DSL.textView {
                                    DSL.text(item.toString())
                                }
                            }
                        }))
                    }

                    button {
                        size(MATCH, WRAP)
                        text("Submit")
                        onClick {
                            Dao(this@WifiActivity)
                                    .save(WifiDataset(place, sampler.fingerprints))
                            sampler.finished = false
                        }
                        weight(0f)
                        enabled(sampler.finished && place.isNotBlank())
                    }
                }
            }
        })


        val PERMS_INITIAL = arrayOf<String>(Manifest.permission.ACCESS_FINE_LOCATION)
        ActivityCompat.requestPermissions(this, PERMS_INITIAL, 127)
    }

    override fun onPause() {
        super.onPause()
        sampler.stopSampling()
    }

    private fun editingEnabled() = !sampler.started || sampler.finished

    enum class SamplingRate(val delay: Long) {
        _1000MS(1000), _2000MS(2000), _5000MS(5000);
    }
}
