package io.github.t3r1jj.ips.research.view

import android.content.Context
import io.github.t3r1jj.ips.research.R


class I18nUtils {
    companion object {
        fun tryI18nException(context: Context, e: Exception): String {
            val str = e.toString()
            return " " + when {
                str.contains("startScan() fail") -> context.resources.getString(R.string.throw_scan_fail)
                str.contains("WiFi not enabled") -> context.resources.getString(R.string.throw_wifi_not_enabled)
                str.contains("Trying to transform data that does not have common DatasetType")
                -> context.resources.getString(R.string.throw_dataset_type)
                str.contains("No training data") -> context.resources.getString(R.string.throw_no_training_data)
                else -> e.toString()
            }
        }
    }
}