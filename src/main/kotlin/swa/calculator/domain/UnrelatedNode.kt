package swa.calculator.domain

import org.neo4j.driver.Record

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2022-11-26
 */
data class UnrelatedNode(
    val name: String,
    val clusterId: Int,
)
