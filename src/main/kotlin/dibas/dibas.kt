package dibas

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.selects.select
import java.util.PriorityQueue

//threshold of load difference to consider that a delegation is worth doing
const val threshold = 0

data class Task(val content: String)
data class Result(val content: String)
data class NodeLoad(val node: Node, val load: Int)
data class DelegationTask(val task: Task, val nodeId: String)
data class DelegationResult(val result: Result, val nodeId: String)

fun String.toLoadUpdate(): NodeLoad = TODO()

enum class LocalLoadUpdate {
    INC {
        override fun update(load: Int): Int = load + 1
    },
    DEC {
        override fun update(load: Int): Int = load - 1
    };

    abstract fun update(load: Int): Int
}

fun priorityQueueOf(cluster: Cluster): PriorityQueue<NodeLoad> {
    //put all neighbors of localhost with 0 tasks
    val q = PriorityQueue<NodeLoad>(
        Comparator { o1, o2 -> o1.load - o2.load }
    )
    cluster.neighbors.forEach { node ->
        q.add(NodeLoad(node, 0))
    }
    return q
}

suspend fun dibas(
    cluster: Cluster,
    taskProducer: suspend (todo: SendChannel<Task>) -> Unit,
    taskResult: suspend (Task) -> Result,
    resultConsumer: suspend (done: ReceiveChannel<Result>) -> Unit
) {
    //local tasks and results
    val todo = Channel<Task>(Channel.UNLIMITED)
    val done = Channel<Result>()

    //tasks and results delegated FROM others
    val receivedDelegationsTasks = Channel<Task>(Channel.UNLIMITED)
    val receivedDelegationsResults = Channel<Result>()

    //tasks and results delegated TO others
    val sentDelegationsTasks = Channel<DelegationTask>(Channel.UNLIMITED)
    val sentDelegationsResults = Channel<DelegationResult>(Channel.UNLIMITED)

    //load updates of neighbors or of this node
    val remoteUpdates = Channel<NodeLoad>(Channel.UNLIMITED)
    val localUpdates = Channel<LocalLoadUpdate>(Channel.UNLIMITED)

    //ordered loads of each neighbor and of this node
    val neighborsLoads = priorityQueueOf(cluster)
    var load = 0

    suspend fun doOrDelegate(task: Task, results: SendChannel<Result>) {
        val neighbor = neighborsLoads.poll()
        if (neighbor != null && neighbor.load + threshold < load) {
            //delegate execution of task to neighbor
            sentDelegationsTasks.send(DelegationTask(task, neighbor.node.ip))
            val delegationResult = sentDelegationsResults.receive()
            results.send(delegationResult.result)

        } else {
            //execute task locally
            localUpdates.send(LocalLoadUpdate.INC)
            val result = taskResult(task)
            results.send(result)
            localUpdates.send(LocalLoadUpdate.DEC)
        }
    }

    //will finish only when the nested launches finish
    coroutineScope {
        launch { taskProducer(todo) }
        launch { resultConsumer(done) }
        launch { receiveDelegationsAndUpdates(receivedDelegationsTasks, receivedDelegationsResults, remoteUpdates) }
        launch { sendDelegations(sentDelegationsTasks) }
        launch {
            while (true) select<Unit> {
                todo.onReceive {
                    launch { doOrDelegate(it, done) }
                }
                receivedDelegationsTasks.onReceive {
                    launch { doOrDelegate(it, receivedDelegationsResults) }
                }
                remoteUpdates.onReceive {
                    neighborsLoads.add(it)
                }
                localUpdates.onReceive {
                    load = it.update(load)
                }
            }
        }
    }

    todo.close()
    done.close()
    receivedDelegationsTasks.close()
    receivedDelegationsResults.close()
    remoteUpdates.close()
    localUpdates.close()
    sentDelegationsTasks.close()
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
