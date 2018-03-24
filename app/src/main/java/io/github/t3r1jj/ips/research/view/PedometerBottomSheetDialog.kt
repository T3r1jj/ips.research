package io.github.t3r1jj.ips.research.view

import android.annotation.SuppressLint
import android.content.Context
import android.support.v4.widget.NestedScrollView
import android.widget.LinearLayout
import android.widget.TextView
import io.github.t3r1jj.ips.research.DatabaseActivity
import io.github.t3r1jj.ips.research.R
import trikita.anvil.DSL.*

@SuppressLint("ViewConstructor")
class PedometerBottomSheetDialog(context: Context, private val text: String, private val pedometer: DatabaseActivity) : RenderableView(context) {
    private var withButtons: Boolean = true

    constructor(context: Context, pedometer: DatabaseActivity, text: String, withButtons: Boolean) : this(context, text, pedometer) {
        this.withButtons = withButtons
    }

    override fun view() {
        linearLayout {
            padding(dip(6))
            size(MATCH, WRAP)
            orientation(LinearLayout.VERTICAL)
            if (withButtons) {
                linearLayout {
                    size(MATCH, WRAP)
                    orientation(LinearLayout.HORIZONTAL)
                    button {
                        size(0, WRAP)
                        weight(1f)
                        text(R.string.info_to_file)
                        onClick {
                            pedometer.onPedometerInfoClick()
                        }
                    }
                    button {
                        size(0, WRAP)
                        weight(1f)
                        text(R.string.output_to_file)
                        onClick {
                            pedometer.onPedometerOutputClick()
                        }
                    }
                    button {
                        size(0, WRAP)
                        weight(1f)
                        text(R.string.debug_to_file)
                        onClick {
                            pedometer.onPedometerDebugClick()
                        }
                    }
                }
            }
            val scrollView = NestedScrollView(context)
            val textView = TextView(context)
            textView.text = text
            scrollView.addView(textView)
            customView(scrollView)
        }
    }
}