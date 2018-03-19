package io.github.t3r1jj.ips.research.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import trikita.anvil.Anvil
import trikita.anvil.BaseDSL

// Fixes bug with null observer on detachment from window
abstract class RenderableView(context: Context) : trikita.anvil.RenderableView(context) {
    override fun onDetachedFromWindow() {
        Anvil.unmount(this, false)
        super.onDetachedFromWindow()
    }

    fun customView(view: View) {
        if (view.parent is ViewGroup) {
            (view.parent as ViewGroup).removeView(view)
        }
        Anvil.currentView<ViewGroup>().addView(view, ViewGroup.LayoutParams(BaseDSL.MATCH, BaseDSL.MATCH))
    }
}