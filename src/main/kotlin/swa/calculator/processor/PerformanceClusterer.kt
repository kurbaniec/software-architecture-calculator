package swa.calculator.processor

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import swa.calculator.domain.Constants

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2022-11-26
 */
@Component
class PerformanceClusterer(
    private val clusterer: ServiceClusterer,
    @Value("\${config.process.cluster.threshold}") private val threshold: Int,
    @Value("\${config.process.cluster.new.relationship.weight}") private val newWeight: Int,
    @Value("\${config.process.cluster.max.retries}") private val maxRetries: Int,
    @Value("\${config.process.cluster.min.group.count}") private val minGroupCount: Int
) {
    companion object {
        val logger = LoggerFactory.getLogger(PerformanceClusterer::class.java)
    }

    fun clusterServices() {
        logger.info("Clustering for Performance")
        val config = ServiceClusterer.ServiceClusterProperties(
            graph = Constants.PerformanceGraph,
            label = Constants.Service,
            cluster = Constants.PerformanceClusterId,
            relation = Constants.PerformanceRel,
            weight = Constants.PerformanceWeight,
            newWeight = newWeight,
            threshold = threshold,
            maxRetries = maxRetries,
            minGroupCount = minGroupCount
        )
        clusterer.clusterServices(config)
    }
}