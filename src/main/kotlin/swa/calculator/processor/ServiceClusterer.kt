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
    @Value("\${config.process.cluster.threshold}") private val threshold: Int,
    @Value("\${config.process.cluster.new.relationship.weight}") private val newWeight: Int,
    @Value("\${config.process.cluster.max.retries}") private val maxRetries: Int
) {
    companion object {
        private val logger = LoggerFactory.getLogger(ServiceClusterer::class.java)
    }

    fun clusterServices() {
        var count: Int
        var retries = 0
        do {
            dropAndCreateGraph()
            count = performWriteLeiden()
            if (count > threshold) {
                logger.warn("Got [$count] clusters, exceeding threshold [$threshold]")
                logger.warn("Connecting smallest clusters together")
                connectSmallestClusters()
                retries++
            }
        } while (count > threshold && retries < maxRetries)

        if (retries < maxRetries) {
            logger.info("Created graph with [$count] clusters")
        } else {
            logger.error("Could not cluster under threshold")
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
        val nodes = smallestClusterNodes(count)
        createNewRelationships(nodes)
    }

    private fun createNewRelationships(nodes: List<UnrelatedNode>) {
        val nodesPerCluster = nodes.filterOneNodePerCluster()
        val nodePairs = nodesPerCluster.createNodePairs()
        for ((a, b) in nodePairs) {
            // println("new: $a = $b")
            val query = """
                MATCH (a:Service),(b:Service) 
                WHERE a.name='${a.name}' AND b.name='${b.name}' 
                CREATE 
                    (a)-[r1:$CommonChangesRel {$CommonChangesWeight: $newWeight}]->(b),
                    (b)-[r2:$CommonChangesRel {$CommonChangesWeight: $newWeight}]->(a)
            """.trimIndent()
            db.exec { tx ->
                tx.run(query)
            }
        }
    }

    private fun smallestClusterNodes(count: MinCount): List<UnrelatedNode> {
        val query = """
            CALL {
            	MATCH (s:Service)
                WITH s.$CommonChangesClusterId as id, count(s) as count
                where count = $count
                return id
            }
            WITH id
            MATCH (s:Service)
            WHERE s.$CommonChangesClusterId in id
            RETURN s.name as name, s.$CommonChangesClusterId as clusterId
        """.trimIndent()
        val nodes = mutableListOf<UnrelatedNode>()
        db.exec { tx ->
            val result = tx.run(query)
            while (result.hasNext()) {
                val record = result.next()
                val name = record["name"].toString().replace("\"", "")
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
            WITH s.$CommonChangesClusterId as id, count(s) as count 
            RETURN min(count) as min
        """.trimIndent()
        return db.exec { tx ->
            val result = tx.run(query)
            val record = result.next()
            record["min"].asInt()
        }
    }

    private fun List<UnrelatedNode>.filterOneNodePerCluster(): List<UnrelatedNode> {
        val inspectedClusterIds = mutableListOf<Int>()
        return this.filter { node ->
            if (!inspectedClusterIds.contains(node.clusterId)) {
                inspectedClusterIds.add(node.clusterId)
                return@filter true
            }
            false
        }

    }

    private fun List<UnrelatedNode>.createNodePairs(): List<Pair<UnrelatedNode, UnrelatedNode>> {
        return this.chunked(2)
            .map { input ->
                if (input.count() == 1) {
                    Pair(input.first(), this.random())
                } else {
                    Pair(input[0], input[1])
                }
            }
    }

}

typealias CommunityCount = Int
typealias MinCount = Int