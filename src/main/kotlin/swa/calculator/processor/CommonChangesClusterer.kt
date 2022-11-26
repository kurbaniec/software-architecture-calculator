package swa.calculator.processor

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
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
class CommonChangesClusterer(
    private val clusterer: ServiceClusterer,
    @Value("\${config.process.cluster.threshold}") private val threshold: Int,
    @Value("\${config.process.cluster.new.relationship.weight}") private val newWeight: Int,
    @Value("\${config.process.cluster.max.retries}") private val maxRetries: Int
) {
    companion object {
        val logger = LoggerFactory.getLogger(CommonChangesClusterer::class.java)
    }

    fun clusterServices() {
        logger.info("Clustering for Common Changes")
        val config = ServiceClusterer.ServiceClusterProperties(
            graph = CommonChangesGraph,
            label = Service,
            cluster = CommonChangesClusterId,
            relation = CommonChangesRel,
            weight = CommonChangesWeight,
            newWeight = newWeight,
            threshold = threshold,
            maxRetries = maxRetries
        )
        clusterer.clusterServices(config)
    }
}