package swa.calculator

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component
import swa.calculator.db.Neo4jDb
import swa.calculator.loader.CommonChangesLoader
import swa.calculator.loader.ServiceNodeLoader
import swa.calculator.processor.ServiceClusterer

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
    private lateinit var nodeLoader: ServiceNodeLoader
    @Autowired
    private lateinit var commonChangesLoader: CommonChangesLoader
    @Autowired
    private lateinit var clusterer: ServiceClusterer

    override fun run(vararg args: String?) {
        if (loadData) {
            logger.info("Loading Data ...")
            nodeLoader.loadServiceNodes()
            commonChangesLoader.loadCommonChangesRelation()
            logger.info("Loading Finished")
        }
        if (processData) {
            logger.info("Processing Data ...")
            clusterer.clusterServices()
            logger.info("Processing Finished")
        }
    }
}