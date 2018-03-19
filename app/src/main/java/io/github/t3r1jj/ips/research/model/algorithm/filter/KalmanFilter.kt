package io.github.t3r1jj.ips.research.model.algorithm.filter

class KalmanFilter : SignalFilter {
    private val A = 1f  // state transition
    private val B = 0f  // control factor
    private val C = 1f  // observation factor
    private val u = 0f  // control factor
    private val Q = 1f  // estimated process error covariance

    private var G = 1f  // Kalman gain (moderates prediction)
    private var P = Float.NaN   // covariance prediction (average error)
    private var measurement = Float.NaN // measured signal
    private var prediction = Float.NaN // predicted signal without noise
    private var R = 10f // estimated measurement error covariance

    override fun apply(value: Float): Float {
        measurement = value
        if (prediction.isNaN()) {
            prediction = 1 / C * measurement
            P = 1 / C * R * 1 / C
        } else {
            prediction = A * prediction + B * u
            P = A * P * A + Q
            G = P * C * 1 / (C * P * C + R)
            prediction += G * (measurement - C * prediction)
            P -= G * C * P
        }
        return prediction
    }

    override fun onVarianceUpdate(variance: Float) {
        R = variance
    }
}