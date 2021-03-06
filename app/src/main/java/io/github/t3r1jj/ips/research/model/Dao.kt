package io.github.t3r1jj.ips.research.model

import android.content.Context
import android.util.Log
import com.couchbase.lite.Database
import com.couchbase.lite.Manager
import com.couchbase.lite.Query
import com.couchbase.lite.android.AndroidContext
import com.couchbase.lite.auth.PasswordAuthorizer
import com.couchbase.lite.replicator.Replication
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.t3r1jj.ips.research.BuildConfig
import io.github.t3r1jj.ips.research.model.data.*
import java.io.File
import java.net.URL


class Dao(private val context: Context) {
    companion object {
        internal const val DB_ROOT_URL = "couchdb-25e1c3.smileupps.com"
        private const val DB_PROTOCOL = "https://"
        private const val DB_NAME = "/ips"
        private const val DB_URL = DB_PROTOCOL + DB_ROOT_URL + DB_NAME
        private const val DB_USERNAME = "admin"
        private const val DB_PASSWORD = "0b9e78e5148e"
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
        database.close()
    }

    fun saveAll(file: File) {
        val objectMapper = ObjectMapper()
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        val data = objectMapper.readValue<Array<Dataset>>(file, object : TypeReference<Array<Dataset>>() {})
        val database = getDatabase()
        for (dataset in data) {
            val document = database.createDocument()
            val properties = objectMapper.convertValue<Map<String, Any>>(dataset,
                    object : TypeReference<HashMap<String, Any>>() {})
            document.putProperties(properties)
        }
        database.close()
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
        val database = getDatabase()
        val query = database.createAllDocumentsQuery()
        query.allDocsMode = Query.AllDocsMode.ALL_DOCS
        val result = query.run()
        while (result.hasNext()) {
            val row = result.next()
            try {
                when {
                    DatasetType.WIFI.toString() == row.document.getProperty("type") -> results[row.sourceDocumentId] = objectMapper.convertValue(row.document.properties, object : TypeReference<WifiDataset>() {})
                    DatasetType.INERTIAL.toString() == row.document.getProperty("type") -> results[row.sourceDocumentId] = objectMapper.convertValue(row.document.properties, object : TypeReference<InertialDataset>() {})
                    DatasetType.MAGNETIC.toString() == row.document.getProperty("type") -> results[row.sourceDocumentId] = objectMapper.convertValue(row.document.properties, object : TypeReference<MagneticDataset>() {})
                }
            } catch (ex: Exception) {
                Log.w("Dao", "Record incompatible with the current model, ignoring...")
                delete(row.sourceDocumentId)
            }
        }
        database.close()
        return results
    }

    fun pull(): Replication {
        val url = URL(DB_URL)
        val pull = getDatabase().createPullReplication(url)
        pull.authenticator = PasswordAuthorizer(DB_USERNAME, DB_PASSWORD)
        pull.isContinuous = false
        pull.start()
        return pull
    }

    fun delete(id: String) {
        val database = getDatabase()
        database.getDocument(id).delete()
        database.close()
    }

    fun push(): Replication {
        val url = URL(DB_URL)
        val push = getDatabase().createPushReplication(url)
        push.authenticator = PasswordAuthorizer(DB_USERNAME, DB_PASSWORD)
        push.isContinuous = false
        push.start()
        return push
    }

}