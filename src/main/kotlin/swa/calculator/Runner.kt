package swa.calculator

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component
import swa.calculator.loader.Clearer
import swa.calculator.loader.CommonChangesLoader
import swa.calculator.loader.PerformanceLoader
import swa.calculator.loader.ServiceNodeLoader
import swa.calculator.processor.CommonChangesClusterer
import swa.calculator.processor.PerformanceClusterer

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2022-11-25
 */
@Component
class Runner(
    @Value("\${config.load.data}") private val loadData: Boolean,
    @Value("\${config.process.data}") private val processData: Boolean,
) : CommandLineRunner {
    companion object {
        private val logger = LoggerFactory.getLogger(Runner::class.java)
    }

    @Autowired
    private lateinit var clearer: Clearer
    @Autowired
    private lateinit var nodeLoader: ServiceNodeLoader
    @Autowired
    private lateinit var changesLoader: CommonChangesLoader
    @Autowired
    private lateinit var performanceLoader: PerformanceLoader
    @Autowired
    private lateinit var changesClusterer: CommonChangesClusterer
    @Autowired
    private lateinit var performanceClusterer: PerformanceClusterer

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
        }
    }
}