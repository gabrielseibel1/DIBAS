package dibas

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.selects.select
import java.util.*

//threshold of load difference to consider that a delegation is worth doing
const val threshold = 0

data class Task(val content: String)
data class Result(val content: String)
data class RemoteLoadUpdate(val nodeId: String, val load: Int)
data class Delegation(val task: Task, val nodeId: String)

fun String.toLoadUpdate(): RemoteLoadUpdate = TODO()

enum class LocalLoadUpdate {
    INC {
        override fun update(load: Int): Int = load + 1
    },
    DEC {
        override fun update(load: Int): Int = load - 1
    };

    abstract fun update(load: Int): Int
}

fun TreeMap<Int, String>.from(cluster: Cluster): TreeMap<Int, String> {
    TODO("put all neighbors of localhost ip")
}

suspend fun dibas(
    cluster: Cluster,
    taskProducer: suspend (todo: SendChannel<Task>) -> Unit,
    taskResult: suspend (Task) -> Result,
    resultConsumer: suspend (done: ReceiveChannel<Result>) -> Unit
) {
    val todo = Channel<Task>(Channel.UNLIMITED)
    val done = Channel<Result>()
    val delegatedTodo = Channel<Task>(Channel.UNLIMITED)
    val delegatedDone = Channel<Result>()
    val remoteUpdates = Channel<RemoteLoadUpdate>(Channel.UNLIMITED)
    val localUpdates = Channel<LocalLoadUpdate>(Channel.UNLIMITED)
    val toDelegate = Channel<Delegation>(Channel.UNLIMITED)
    val neighborsLoads = TreeMap<Int, String>().from(cluster)
    var load = 0

    suspend fun doOrDelegate(task: Task, results: SendChannel<Result>) {
        //see if there is neighbor to delegate task to
        val neighbor = neighborsLoads.firstEntry()
        if (neighbor.key + threshold < load) {
            toDelegate.send(Delegation(task, neighbor.value))
            return
        }

        //execute task
        localUpdates.send(LocalLoadUpdate.INC)
        val result = taskResult(task)
        results.send(result)
        localUpdates.send(LocalLoadUpdate.DEC)
    }

    //will finish only when the nested launches finish
    coroutineScope {
        launch { taskProducer(todo) }
        launch { resultConsumer(done) }
        launch { receiveDelegationsAndUpdates(delegatedTodo, delegatedDone, remoteUpdates) }
        launch {
            while (true) select<Unit> {
                todo.onReceive {
                    launch { doOrDelegate(it, done) }
                }
                delegatedTodo.onReceive {
                    launch { doOrDelegate(it, delegatedDone) }
                }
                remoteUpdates.onReceive {
                    neighborsLoads[it.load] = it.nodeId
                }
                localUpdates.onReceive {
                    load = it.update(load)
                }
            }
        }
    }

    todo.close()
    done.close()
}

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
