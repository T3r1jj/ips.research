package io.github.t3r1jj.ips.research.model.algorithm.filter

class FilterFactory(var filterType: FilterType) {
    var averagingWindowLength = 3

    fun createFilter(): SignalFilter {
        return when (filterType) {
            FilterType.NO_FILTER -> NoFilter()
            FilterType.MOVING_AVERAGE_FILTER -> MovingAverageFilter(averagingWindowLength)
            FilterType.KALMAN_FILTER -> KalmanFilter()
        }
    }

    enum class FilterType {
        NO_FILTER, MOVING_AVERAGE_FILTER, KALMAN_FILTER;
    }
}