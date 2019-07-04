package dibas

import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.*

@KtorExperimentalAPI
fun main(): Unit = runBlocking {
    val cluster = Cluster.fromFile("src/main/resources/config/clusterSingle.csv")
    println(cluster)

    val dibas = Dibas(cluster)

    launch {
        dibas.run()
    }

    println("Wait a bit ...")
    delay(10_000L)
    println("Start producing tasks ...")

    val task = Task {
        Result("I was computed")
    }

    println("dibas.resolve(task) ...")
    withContext(Dispatchers.Default) {
        val result = dibas.resolve(task)
        println("Main -> $result")
    }

    /*var i = 0
    while (true) {
        i++
        delay(1_000L)

        println("Producing task $i ...")
        val task = Task {
            println("I'm computation $i")
            delay((100..3_000).random().toLong())
            Result("I'm result $i")
        }

        println("dibas.resolve(task $i) ...")
        launch {
            val result = dibas.resolve(task)
            println(result)
        }
    }*/
}