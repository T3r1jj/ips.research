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

    @Test
    fun median() {
        val arff = ArffTransform(Regex(""))
        val testData = mutableListOf(
                listOf(1, 3),
                listOf(2, 2),
                listOf(3, 1)
        )
        assertEquals(mutableListOf(listOf(2.0f, 2.0f)), arff.median(testData,3))
    }

    @Test
    fun medianMultiple() {
        val arff = ArffTransform(Regex(""))
        val testData = mutableListOf(
                listOf(1, 3),
                listOf(2, 2),
                listOf(3, 1),
                listOf(3, 1)
        )
        assertEquals(mutableListOf(listOf(2.0f, 2.0f), listOf(3.0f, 1.0f)), arff.median(testData,3))
    }

    @Test
    fun median2() {
        val arff = ArffTransform(Regex(""))
        val testData = mutableListOf(
                listOf(1, 4),
                listOf(2, 3),
                listOf(3, 2),
                listOf(4, 1)
        )
        assertEquals(mutableListOf(listOf(2.5f, 2.5f)), arff.median(testData,4))
    }

    @Test
    fun median2Multiple() {
        val arff = ArffTransform(Regex(""))
        val testData = mutableListOf(
                listOf(1, 4),
                listOf(2, 3),
                listOf(3, 2),
                listOf(4, 1),
                listOf(4, 1)
        )
        assertEquals(mutableListOf(listOf(2.5f, 2.5f), listOf(4.0f, 1.0f)), arff.median(testData,4))
    }

    @Test
    fun avg() {
        val arff = ArffTransform(Regex(""))
        val testData = mutableListOf(
                listOf(1, 2),
                listOf(2, 3),
                listOf(3, 4),
                listOf(4, 5)
        )
        assertEquals(listOf(2.5f, 3.5f), arff.avg(testData))
    }
}