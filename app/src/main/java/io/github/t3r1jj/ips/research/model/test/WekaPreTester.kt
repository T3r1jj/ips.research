package io.github.t3r1jj.ips.research.model.test

import io.github.t3r1jj.ips.research.model.algorithm.ArffTransform

class WekaPreTester(val arff: ArffTransform) {
    fun knnTest(): List<String> {
        return arff.testDevices.map {
            var hits = 0
            var total = 0
            for (testDeviceObject in arff.objects[it]!!) {
                val testVals = testDeviceObject.split(",")
                var minDist = Double.MAX_VALUE
                var klass = ""
                for (trainDeviceObject in arff.objects[arff.trainDevices]!!) {
                    val trainVals = trainDeviceObject.split(",")
                    var dist = 0.toDouble() + (0 until testVals.lastIndex)
                            .map { trainVals[it].toDouble() - testVals[it].toDouble() }
                            .sumByDouble { it * it }
                    dist = Math.sqrt(dist)
                    if (dist < minDist) {
                        minDist = dist
                        klass = trainVals.last()
                    }
                }
                if (klass == testVals.last()) {
                    hits++
                }
                total++
            }
            it + " = " + Math.round(10000 * hits.toDouble() / total).div(100.toDouble())
        }
    }

    fun customTest(): List<String> {
        return arff.testDevices.map {
            var hits = 0
            var total = 0
            for (testDeviceObject in arff.objects[it]!!) {
                val testVals = testDeviceObject.split(",")
                var minDist = Double.MAX_VALUE
                var klass = ""
                for (trainDeviceObject in arff.objects[arff.trainDevices]!!) {
                    val trainDeviceVals = trainDeviceObject.split(",")
                    var dist = 0.toDouble() + (0 until testVals.lastIndex)
                            .filter {
                                if (arff.attributeDataType == ArffTransform.AttributeDataType.POWER) {
                                    Math.abs(ArffTransform.pikoWattToDBm(trainDeviceVals[it].toDouble()) - ArffTransform.pikoWattToDBm(testVals[it].toDouble())) > 5
                                } else {
                                    Math.abs(trainDeviceVals[it].toDouble() - testVals[it].toDouble()) > 5
                                }
                            }
                            .map { trainDeviceVals[it].toDouble() - testVals[it].toDouble() }
                            .sumByDouble { it * it }
                    dist = Math.sqrt(dist)
                    if (dist < minDist) {
                        minDist = dist
                        klass = trainDeviceVals.last()
                    }
                }
                if (klass == testVals.last()) {
                    hits++
                }
                total++
            }
            it + " = " + Math.round(10000 * hits.toDouble() / total).div(100.toDouble())
        }
    }
}