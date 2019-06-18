import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.selects.select

data class Task(val request: String, val response: String)

suspend fun dibas(
    taskProducer: suspend (todo: SendChannel<Task>) -> Unit,
    taskConsumer: suspend (done: ReceiveChannel<Task>) -> Unit
) {
    val todo = Channel<Task>()
    val done = Channel<Task>()

    //will finish only when the nested launches finish
    coroutineScope {

        launch { taskProducer(todo) }
        launch { taskConsumer(done) }

        //launch a coroutine that selects receives on channels
        launch {
            while (true) {
                println("Selecting ...")
                select<Unit> {
                    todo.onReceive { task ->
                        println("Doing $task ...")
                        done.send(task.copy(response = "done"))
                    }
                    //clusterRequest.onReceive { ... }
                }
            }
        }
    }
}

@ExperimentalCoroutinesApi
fun main() = runBlocking {
    dibas(::taskProducer, ::taskConsumer)
}

//produce sends tasks to the "to do" chanel
suspend fun taskProducer(todo: SendChannel<Task>) {
    var i = 0
    while (true) {
        delay(1000L)

        val task = Task("${i++}", "")
        println("Produced $task")
        todo.send(task)
    }
}

//consume receives tasks from the "done" channel
suspend fun taskConsumer(done: ReceiveChannel<Task>) {
    for (task in done) {
        println("Consumed $task")
    }
}
