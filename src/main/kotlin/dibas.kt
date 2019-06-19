import io.ktor.application.Application
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.websocket.WebSockets
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.selects.select
import java.time.Duration

data class Task(val content: String)
data class Result(val content: String)

suspend fun dibas(
    cluster: Cluster,
    taskProducer: suspend (todo: SendChannel<Task>) -> Unit,
    taskResult: suspend (Task) -> Result,
    resultConsumer: suspend (done: ReceiveChannel<Result>) -> Unit
) {
    val todo = Channel<Task>(Channel.UNLIMITED)
    val done = Channel<Result>()

    var load = 0

    val server = embeddedServer(Netty, 8080) {
        install(WebSockets) {
            pingPeriod = Duration.ofSeconds(60) // Disabled (null) by default
            timeout = Duration.ofSeconds(15)
            maxFrameSize = Long.MAX_VALUE // Disabled (max value). The connection will be closed if surpassed this length.
            masking = false
        }
    }


    //will finish only when the nested launches finish
    coroutineScope {

        launch { taskProducer(todo) }
        launch { resultConsumer(done) }

        //launch a coroutine that selects receives on channels
        launch {
            while (true) {
                select<Unit> {

                    todo.onReceive { task ->

                        println("Doing $task. Load is now ${++load} ...")

                        launch {
                            val result = taskResult(task)
                            --load
                            done.send(result)
                        }
                    }

                    //clusterRequest.onReceive { ... }
                }
            }
        }
    }

    todo.close()
    done.close()
}

fun main() = runBlocking {
    dibas(
        clusterFromFile(javaClass.classLoader.getResource("../resources/config/cluster.csv").toString()),
        ::taskProducer,
        ::taskResult,
        ::resultConsumer
    )
}

//taskResult takes a Task and returns its Result
suspend fun taskResult(task: Task): Result {
    delay(1500L)
    return Result("$task done")
}

//produce sends tasks to the "to do" chanel
suspend fun taskProducer(todo: SendChannel<Task>) {
    var i = 0
    while (true) {
        delay((100..3000).random().toLong())

        val task = Task("${i++}")
        println("Produced $task")
        todo.send(task)
    }
}

//consume receives tasks from the "done" channel
suspend fun resultConsumer(done: ReceiveChannel<Result>) {
    for (result in done) {
        println("Consumed $result\n\n")
    }
}
