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
import swa.calculator.domain.UnrelatedNode

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
    @Value("\${config.process.cluster.threshold}") private val threshold: Int,
    @Value("\${config.process.cluster.new.relationship.weight}") private val newWeight: Int
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
            connectSmallestClusters()
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
        val count = smallestClusterCount()
        smallestClusterNodes(count)
    }

    private fun createNewRelationships(nodes: List<UnrelatedNode>) {

    }

    private fun smallestClusterNodes(count: MinCount): List<UnrelatedNode> {
        val query = """
            CALL {
            	MATCH (s:Service)
                WITH s.$CommonChangesClusterId as id, count(s) as count
                where count = $count
                return id
            }
            with id
            match (s:Service)
            where s.$CommonChangesClusterId in id
            return s.name as name, s.$CommonChangesClusterId as clusterId
        """.trimIndent()
        val nodes = mutableListOf<UnrelatedNode>()
        db.exec { tx ->
            val result = tx.run(query)
            while (result.hasNext()) {
                val record = result.next()
                val name = record["name"].toString()
                val id = record["clusterId"].asInt()
                nodes.add(UnrelatedNode(name, id))
            }
        }
        logger.info(nodes.count().toString())
        return nodes
    }

    private fun smallestClusterCount(): MinCount {
        val query = """
            MATCH (s:$Service)
            with s.$CommonChangesClusterId as id, count(s) as count 
            return min(count) as min
        """.trimIndent()
        return db.exec { tx ->
            val result = tx.run(query)
            val record = result.next()
            record["min"].asInt()
        }
    }
    
}

typealias CommunityCount = Int
typealias MinCount = Int