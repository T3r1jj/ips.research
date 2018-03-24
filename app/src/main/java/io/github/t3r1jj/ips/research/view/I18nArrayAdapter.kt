package io.github.t3r1jj.ips.research.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

class I18nArrayAdapter<T>(context: Context?, objects: Array<out T>?) : ArrayAdapter<T>(context,
        io.github.t3r1jj.ips.research.R.layout.support_simple_spinner_dropdown_item, objects) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        return applyI18n(super.getView(position, convertView, parent), position)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup?): View {
        return applyI18n(super.getDropDownView(position, convertView, parent), position)
    }

    private fun applyI18n(convertView: View, position: Int): View {
        val textView = convertView.findViewById<TextView>(android.R.id.text1)
        val item = getItem(position)
        textView.text = getStringResourceByName(item.toString().toUpperCase())
        return convertView
    }

    private fun getStringResourceByName(aString: String): String {
        val packageName = context.packageName
        val resId = context.resources.getIdentifier(aString, "string", packageName)
        return context.getString(resId)
    }
}