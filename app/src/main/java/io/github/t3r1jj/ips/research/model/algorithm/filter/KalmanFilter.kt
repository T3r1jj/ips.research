package io.github.t3r1jj.ips.research.model.algorithm.filter

class KalmanFilter(val R: Float) : SignalFilter {
    constructor() : this(4f) // estimated measurement error covariance

    private val A = 1f  // state transition factor
    private val H = 1f  // observation factor
    private val Q = 1f  // estimated process error covariance

    private var K = 1f  // Kalman gain (moderates prediction)
    private var P = Float.NaN   // covariance prediction (average error)
    private var z = Float.NaN // measured signal
    private var x = Float.NaN // predicted signal without noise

    override fun apply(value: Float): Float {
        z = value
        if (x.isNaN()) {
            x = 1 / H * z
            P = 1 / H * R * 1 / H
        } else {
            x *= A
            P = A * P * A + Q
            K = P * H * 1 / (H * P * H + R)
            x += K * (z - H * x)
            P -= K * H * P
        }
        return x
    }

}