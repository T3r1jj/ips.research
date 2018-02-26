package io.github.t3r1jj.ips.collector.model

import android.content.Context
import com.couchbase.lite.Database
import com.couchbase.lite.Manager
import com.couchbase.lite.Query
import com.couchbase.lite.android.AndroidContext
import com.couchbase.lite.auth.PasswordAuthorizer
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.t3r1jj.ips.collector.BuildConfig
import io.github.t3r1jj.ips.collector.model.data.*
import java.net.URL


class Dao(private val context: Context) {
    companion object {
        internal const val DB_ROOT_URL = "couchdbtest12e3.smileupps.com"
        private const val DB_PROTOCOL = "http://"
        private const val DB_NAME = "/ips"
        private const val DB_URL = DB_PROTOCOL + DB_ROOT_URL + DB_NAME
        private const val DB_USERNAME = "admin"
        private const val DB_PASSWORD = "49c0738edb83"
    }

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

    fun replicate() {
        val url = URL(DB_URL)
        val push = getDatabase().createPushReplication(url)
        push.authenticator = PasswordAuthorizer(DB_USERNAME, DB_PASSWORD)
        push.isContinuous = false
        push.start()
    }

}