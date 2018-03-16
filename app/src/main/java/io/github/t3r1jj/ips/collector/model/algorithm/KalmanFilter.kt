package io.github.t3r1jj.ips.collector.model.algorithm

class KalmanFilter {
    private val A = 1f  // state transition
    private val B = 0f  // control factor
    private val C = 1f  // observation factor
    private val u = 0f  // control factor
    private val Q = 1f  // estimated process error covariance

    private var G = 1f  // Kalman gain (moderates prediction)
    private var P = Float.NaN   // covariance prediction (average error)
    private var measurement = Float.NaN // measured signal
    var prediction = Float.NaN // predicted signal without noise
    var R = 10f // estimated measurement error covariance

    fun apply(ech: Float) {
        measurement = ech
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
    }
}