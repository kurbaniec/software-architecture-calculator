package swa.calculator.db

import org.neo4j.driver.AuthTokens
import org.neo4j.driver.Driver
import org.neo4j.driver.GraphDatabase
import org.neo4j.driver.TransactionContext
import org.springframework.stereotype.Component

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2022-11-25
 */
@Component
class Neo4jDb : AutoCloseable {
    private val driver: Driver
        = GraphDatabase.driver("bolt://localhost:7687", AuthTokens.basic("neo4j", "test"))

    fun <T> exec(cmd: (tx: TransactionContext) -> T): T {
        return driver.session().use { session ->
            session.executeWrite { tx ->
                // val query = Query(
                //                    "CREATE (a:Greeting) SET a.message = \$message RETURN a.message + ', from node ' + id(a)",
                //                    parameters("message", cmd)
                //                )
                //                val result: Result = tx.run(query)
                //                result.single().get(0).asString()
                cmd(tx)
            }
        }
    }

    override fun close() {
        driver.close()
    }
}