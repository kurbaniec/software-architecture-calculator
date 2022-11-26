package swa.calculator.loader

import org.springframework.stereotype.Component
import swa.calculator.db.Neo4jDb
import swa.calculator.domain.Constants.Service

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2022-11-26
 */
@Component
class Clearer(
    private val db: Neo4jDb
) {
    fun clearData() {
        val query = """
            MATCH (s:$Service)
            DETACH DELETE s
        """.trimIndent()
        db.exec { tx -> tx.run(query) }
    }
}