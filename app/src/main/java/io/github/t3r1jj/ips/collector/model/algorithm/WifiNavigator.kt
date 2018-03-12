package io.github.t3r1jj.ips.collector.model.algorithm

import io.github.t3r1jj.ips.collector.model.data.WifiDataset
import weka.classifiers.Classifier
import weka.classifiers.bayes.BayesNet
import weka.classifiers.lazy.IBk
import weka.core.*
import weka.core.converters.ConverterUtils
import java.io.*
import android.R.interpolator.bounce
import java.net.Authenticator


class WifiNavigator(val classifier: Classifier, val ssidRegex: Regex) {

    constructor(inputStream: InputStream) : this(SerializationHelper.read(inputStream) as Classifier,
            Regex("(eduroam)", RegexOption.IGNORE_CASE)) {
        inputStream.close()
    }

    fun classify(wifiDataset: WifiDataset): String {
//        ArffTransform(Regex("(eduroam)")).

        val attributes =
                ArrayList(("28:34:a2:32:1f:80," +
                        "28:34:a2:24:9f:61," +
                        "28:34:a2:32:27:71," +
                        "54:4a:00:c9:39:11," +
                        "28:34:a2:32:1f:81," +
                        "54:4a:00:c9:39:10," +
                        "28:34:a2:32:27:70," +
                        "84:16:f9:c8:84:08," +
                        "00:e1:6d:4e:9c:01," +
                        "28:34:a2:24:b5:c0," +
                        "28:34:a2:32:28:d1," +
                        "00:e1:6d:4e:9c:00," +
                        "28:34:a2:24:b5:c1," +
                        "24:a4:3c:72:ba:21," +
                        "b0:00:b4:fa:98:31," +
                        "28:34:a2:24:9f:60," +
                        "00:e1:6d:4e:fb:d0," +
                        "00:e1:6d:4e:fb:d1," +
                        "28:34:a2:32:28:d0," +
                        "b0:00:b4:fa:98:30," +
                        "b0:00:b4:a5:dd:81," +
                        "00:26:cb:a9:dc:70," +
                        "4c:5e:0c:17:f8:c3," +
                        "4c:5e:0c:17:f7:7b,place")
                        .split(",")
                        .map {
                            if (it == "place") {
                                Attribute(it, "121,125,126,127,128a,128winda,130,132,133,135,136,139,141,221,222,223,223schody127,224,224winda,225,226,227,229schody,229schody133,230,232,233,235,236,237/238,237schody,237schody121".split(","))
                            } else {
                                Attribute(it)
                            }
                        }
                )
        val values =
                "12, 1, 1, 1, 29, 1, 1, 1, 1, 1, 134355, 1, 1, 18, 1, 1, 9, 13, 131576, 1, 1, 1, 1, 1,?".split(",")

        val data = Instances("TestInstances", attributes, 0)
        data.setClassIndex(attributes.lastIndex)
        val instance = DenseInstance(data.numAttributes())
        data.add(instance)
        for (i in 0 until attributes.size - 1) {
            try {
                instance.setValue(attributes[i], values[i].toDouble())
            } catch (nfe: NumberFormatException) {
                instance.setValue(attributes[i], values[i])
            }
        }
        val firstInstance = data.firstInstance()
        val pred = classifier.classifyInstance(firstInstance)
        return firstInstance.classAttribute().value(pred.toInt())
    }


    fun classify(trainSet: Iterable<WifiDataset>, wifiDataset: WifiDataset): String {
        val transform = ArffTransform(ssidRegex)
        transform.apply(trainSet, listOf(wifiDataset))
        val trainInstances = getInstances(transform, transform.trainDevices)
        trainInstances.setClassIndex(trainInstances.numAttributes() - 1)
        val testInstances = getInstances(transform, wifiDataset.device)
        testInstances.setClassIndex(testInstances.numAttributes() - 1)
        classifier.buildClassifier(trainInstances)
        val testInstance = testInstances[testInstances.size - 1]
        val prediction = classifier.classifyInstance(testInstance)
        return trainInstances.classAttribute().value(prediction.toInt())
    }

    private fun getInstances(transform: ArffTransform, device: String): Instances {
        val trainOutputStream = ByteArrayOutputStream()
        transform.writeToFile(trainOutputStream, device)
        println(trainOutputStream.toString())
        val input = ByteArrayInputStream(trainOutputStream.toByteArray())
        val reader = ConverterUtils.DataSource(input)
        return reader.dataSet
    }
}