package io.github.t3r1jj.ips.research.model.algorithm

import io.github.t3r1jj.ips.research.model.algorithm.ArffTransform.Companion.dBmToPikoWatt
import io.github.t3r1jj.ips.research.model.algorithm.ArffTransform.Companion.pikoWattToDBm
import org.junit.Assert.*
import org.junit.Test

class ArffTransformTest {

    @Test
    fun dbmToWatt() {
        val dBm = -65
        assertEquals(dBm, pikoWattToDBm(dBmToPikoWatt(dBm.toDouble()).toDouble()))
    }
    @Test
    fun dbmToWatt29() {
        val dBm = -29
        assertEquals(dBm, pikoWattToDBm(dBmToPikoWatt(dBm.toDouble()).toDouble()))
    }
    @Test
    fun dbmToWatt99() {
        val dBm = -100
        assertEquals(dBm, pikoWattToDBm(dBmToPikoWatt(dBm.toDouble()).toDouble()))
    }
}