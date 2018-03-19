package io.github.t3r1jj.ips.research.model.algorithm.filter

class NoFilter : SignalFilter {
    override fun apply(value: Float): Float {
        return value
    }

    override fun onVarianceUpdate(variance: Float) {
    }

}