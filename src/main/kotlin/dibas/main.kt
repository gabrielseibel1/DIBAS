package dibas

import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    dibas(
        clusterFromFile("../resources/config/cluster.csv"),
        ::taskProducer,
        ::taskResult,
        ::resultConsumer
    )
}

//dibas.taskResult takes a dibas.Task and returns its dibas.Result
suspend fun taskResult(task: Task): Result {
    delay(1500L)
    return Result(task,"done")
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