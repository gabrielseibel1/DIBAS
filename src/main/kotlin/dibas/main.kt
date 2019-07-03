package dibas

import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@KtorExperimentalAPI
fun main(): Unit = runBlocking {
    val cluster = clusterFromFile("src/main/resources/config/clusterDense.csv")
    println(cluster)

    val dibas = Dibas(cluster)

    launch { dibas.run() }

    var i = 0
    while (true) {
        i++
        delay(1_000L)

        val task = Task {
            println("I'm task $i")
            delay((100..3_000).random().toLong())
            Result("I'm result $i")
        }

        val result = dibas.resolve(task)
        println(result)
    }
}