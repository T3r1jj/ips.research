package io.github.t3r1jj.ips.research.view

import android.content.Context
import trikita.anvil.Anvil

// Fixes bug with null observer on detachment from window
abstract class RenderableView(context: Context) : trikita.anvil.RenderableView(context) {
    override fun onDetachedFromWindow() {
        Anvil.unmount(this, false)
        super.onDetachedFromWindow()
    }
}