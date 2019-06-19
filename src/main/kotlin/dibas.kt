import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.selects.select

data class Task(val content: String)
data class Result(val content: String)

suspend fun dibas(
    taskProducer: suspend (todo: SendChannel<Task>) -> Unit,
    taskResult: suspend (Task) -> Result,
    resultConsumer: suspend (done: ReceiveChannel<Result>) -> Unit
) {
    val todo = Channel<Task>(Channel.UNLIMITED)
    val done = Channel<Result>()

    var load = 0

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
