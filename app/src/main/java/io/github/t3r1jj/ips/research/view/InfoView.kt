package io.github.t3r1jj.ips.research.view

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.widget.LinearLayout
import io.github.t3r1jj.ips.research.MainActivity
import trikita.anvil.BaseDSL
import trikita.anvil.BaseDSL.dip
import trikita.anvil.BaseDSL.sip
import trikita.anvil.DSL
import trikita.anvil.DSL.linearLayout
import trikita.anvil.DSL.textView

class InfoView(context: Context) : RenderableView(context) {
    override fun view() {
        linearLayout {
            BaseDSL.size(BaseDSL.MATCH, BaseDSL.WRAP)
            DSL.orientation(LinearLayout.VERTICAL)
            textView {
                BaseDSL.size(BaseDSL.MATCH, BaseDSL.WRAP)
                DSL.gravity(BaseDSL.CENTER_HORIZONTAL)
                DSL.text("Indoor Positioning System")
                DSL.textColor(ColorStateList.valueOf(Color.BLACK))
                BaseDSL.textSize(sip(18f))
            }
            textView {
                BaseDSL.size(BaseDSL.MATCH, BaseDSL.WRAP)
                DSL.gravity(BaseDSL.CENTER_HORIZONTAL)
                DSL.text("Author: Damian Terlecki")
            }
            textView {
                BaseDSL.size(BaseDSL.MATCH, BaseDSL.WRAP)
                DSL.gravity(BaseDSL.CENTER_HORIZONTAL)
                DSL.text("Created on Bialystok University of Technology, Faculty of Computer Science, Software Engineering")
            }
            textView {
                BaseDSL.size(BaseDSL.MATCH, BaseDSL.WRAP)
                DSL.gravity(BaseDSL.CENTER_HORIZONTAL)
                DSL.text("Licensed under GPL 3.0")
            }
            linearLayout {
                BaseDSL.size(BaseDSL.MATCH, BaseDSL.WRAP)
                BaseDSL.padding(0, dip(8), 0, 0)
                textView {
                    BaseDSL.size(BaseDSL.MATCH, BaseDSL.WRAP)
                    DSL.gravity(BaseDSL.CENTER_HORIZONTAL)
                    DSL.text("Components used:")
                    DSL.textColor(ColorStateList.valueOf(Color.BLACK))
                    BaseDSL.textSize(sip(16f))
                }
                DSL.orientation(LinearLayout.VERTICAL)
                for (info in MainActivity.licenses) {
                    linearLayout {
                        BaseDSL.size(BaseDSL.MATCH, BaseDSL.WRAP)
                        BaseDSL.padding(0, 0, 0, dip(8))
                        DSL.orientation(LinearLayout.VERTICAL)
                        textView {
                            BaseDSL.size(BaseDSL.MATCH, BaseDSL.WRAP)
                            DSL.gravity(BaseDSL.CENTER_HORIZONTAL)
                            DSL.text(info.title)
                        }
                        if (info.author != null) {
                            textView {
                                BaseDSL.size(BaseDSL.MATCH, BaseDSL.WRAP)
                                DSL.gravity(BaseDSL.CENTER_HORIZONTAL)
                                DSL.text(info.author)
                            }
                        }
                        if (info.license != null) {
                            textView {
                                BaseDSL.size(BaseDSL.MATCH, BaseDSL.WRAP)
                                DSL.gravity(BaseDSL.CENTER_HORIZONTAL)
                                DSL.text(info.license)
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