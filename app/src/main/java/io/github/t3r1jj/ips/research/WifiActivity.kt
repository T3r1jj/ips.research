package io.github.t3r1jj.ips.research

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.InputType
import android.widget.Adapter
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Toast
import io.github.t3r1jj.ips.research.model.Dao
import io.github.t3r1jj.ips.research.model.collector.WifiSampler
import io.github.t3r1jj.ips.research.model.data.WifiDataset
import io.github.t3r1jj.ips.research.view.I18nUtils
import io.github.t3r1jj.ips.research.view.RenderableView
import trikita.anvil.BaseDSL.MATCH
import trikita.anvil.BaseDSL.WRAP
import trikita.anvil.DSL.*
import trikita.anvil.RenderableAdapter

class WifiActivity : AppCompatActivity() {

    var place = ""
    var submitted = false
    lateinit var sampler: WifiSampler
    lateinit var spinnerAdapter: Adapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sampler = WifiSampler(this)
        spinnerAdapter = ArrayAdapter<SamplingRate>(this,
                io.github.t3r1jj.ips.research.R.layout.support_simple_spinner_dropdown_item,
                SamplingRate.values())
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
                            text(R.string.place_name)
                        }
                        editText {
                            size(MATCH, WRAP)
                            text(place)
                            onTextChanged {
                                place = it.toString()
                            }
                            enabled(!sampler.running)
                        }
                    }

                    linearLayout {
                        orientation(LinearLayout.HORIZONTAL)
                        size(MATCH, WRAP)
                        textView {
                            size(WRAP, WRAP)
                            text(R.string.number_of_scans)
                        }
                        editText {
                            size(MATCH, WRAP)
                            text(sampler.sampleCount.toString())
                            inputType(InputType.TYPE_CLASS_NUMBER)
                            onTextChanged {
                                try {
                                    sampler.sampleCount = it.toString().toInt()
                                } catch (nfe: NumberFormatException) {
                                }
                            }
                            enabled(!sampler.running)
                        }
                    }
                    linearLayout {
                        orientation(LinearLayout.HORIZONTAL)
                        size(MATCH, WRAP)
                        textView {
                            size(WRAP, WRAP)
                            text(R.string.sampling_rate)
                        }
                        spinner {
                            size(MATCH, WRAP)
                            adapter(spinnerAdapter)
                            onItemSelected { a, _, _, _ ->
                                sampler.samplingRate = a.selectedItem as SamplingRate
                            }
                        }
                        enabled(!sampler.running)
                    }

                    linearLayout {
                        orientation(LinearLayout.HORIZONTAL)
                        size(MATCH, WRAP)
                        button {
                            size(0, WRAP)
                            text(R.string.sample)
                            onClick {
                                submitted = false
                                try {
                                    sampler.startSampling()
                                } catch (ex: RuntimeException) {
                                    Toast.makeText(this@WifiActivity,
                                            I18nUtils.tryI18nException(this@WifiActivity, ex),
                                            Toast.LENGTH_LONG).show()
                                }
                            }
                            weight(0.5f)
                            enabled(!sampler.running)
                        }
                        button {
                            size(0, WRAP)
                            text(R.string.stop)
                            onClick {
                                sampler.stopSampling()
                            }
                            weight(0.5f)
                            enabled(sampler.running)
                        }
                    }

                    listView {
                        orientation(LinearLayout.VERTICAL)
                        size(MATCH, 0)
                        weight(1f)
                        adapter(RenderableAdapter.withItems(sampler.fingerprints, { _, item ->
                            linearLayout {
                                textView {
                                    text(item.toString())
                                }
                            }
                        }))
                    }

                    button {
                        size(MATCH, WRAP)
                        text(R.string.submit)
                        onClick {
                            if (place.isBlank()) {
                                Toast.makeText(this@WifiActivity, R.string.place_name_required, Toast.LENGTH_LONG).show()
                            } else {
                                val data = WifiDataset(place, sampler.fingerprints)
                                data.iterations = sampler.sampleIndex
                                Dao(this@WifiActivity)
                                        .save(data)
                                submitted = true
                            }
                        }
                        weight(0f)
                        enabled(sampler.fingerprints.isNotEmpty() && !sampler.running && !submitted)
                    }
                }
            }
        })
    }

    override fun onStop() {
        super.onStop()
        sampler.stopSampling()
    }

    enum class SamplingRate(val delay: Long) {
        _500MS(500), _1000MS(1000), _5000MS(5000);
    }
}
