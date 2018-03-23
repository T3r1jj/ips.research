package io.github.t3r1jj.ips.research.model.algorithm

import io.github.t3r1jj.ips.research.model.LimitedQueue
import io.github.t3r1jj.ips.research.model.data.Fingerprint
import io.github.t3r1jj.ips.research.model.data.WifiDataset
import weka.classifiers.Classifier
import weka.core.Instances
import weka.core.SerializationHelper
import weka.core.converters.ConverterUtils
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream


class WifiNavigator(var classifier: Classifier, var ssidRegex: Regex) {

    constructor(modelInputStream: InputStream, trainData: List<WifiDataset>) : this(SerializationHelper.read(modelInputStream) as Classifier,
            Regex("(eduroam)", RegexOption.IGNORE_CASE)) {
        modelInputStream.close()
        this.trainData = trainData
    }

    val fingerprints = LimitedQueue<Fingerprint>(10)
    private val predictionSet = WifiDataset("?", fingerprints)
    private var trainData: List<WifiDataset>? = null

    fun classify(): String {
        if (trainData == null || trainData!!.isEmpty()) {
            return "null"
        } else if (fingerprints.isEmpty()) {
            return "?"
        }
        predictionSet.iterations = fingerprints.size
        val transform = ArffTransform(ssidRegex)
        transform.apply(trainData!!, listOf(predictionSet))
        val trainInstances = getInstances(transform, transform.trainDevices)
        trainInstances.setClassIndex(trainInstances.numAttributes() - 1)
        val predictionInstances = getInstances(transform, predictionSet.device)
        predictionInstances.setClassIndex(predictionInstances.numAttributes() - 1)
        val predictionInstance = predictionInstances[predictionInstances.size - 1]
        val prediction = classifier.classifyInstance(predictionInstance)
        return trainInstances.classAttribute().value(prediction.toInt())
    }

    fun train(trainData: List<WifiDataset>) {
        if (trainData.isEmpty()) {
            throw RuntimeException("No training data")
        }
        val transform = ArffTransform(ssidRegex)
        transform.apply(trainData, listOf())
        val trainInstances = getInstances(transform, transform.trainDevices)
        trainInstances.setClassIndex(trainInstances.numAttributes() - 1)
        classifier.buildClassifier(trainInstances)
        this.trainData = trainData
    }

    private fun getInstances(transform: ArffTransform, device: String): Instances {
        val trainOutputStream = ByteArrayOutputStream()
        transform.writeToFile(trainOutputStream, device)
        val input = ByteArrayInputStream(trainOutputStream.toByteArray())
        val reader = ConverterUtils.DataSource(input)
        return reader.dataSet
    }

    fun addFingerprints(newFingerprints: List<Fingerprint>) {
        fingerprints.addAll(newFingerprints)
    }

}