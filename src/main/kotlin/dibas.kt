import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.selects.select


data class Task(val request: String, val response: String)

@ExperimentalCoroutinesApi
suspend fun dibas(produceAndConsume: suspend (todo: SendChannel<Task>, done: ReceiveChannel<Task>) -> Unit) {
    val todo = Channel<Task>()
    val done = Channel<Task>()

    //will finish only when the nested launches finish
    coroutineScope {

        //launch producer and consumer
        launch {
            produceAndConsume(todo, done)
        }

        //launch a coroutine that selects receives on channels
        launch {
            while (true) {
                println("Selecting ...")
                select<Unit> {

                    //clusterRequest.onReceive { ... }

                    todo.onReceive { task ->
                        println("Doing $task ...")
                        done.send(task.copy(response = "done"))
                    }
                }
            }
        }
    }


    println("Dibas finished")
}

@ExperimentalCoroutinesApi
suspend fun produceAndConsume(todo: SendChannel<Task>, done: ReceiveChannel<Task>) {

    //will finish only when the nested launches finish
    coroutineScope {
        //launch producer
        launch {
            var i = 0
            while (true) {
                delay(1000L)

                println("Producing ...")
                todo.send(Task("Task ${i++}", ""))
            }
        }

        //launch consumer
        launch {
            println("Consuming ...")
            done.consumeEach {
                println("Main received ${it.request}: ${it.response}")
            }
        }
    }
}

@ExperimentalCoroutinesApi
fun main() = runBlocking {

    dibas { todo, done ->
        produceAndConsume(todo, done)
    }

    //will execute after dibas finishes
    println("Main finished")
}