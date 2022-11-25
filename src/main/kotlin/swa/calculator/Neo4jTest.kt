package swa.calculator

import org.neo4j.driver.AuthTokens
import org.neo4j.driver.Driver
import org.neo4j.driver.GraphDatabase
import org.neo4j.driver.Query
import org.neo4j.driver.Result
import org.neo4j.driver.Values.parameters
import org.springframework.stereotype.Component

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2022-11-25
 */
@Component
class Neo4jTest : AutoCloseable {
    private val driver: Driver
        = GraphDatabase.driver("bolt://localhost:7687", AuthTokens.basic("neo4j", "test"))


    fun exec(cmd: String) {
        driver.session().use { session ->
            val greeting: Unit = session.executeWrite { tx ->
                val query = Query(
                    "CREATE (a:Greeting) SET a.message = \$message RETURN a.message + ', from node ' + id(a)",
                    parameters("message", cmd)
                )
                val result: Result = tx.run(query)
                result.single().get(0).asString()
            }
            println(greeting)
        }
    }

    override fun close() {
        driver.close()
    }
}