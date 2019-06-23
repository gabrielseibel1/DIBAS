package dibas

import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@KtorExperimentalAPI
fun main(): Unit = runBlocking {

    val sender = Sender()
    val cluster = clusterFromFile("../resources/config/cluster.csv")

    launch { dibas(cluster) }

    var i = 0
    while (true) {
        i++
        delay(1_000L)

        val task = Task {
            println("I'm task $i")
            delay((100..3_000).random().toLong())
            Result("I'm result $i")
        }

        val result = sender.delegate(task, cluster.hostNode)
        println(result)
    }
}