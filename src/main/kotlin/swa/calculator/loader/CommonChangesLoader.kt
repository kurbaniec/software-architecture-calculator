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
class CommonChangesLoader(
    private val db: Neo4jDb,
    @Value("\${db.insert.size}") private val batchSize: Int,
    @Value("\${common.changes.csv}") private val csvFilename: String
) {
    companion object {
        private val logger = LoggerFactory.getLogger(CommonChangesLoader::class.java)
    }

    fun loadCommonChangesRelation() {
        val file = csvFile()
        readCsv(file, ::createCommonChangesRelation)
    }

    private fun createCommonChangesRelation(commonChanges: List<String>) {
        val queries = mutableListOf<String>()
        for (str in commonChanges) {
            val queryString = buildString {
                val (service1, service2, changes) = str.split(';', ignoreCase = false, limit = 3)
                append("MATCH (a:Service),(b:Service) ")
                append("WHERE a.name='${service1}' AND b.name='${service2}' " )
                append("CREATE (a)-[r:COMMON_CHANGES {weight: $changes}]->(b); ")
            }
            queries.add(queryString)

        }
        db.exec {tx ->
            for (queryString in queries) {
                val query = Query(queryString)
                // val result = tx.run(query)
                // logger.info(result.toString())
                tx.run(query)
            }
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