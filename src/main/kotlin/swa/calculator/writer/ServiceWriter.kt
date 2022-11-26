package swa.calculator.writer

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import swa.calculator.db.Neo4jDb
import swa.calculator.domain.Node
import java.io.File
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2022-11-26
 */
@Component
class ServiceWriter(
    private val db: Neo4jDb
) {
    companion object {
        val logger = LoggerFactory.getLogger(ServiceWriter::class.java)
    }

    data class ServiceWriterProperties(
        val path: File,
        val fileName: String,
        val label: String,
        val cluster: String,
    )

    private lateinit var cfg: ServiceWriterProperties

    fun writeServices(config: ServiceWriterProperties) {
        cfg = config
        val nodes = clusterNodes()
        writeNodes(nodes)
    }

    private fun writeNodes(nodes: List<Node>) {
        val groupedNodes = nodes.groupBy { it.clusterId }
        val dir = File(cfg.path, "calculator.${currentDate()}")
        dir.mkdirs()
        val file = File(dir, "${cfg.fileName}.${currentTime()}.cluster.json")
        file.printWriter().use { out ->
            writeSystem(out, groupedNodes)
        }
    }

    private fun writeSystem(out: PrintWriter, groupedNodes: Map<Int, List<Node>>) {
        var serviceId = 0
        val iterator = groupedNodes.keys.iterator()
        out.println("{")
        while (iterator.hasNext()) {
            serviceId++
            val nodes = groupedNodes.getValue(iterator.next())
            writeSubSystem(out, "$serviceId", nodes)
            if (iterator.hasNext()) out.println(",")
            else out.println()
        }
        out.println("}")
    }

    private fun writeSubSystem(out: PrintWriter, identifier: String, nodes: List<Node>) {
        out.println("\t\"SERVICE_$identifier\": [")
        val iterator = nodes.iterator()
        while (iterator.hasNext()) {
            val name = iterator.next().name
            out.print("\t\t\"${name}\"")
            if (iterator.hasNext()) out.println(",")
            else out.println()
        }
        out.print("\t]")
    }

    private fun clusterNodes(): List<Node> {
        val query = """
            MATCH (s:${cfg.label})
            WITH s.${cfg.cluster} as clusterId, s.name as name
            RETURN clusterId, name
            ORDER BY clusterId asc
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
        return nodes
    }

    private fun currentDate(): String {
        val simpleDate = SimpleDateFormat("dd-MM-yyyy")
        return simpleDate.format(Date())
    }

    private fun currentTime(): String {
        val simpleDate = SimpleDateFormat("HH-mm-ss")
        return simpleDate.format(Date())
    }
}