package io.github.t3r1jj.ips.collector.model

import android.content.Context
import com.couchbase.lite.Database
import com.couchbase.lite.Manager
import com.couchbase.lite.Query
import com.couchbase.lite.android.AndroidContext
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.t3r1jj.ips.collector.BuildConfig
import io.github.t3r1jj.ips.collector.model.data.*


class Dao(private val context: Context) {
    private val objectMapper = ObjectMapper()

    init {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }

    fun save(data: Dataset) {
        val database = getDatabase()
        val document = database.createDocument()
        val properties = objectMapper.convertValue<Map<String, Any>>(data,
                object : TypeReference<HashMap<String, Any>>() {})
        document.putProperties(properties)
    }

    private fun getDatabase(): Database {
        val manager = Manager(AndroidContext(context.applicationContext), Manager.DEFAULT_OPTIONS)
        return manager.getDatabase(BuildConfig.APPLICATION_ID + ".db")
    }

    fun clear() {
        getDatabase().delete()
    }

    fun findAll(): Map<String, Dataset> {
        val results = mutableMapOf<String, Dataset>()
        val query = getDatabase().createAllDocumentsQuery()
        query.allDocsMode = Query.AllDocsMode.ALL_DOCS
        val result = query.run()
        while (result.hasNext()) {
            val row = result.next()
            when {
                DatasetType.WIFI.toString() == row.document.getProperty("type") -> results[row.sourceDocumentId] = objectMapper.convertValue(row.document.properties, object : TypeReference<WifiDataset>() {})
                DatasetType.INERTIAL.toString() == row.document.getProperty("type") -> results[row.sourceDocumentId] = objectMapper.convertValue(row.document.properties, object : TypeReference<InertialDataset>() {})
                DatasetType.MAGNETIC.toString() == row.document.getProperty("type") -> results[row.sourceDocumentId] = objectMapper.convertValue(row.document.properties, object : TypeReference<MagneticDataset>() {})
            }
        }
        return results
    }

    fun delete(id: String) {
        getDatabase().getDocument(id).delete()
    }

}