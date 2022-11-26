package swa.calculator

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component
import swa.calculator.loader.Clearer
import swa.calculator.loader.CommonChangesLoader
import swa.calculator.loader.PerformanceLoader
import swa.calculator.loader.ServiceNodeLoader
import swa.calculator.processor.CommonChangesClusterer
import swa.calculator.processor.PerformanceClusterer
import swa.calculator.writer.CommonChangesWriter
import swa.calculator.writer.PerformanceWriter

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2022-11-25
 */
@Component
class Runner(
    private val clearer: Clearer,
    private val nodeLoader: ServiceNodeLoader,
    private val changesLoader: CommonChangesLoader,
    private val performanceLoader: PerformanceLoader,
    private val changesClusterer: CommonChangesClusterer,
    private val performanceClusterer: PerformanceClusterer,
    private val changesWriter: CommonChangesWriter,
    private val performanceWriter: PerformanceWriter,
    @Value("\${config.load.data}") private val loadData: Boolean,
    @Value("\${config.process.data}") private val processData: Boolean,
) : CommandLineRunner {
    companion object {
        private val logger = LoggerFactory.getLogger(Runner::class.java)
    }

    override fun run(vararg args: String?) {
        if (loadData) {
            logger.info("Clearing Data ...")
            clearer.clearData()
            logger.info("Clearing Finished")
            logger.info("Loading Data ...")
            nodeLoader.loadServiceNodes()
            changesLoader.loadCommonChangesRelations()
            performanceLoader.loadCallerCalleeRelations()
            logger.info("Loading Finished")
        }
        if (processData) {
            logger.info("Processing Data ...")
            changesClusterer.clusterServices()
            performanceClusterer.clusterServices()
            logger.info("Processing Finished")
            logger.info("Persisting System Architecture ...")
            changesWriter.persistArchitecture()
            performanceWriter.persistArchitecture()
            logger.info("Persisting Finished")
        }
    }
}