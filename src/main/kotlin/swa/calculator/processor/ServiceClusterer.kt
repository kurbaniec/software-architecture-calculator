package swa.calculator.processor

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import swa.calculator.db.Neo4jDb
import swa.calculator.domain.Constants.CommonChangesClusterId
import swa.calculator.domain.Constants.CommonChangesGraph
import swa.calculator.domain.Constants.CommonChangesRel
import swa.calculator.domain.Constants.CommonChangesWeight
import swa.calculator.domain.Constants.Service

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2022-11-26
 */
@Component
class ServiceClusterer(
    private val db: Neo4jDb,
    @Value("\${db.insert.size}") private val batchSize: Int,
    @Value("\${config.process.cluster.threshold}") private val threshold: Int
) {
    companion object {
        private val logger = LoggerFactory.getLogger(ServiceClusterer::class.java)
    }
    
    fun clusterServices() {
        dropAndCreateGraph()
        val count = performWriteLeiden()
        if (count > threshold) {
            logger.warn("Got [$count] clusters, exceeding threshold [$threshold]")
            logger.warn("Connecting smallest clusters together")
        }

    }
    
    private fun dropAndCreateGraph() {
        val dropQuery = "CALL gds.graph.drop('$CommonChangesGraph', false) YIELD graphName"
        db.exec { tx -> tx.run(dropQuery) }
        val createQuery = """
            CALL gds.graph.project(
                '$CommonChangesGraph',
                '$Service',
                {
                    $CommonChangesRel: {
                        orientation: 'UNDIRECTED'
                    }
                },
                {
                    relationshipProperties: '$CommonChangesWeight'
                }
            )
        """.trimIndent()
        db.exec { tx -> tx.run(createQuery) }
    }
    
    private fun performWriteLeiden(): CommunityCount {
        val query = """
            CALL gds.alpha.leiden.write('$CommonChangesGraph', { 
            	writeProperty: '$CommonChangesClusterId',
            	relationshipWeightProperty: '$CommonChangesWeight'
            })
            YIELD communityCount
            RETURN communityCount
        """.trimIndent()
        return db.exec { tx ->
            val result = tx.run(query)
            val record = result.next()
            record["communityCount"].asInt()
        }
    }

    private fun connectSmallestClusters() {

    }

    private fun smallestClusterCount(): Int {
        return 0
    }
    
}

typealias CommunityCount = Int
typealias MinCount = Int