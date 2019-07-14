package dibas

import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.*

@KtorExperimentalAPI
fun main() {
    delayDemo()
}

@KtorExperimentalAPI
fun delayDemo() = runBlocking {
    val cluster = Cluster.fromFile("src/main/resources/config/clusterSingle.csv")
    println(cluster)

    val logger = ThreadAwareLogger()
    val dibas = Dibas(cluster, logger)

    launch {
        dibas.run()
    }

    delay(5_000L)
    println("Start producing tasks ...")

    repeat(10) {

        logger.log("\n\nProducing task $it ...")
        val task = Task {
            delay(20_000L / (it + 1))
            Result("$it")
        }

        launch {
            val result = dibas.resolve(task)
            logger.log("main --> $result")
        }
    }
}