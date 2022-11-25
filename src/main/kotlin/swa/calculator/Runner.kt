package swa.calculator

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component

/**
 *
 *
 * @author Kacper Urbaniec
 * @version 2022-11-25
 */
@Component
class Runner : CommandLineRunner {
    companion object {
        private val logger = LoggerFactory.getLogger(Runner::class.java)
    }

    @Autowired
    private lateinit var db: Neo4jTest

    override fun run(vararg args: String?) {
        logger.info("Hey")

    }
}