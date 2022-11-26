package swa.calculator.processor

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import swa.calculator.db.Neo4jDb
import swa.calculator.domain.Node
import java.util.*
import kotlin.math.ceil

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2022-11-26
 */
@Component
class ServiceClusterer(
    private val db: Neo4jDb,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(ServiceClusterer::class.java)
    }

    data class ServiceClusterProperties(
        val graph: String,
        val label: String,
        val cluster: String,
        val relation: String,
        val weight: String,
        val newWeight: Int,
        val threshold: Int,
        val maxRetries: Int,
        val minGroupCount: Int
    )
    private lateinit var cfg: ServiceClusterProperties

    fun clusterServices(config: ServiceClusterProperties) {
        cfg = config
        var count: Int
        var retries = 0
        do {
            dropAndCreateGraph()
            count = performWriteLeiden()
            if (count > cfg.threshold) {
                logger.warn("Got [$count] clusters, exceeding threshold [${cfg.threshold}]")
                logger.warn("Connecting smallest clusters together")
                connectSmallestClusters()
                retries++
            }
        } while (count > cfg.threshold && retries < cfg.maxRetries)

        if (retries < cfg.maxRetries) {
            logger.info("Created graph with [$count] clusters")
        } else {
            logger.error("Could not cluster under threshold")
        }
    }

    private fun dropAndCreateGraph() {
        val dropQuery = "CALL gds.graph.drop('${cfg.graph}', false) YIELD graphName"
        db.exec { tx -> tx.run(dropQuery) }
        val createQuery = """
            CALL gds.graph.project(
                '${cfg.graph}',
                '${cfg.label}',
                {
                    ${cfg.relation}: {
                        orientation: 'UNDIRECTED'
                    }
                },
                {
                    relationshipProperties: '${cfg.weight}'
                }
            )
        """.trimIndent()
        db.exec { tx -> tx.run(createQuery) }
    }

    private fun performWriteLeiden(): CommunityCount {
        val query = """
            CALL gds.alpha.leiden.write('${cfg.graph}', { 
            	writeProperty: '${cfg.cluster}',
            	relationshipWeightProperty: '${cfg.weight}'
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

    private fun createNewRelationships(nodes: List<Node>) {
        val nodesPerCluster = nodes.filterOneNodePerCluster()
        val nodePairs = nodesPerCluster.createNodePairs()
        for ((a, b) in nodePairs) {
            // println("new: $a = $b")
            val query = """
                MATCH (a:${cfg.label}),(b:${cfg.label}) 
                WHERE a.name='${a.name}' AND b.name='${b.name}' 
                CREATE 
                    (a)-[r1:${cfg.relation} {${cfg.weight}: ${cfg.newWeight}}]->(b),
                    (b)-[r2:${cfg.relation} {${cfg.weight}: ${cfg.newWeight}}]->(a)
            """.trimIndent()
            db.exec { tx ->
                tx.run(query)
            }
        }
    }

    private fun smallestClusterNodes(count: MinCount): List<Node> {
        val query = """
            CALL {
            	MATCH (s:Service)
                WITH s.${cfg.cluster} as id, count(s) as count
                where count <= $count
                return id
            }
            WITH id
            MATCH (s:Service)
            WHERE s.${cfg.cluster} in id
            RETURN s.name as name, s.${cfg.cluster} as clusterId
        """.trimIndent()
        val nodes = mutableListOf<Node>()
        db.exec { tx ->
            val result = tx.run(query)
            while (result.hasNext()) {
                val record = result.next()
                val name = record["name"].toString().replace("\"", "")
                val id = record["clusterId"].asInt()
                nodes.add(Node(name, id))
            }
        }
        // logger.info(nodes.count().toString())
        return nodes
    }

    private fun smallestClusterCount(): MinCount {
        val minCountQuery = """
            MATCH (s:${cfg.label})
            WITH s.${cfg.cluster} as id, count(s) as count 
            RETURN min(count) as min
        """.trimIndent()
        var minCount = db.exec { tx ->
            val result = tx.run(minCountQuery)
            val record = result.next()
            record["min"].asDouble()
        }
        // Check if more than configured nodes were found
        // If there are only two nodes and if there in the same group
        // the algorithm would stuck in place as the smallestClusterCount method
        // would always return the same minCount
        do {
            val nodeCountQuery = """
            CALL {
                MATCH (s:Service)
                WITH s.${cfg.cluster} as id, count(s) as count
                where count <= ${"%.2f".format(Locale.ENGLISH, minCount)}
                return id
            }
            WITH id
            MATCH (s:Service)
            WHERE s.${cfg.cluster} in id
            RETURN count(s) as nodeCount
        """.trimIndent()
            val nodeCount = db.exec { tx ->
                val result = tx.run(nodeCountQuery)
                val record = result.next()
                record["nodeCount"].asInt()
            }
            if (nodeCount < cfg.minGroupCount) {
                minCount *= 1.25
            }
        } while (nodeCount < cfg.minGroupCount)
        return ceil(minCount).toInt()
    }

    private fun List<Node>.filterOneNodePerCluster(): List<Node> {
        val inspectedClusterIds = mutableListOf<Int>()
        return this.filter { node ->
            if (!inspectedClusterIds.contains(node.clusterId)) {
                inspectedClusterIds.add(node.clusterId)
                return@filter true
            }
            false
        }

    }

    private fun List<Node>.createNodePairs(): List<Pair<Node, Node>> {
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