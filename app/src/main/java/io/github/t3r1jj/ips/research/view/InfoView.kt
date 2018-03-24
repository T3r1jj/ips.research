package io.github.t3r1jj.ips.research.view

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.widget.LinearLayout
import io.github.t3r1jj.ips.research.MainActivity
import io.github.t3r1jj.ips.research.R
import trikita.anvil.BaseDSL.dip
import trikita.anvil.BaseDSL.sip
import trikita.anvil.DSL.*

class InfoView(context: Context) : RenderableView(context) {
    private val additionalInfo: List<Model>

    init {
        additionalInfo = listOf(
                Model("Android + Kotlin"),
                Model(context.getString(R.string.tested_with_)),
                Model(context.getString(R.string.generated_test_cases_))
        )
    }

    override fun view() {
        linearLayout {
            padding(dip(6))
            size(MATCH, WRAP)
            orientation(LinearLayout.VERTICAL)
            textView {
                size(MATCH, WRAP)
                gravity(CENTER_HORIZONTAL)
                text(R.string.app_name)
                textColor(ColorStateList.valueOf(Color.BLACK))
                textSize(sip(18f))
            }
            textView {
                size(MATCH, WRAP)
                gravity(CENTER_HORIZONTAL)
                text(resources.getString(R.string.author) + ": Damian Terlecki")
            }
            textView {
                size(MATCH, WRAP)
                gravity(CENTER_HORIZONTAL)
                text(R.string.created_on)
            }
            textView {
                size(MATCH, WRAP)
                gravity(CENTER_HORIZONTAL)
                text(R.string.licensed_under)
            }
            linearLayout {
                size(MATCH, WRAP)
                padding(0, dip(8), 0, 0)
                textView {
                    size(MATCH, WRAP)
                    gravity(CENTER_HORIZONTAL)
                    text(R.string.components_used)
                    textColor(ColorStateList.valueOf(Color.BLACK))
                    textSize(sip(16f))
                }
                orientation(LinearLayout.VERTICAL)
                for (info in MainActivity.licenses.plus(additionalInfo)) {
                    linearLayout {
                        size(MATCH, WRAP)
                        padding(0, 0, 0, dip(8))
                        orientation(LinearLayout.VERTICAL)
                        textView {
                            size(MATCH, WRAP)
                            gravity(CENTER_HORIZONTAL)
                            text(info.title)
                        }
                        if (info.author != null) {
                            textView {
                                size(MATCH, WRAP)
                                gravity(CENTER_HORIZONTAL)
                                text(info.author)
                            }
                        }
                        if (info.license != null) {
                            textView {
                                size(MATCH, WRAP)
                                gravity(CENTER_HORIZONTAL)
                                text(info.license)
                            }
                        }
                    }
                }
            }
        }
    }

    data class Model(val title: String, var author: String?, var license: String?) {
        constructor(title: String) : this(title, null, null)
    }
}