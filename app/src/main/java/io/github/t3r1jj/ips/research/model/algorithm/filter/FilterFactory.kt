package io.github.t3r1jj.ips.research.model.algorithm.filter

class FilterFactory(var filterType: FilterType) {
    var parameter = 3 as Number

    fun createFilter(): SignalFilter {
        return when (filterType) {
            FilterType.NO_FILTER -> NoFilter()
            FilterType.MOVING_AVERAGE_FILTER -> MovingAverageFilter(parameter.toInt())
            FilterType.KALMAN_FILTER -> KalmanFilter(parameter.toFloat())
        }
    }

    enum class FilterType {
        NO_FILTER, MOVING_AVERAGE_FILTER, KALMAN_FILTER;
    }
}