package io.github.t3r1jj.ips.research.model.algorithm.filter

interface SignalFilter {
    fun apply(value: Float): Float
    fun onVarianceUpdate(variance: Float)
}