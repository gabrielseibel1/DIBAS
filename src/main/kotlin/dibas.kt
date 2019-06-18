import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.selects.select

data class Task(val request: String, val response: String)

suspend fun dibas(
    taskProducer: suspend (todo: SendChannel<Task>) -> Unit,
    taskProcessor: suspend (Task) -> Task,
    taskConsumer: suspend (done: ReceiveChannel<Task>) -> Unit
) {
    val todo = Channel<Task>(Channel.UNLIMITED)
    val done = Channel<Task>()

    var load = 0

    //will finish only when the nested launches finish
    coroutineScope {

        launch { taskProducer(todo) }
        launch { taskConsumer(done) }

        //launch a coroutine that selects receives on channels
        launch {
            while (true) {
                select<Unit> {

                    todo.onReceive { task ->

                        println("Doing $task. Load is now ${++load} ...")

                        launch {
                            val processed = taskProcessor(task)
                            done.send(processed)
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

@ExperimentalCoroutinesApi
fun main() = runBlocking {
    dibas(
        ::taskProducer,
        ::taskProcessor,
        ::taskConsumer
    )
}

suspend fun taskProcessor(task: Task): Task {
    delay((100..3000).random().toLong())
    return task.copy(response = "done")
}

//produce sends tasks to the "to do" chanel
suspend fun taskProducer(todo: SendChannel<Task>) {
    var i = 0
    while (true) {
        delay(250L)

        val task = Task("${i++}", "")
        println("Produced $task")
        todo.send(task)
    }
}

//consume receives tasks from the "done" channel
suspend fun taskConsumer(done: ReceiveChannel<Task>) {
    for (task in done) {
        println("Consumed $task\n\n")
    }
}
