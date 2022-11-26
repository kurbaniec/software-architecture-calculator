package swa.calculator.loader

import org.neo4j.driver.Query
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.util.ResourceUtils
import swa.calculator.db.Neo4jDb
import swa.calculator.domain.Constants.CommonChangesRel
import swa.calculator.domain.Constants.CommonChangesWeight
import swa.calculator.domain.Constants.PerformanceRel
import swa.calculator.domain.Constants.PerformanceWeight
import swa.calculator.domain.Constants.Service
import java.io.File

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2022-11-25
 */
@Component
class PerformanceLoader(
    private val db: Neo4jDb,
    @Value("\${db.insert.size}") private val batchSize: Int,
    @Value("\${performance.csv}") private val csvFilename: String
) {
    companion object {
        private val logger = LoggerFactory.getLogger(PerformanceLoader::class.java)
    }

    fun loadCallerCalleeRelations() {
        val file = csvFile()
        readCsv(file, ::createCallerCalleeRelation)
    }

    private fun createCallerCalleeRelation(commonChanges: List<String>) {
        val queries = mutableListOf<String>()
        for (str in commonChanges) {
            val queryString = buildString {
                val (service1, service2, numberOfCalls) = str.split(';', ignoreCase = false, limit = 3)
                append("MATCH (a:$Service),(b:$Service) ")
                append("WHERE a.name='${service1}' AND b.name='${service2}' " )
                append("CREATE (a)-[r:$PerformanceRel {$PerformanceWeight: $numberOfCalls}]->(b); ")
            }
            queries.add(queryString)
        }
        db.exec {tx ->
            for (queryString in queries) {
                val query = Query(queryString)
                tx.run(query)
            }
        }
    }

    private fun readCsv(file: File, lineFn: (lines: List<String>) -> Unit) {
        val reader = file.inputStream().bufferedReader()
        reader.readLine()
        reader.useLines { r ->
            r.filter { it.isNotBlank() }
                .chunked(batchSize, lineFn)
                .count()
        }
    }

    private fun csvFile(): File {
        return ResourceUtils.getFile("classpath:${csvFilename}")
    }
}