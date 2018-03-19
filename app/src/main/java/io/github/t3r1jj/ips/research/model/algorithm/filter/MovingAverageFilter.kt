package io.github.t3r1jj.ips.research.model.algorithm.filter

import java.util.*

class MovingAverageFilter(length: Int) : SignalFilter {
    private val recentValues = LimitedQueue<Float>(length)
    override fun apply(value: Float): Float {
        recentValues.add(value)
        return recentValues.sumByDouble { it.toDouble() }.div(recentValues.size).toFloat()
    }

    override fun onVarianceUpdate(variance: Float) {
    }

    inner class LimitedQueue<E>(private val limit: Int) : LinkedList<E>() {

        override fun add(element: E): Boolean {
            val added = super.add(element)
            while (added && (size > limit)) {
                super.remove()
            }
            return added
        }
    }
}