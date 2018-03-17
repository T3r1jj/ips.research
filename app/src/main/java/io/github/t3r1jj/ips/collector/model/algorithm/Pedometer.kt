package io.github.t3r1jj.ips.collector.model.algorithm

import io.github.t3r1jj.ips.collector.model.collector.SensorSample

class Pedometer {
    companion object {
        const val NORMALIZATION_COEFFICIENT = 9.81f
        const val TIME_WINDOW_NS = 200000000
    }

    private val filter = KalmanFilter()
    val t = mutableListOf<Long>()
    val a = mutableListOf<FloatArray>()
    val aMagnitudes = mutableListOf<Float>()
    val aFilteredMagnitudes = mutableListOf<Float>()
    val aNormalizedMagnitudes = mutableListOf<Float>()
    val min = mutableListOf<Float>()
    val max = mutableListOf<Float>()
    private var isAbove = false
    private var sensitivity = 1 / 30f
    var sensitivities = mutableListOf<Float>()
    var steps = mutableListOf<Int>()
    val stepCount
        get() = steps.last()

    fun processSample(sample: SensorSample) {
        t.add(sample.timestamp)
        a.add(sample.data)
        aMagnitudes.add(Math.sqrt(sample.data.sumByDouble { it.toDouble() * it.toDouble() }).toFloat())

        processLastSample()
    }

    private fun lowPassFilter(values: List<Float>): Float {
        val divider = when (t.lastIndex) {
            1 -> 1
            2 -> 3
            3 -> 6
            4 -> 10
            5 -> 15
            6 -> 19
            7 -> 22
            8 -> 24
            else -> 25
        }
        return ((t.lastIndex downTo t.lastIndex - 3).sumByDouble {
            when {
                it < 0 -> 0.toDouble()
                it == (t.lastIndex - 1) -> 2 * values[it].toDouble()
                it == (t.lastIndex - 2) -> 3 * values[it].toDouble()
                it == (t.lastIndex - 3) -> 4 * values[it].toDouble()
                it == (t.lastIndex - 4) -> 5 * values[it].toDouble()
                it == (t.lastIndex - 5) -> 4 * values[it].toDouble()
                it == (t.lastIndex - 6) -> 3 * values[it].toDouble()
                it == (t.lastIndex - 7) -> 2 * values[it].toDouble()
                else -> values[it].toDouble()
            }
        } / divider).toFloat()
    }


    private fun processLastSample() {
        aFilteredMagnitudes.add(lowPassFilter(aMagnitudes))
        filter.apply(aFilteredMagnitudes.last())
        aNormalizedMagnitudes.add(filter.prediction / NORMALIZATION_COEFFICIENT)
        val timeWindowIndex = timeWindowIndex()
        val aRecentNormalizedMagnitudes = aNormalizedMagnitudes.subList(timeWindowIndex, t.size)
        updateRv(aRecentNormalizedMagnitudes)
        min.add(aRecentNormalizedMagnitudes.min()!!)
        max.add(aRecentNormalizedMagnitudes.max()!!)
        filterSteps()
    }

    private fun filterSteps() {
        val min = min.last()
        val max = max.last()
        val threshold = (min + max) / 2f
        var stepCount = steps.lastOrNull() ?: 0
        sensitivities.add(sensitivity)
        if ((max - min) > sensitivity) {
            val y = aNormalizedMagnitudes.last()
            if (isAbove && (y < threshold)) {
                isAbove = false
                stepCount++
            } else if (!isAbove && (y > threshold)) {
                isAbove = true
            }
        }
        steps.add(stepCount)
    }


    private fun timeWindowIndex(): Int {
        return (t.lastIndex downTo 0).firstOrNull { (t.last() - t[it]) > TIME_WINDOW_NS }
                ?: 0
    }

    private fun updateRv(values: List<Float>) {
        var mean = 0f
        var meanSqr = 0f
        for (value in values) {
            mean += value
            meanSqr += value * value
        }
        var variance = (mean * mean - meanSqr) / values.size
        if (variance > 0.5f) {
            variance -= 0.5f
        }
        if (!variance.isNaN()) {
            filter.R = 20*variance
            sensitivity = 2 * (Math.sqrt(variance.toDouble()) / (NORMALIZATION_COEFFICIENT * NORMALIZATION_COEFFICIENT)).toFloat()
        } else {
            sensitivity = 1f / 30
        }
    }


}