package io.github.t3r1jj.ips.research.model.algorithm

import io.github.t3r1jj.ips.research.model.algorithm.filter.SignalFilter
import io.github.t3r1jj.ips.research.model.collector.SensorSample

open class Pedometer(private val filter: SignalFilter) {
    constructor(filter: SignalFilter, cache: Boolean) : this(filter) {
        this.cache = cache
    }

    constructor(filter: SignalFilter, constantSensitivity: Float) : this(filter) {
        this.constantSensitivity = constantSensitivity
    }

    companion object {
        const val NORMALIZATION_COEFFICIENT = 9.81f
        const val TIME_WINDOW_NS = 200000000
    }

    val t = mutableListOf<Long>()
    val a = mutableListOf<FloatArray>()
    val aMagnitudes = mutableListOf<Float>()
    val aFilteredMagnitudes = mutableListOf<Float>()
    val min = mutableListOf<Float>()
    val max = mutableListOf<Float>()
    private var isAbove = false
    private var sensitivity = 0f
    private var lastStepTime = 0L
    var sensitivities = mutableListOf<Float>()
    var steps = mutableListOf<Int>()
    val stepCount
        get() = steps.last()
    var cache = true
    var constantSensitivity = -1f

    fun processSample(sample: SensorSample) {
        t.add(sample.timestamp)
        a.add(sample.data)
        aMagnitudes.add(Math.sqrt(sample.data.sumByDouble { it.toDouble() * it.toDouble() }).toFloat())

        processLastSample()
    }

    private fun processLastSample() {
        aFilteredMagnitudes.add(filter.apply(aMagnitudes.last()))
        val timeWindowIndex = timeWindowIndex()
        val aRecentMagnitudes = aFilteredMagnitudes.subList(timeWindowIndex, t.size)
        updateSensitivity(aRecentMagnitudes)
        min.add(aRecentMagnitudes.min()!!)
        max.add(aRecentMagnitudes.max()!!)
        filterSteps()
        if (!cache) {
            removeOld(timeWindowIndex)
        }
    }

    private fun removeOld(timeWindowIndex: Int) {
        for (i in 0 until timeWindowIndex) {
            t.removeAt(0)
            a.removeAt(0)
            aMagnitudes.removeAt(0)
            aFilteredMagnitudes.removeAt(0)
            min.removeAt(0)
            max.removeAt(0)
            sensitivities.removeAt(0)
            steps.removeAt(0)
        }
    }

    private fun filterSteps() {
        val min = min.last()
        val max = max.last()
        val threshold = (min + max) / 2f
        var stepCount = steps.lastOrNull() ?: 0
        sensitivities.add(sensitivity)
        if (stepNotTooFast() && aboveSensitivity(max, min)) {
            val y = aFilteredMagnitudes.last()
            if (isAbove && (y < threshold)) {
                isAbove = false
                lastStepTime = t.last()
                stepCount++
            } else if (!isAbove && (y > threshold)) {
                isAbove = true
            }
        }
        steps.add(stepCount)
    }

    private fun stepNotTooFast() = t.last() - lastStepTime > TIME_WINDOW_NS

    private fun aboveSensitivity(max: Float, min: Float) = (max - min) > sensitivity

    private fun timeWindowIndex(): Int {
        return (t.lastIndex downTo 0).firstOrNull { (t.last() - t[it]) > TIME_WINDOW_NS }
                ?: 0
    }

    private fun updateSensitivity(values: List<Float>) {
        if (constantSensitivity >= 0.0) {
            sensitivity = constantSensitivity
            return
        }
        var sum = 0f
        var sumSqr = 0f
        for (value in values) {
            sum += value
            sumSqr += value * value
        }
        val sd = ((sum * sum) - sumSqr) / values.size
        sensitivity = (Math.sqrt(sd.toDouble()) / NORMALIZATION_COEFFICIENT).toFloat()
    }

}