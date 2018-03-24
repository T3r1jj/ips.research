package io.github.t3r1jj.ips.research.model.data

import android.content.Context

enum class DatasetType {
    WIFI, INERTIAL, MAGNETIC;

    fun toString(context: Context): String {
        val packageName = context.packageName
        val resId = context.resources.getIdentifier(this.toString(), "string", packageName)
        return context.getString(resId)
    }
}