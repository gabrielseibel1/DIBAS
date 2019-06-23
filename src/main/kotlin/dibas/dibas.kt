package dibas

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.selects.select

//threshold of load difference to consider that a delegation is worth doing
const val threshold = 0

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
    val nodesLoads = sortedNodeLoadsOf(cluster)
    var load = 0

    suspend fun doOrDelegate(task: Task, results: SendChannel<Result>) {
        val neighbor = nodesLoads.poll()
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
                    nodesLoads.add(it)
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
