package swa.calculator.writer

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.util.ResourceUtils
import swa.calculator.domain.Constants

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2022-11-26
 */
@Component
class PerformanceWriter(
    private val writer: ServiceWriter
) {
    companion object {
        val logger = LoggerFactory.getLogger(PerformanceWriter::class.java)
    }

    fun persistArchitecture() {
        logger.info("Persisting for Performance Architecture")
        val config = ServiceWriter.ServiceWriterProperties(
            path = ResourceUtils.getFile("classpath:application.properties").parentFile,
            fileName = "performance",
            label = Constants.Service,
            cluster = Constants.PerformanceClusterId
        )
        writer.writeServices(config)
    }
}