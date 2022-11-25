package swa.calculator.loader

import org.neo4j.driver.Query
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.util.ResourceUtils
import swa.calculator.db.Neo4jDb
import java.io.File

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2022-11-25
 */
@Component
class ServiceNodeLoader(
    private val db: Neo4jDb,
    @Value("\${db.insert.size}") private val batchSize: Int,
    @Value("\${services.csv}") private val csvFilename: String
) {
    companion object {
        private val logger = LoggerFactory.getLogger(ServiceNodeLoader::class.java)
    }

    fun loadServiceNodes() {
        val file = csvFile()
        readCsv(file, ::createServiceNodes)
    }

    private fun createServiceNodes(services: List<String>) {
        val queryString = buildString {
            append("CREATE ")
            val iterator = services.iterator()
            while(iterator.hasNext()) {
                val service = iterator.next()
                append("(n${service}:Service {name: '${service}'})")
                if (iterator.hasNext()) append(",")
                else append(";")
            }
        }
        db.exec {tx ->
            val query = Query(queryString)
            val result = tx.run(query)
            logger.info(result.toString())
        }
    }

    private fun readCsv(file: File, lineFn: (lines: List<String>) -> Unit) {
        val reader = file.inputStream().bufferedReader()
        reader.readLine()
        // reader.useLines().chunked(batchSize, lineFn).toList()
        reader.useLines { r ->
            r.filter { it.isNotBlank() }
                .chunked(batchSize, lineFn)
                .toList()
        }
    }

    private fun csvFile(): File {
        return ResourceUtils.getFile("classpath:${csvFilename}")
    }
}