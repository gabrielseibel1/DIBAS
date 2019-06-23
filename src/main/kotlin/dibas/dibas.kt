package dibas

import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.selects.select

//threshold of load difference to consider that a delegation is worth doing
const val threshold = 0

@KtorExperimentalAPI
suspend fun dibas(
    cluster: Cluster
) {
    val loadsFromNeighbors = Channel<NodeLoad>(Channel.UNLIMITED)
    val localUpdates = Channel<LocalLoadUpdate>(Channel.UNLIMITED)
    var load = 0

    val sender = Sender()

    suspend fun doOrDelegate(task: Task): Result {
        val neighbor = cluster.lessBusyNeighbor()
        if (neighbor.load + threshold < load) {
            return sender.delegate(task, neighbor.node)
        }

        //execute task locally
        localUpdates.send(LocalLoadUpdate.INC)
        val result = task.toResult.invoke()
        localUpdates.send(LocalLoadUpdate.DEC)
        return result
    }

    receiveTasksAndUpdates(::doOrDelegate, loadsFromNeighbors)

    coroutineScope {
        //only one channel will be selected at a time (synchronized)
        while (true) select<Unit> {
            loadsFromNeighbors.onReceive {
                cluster.load[it.node] = it.load
            }
            localUpdates.onReceive {
                load = it.update(load)
                launch { broadcastUpdate(sender, cluster, load) }
            }
        }
    }
}

@KtorExperimentalAPI
fun CoroutineScope.broadcastUpdate(sender: Sender, cluster: Cluster, load: Int) {
    cluster.neighbors.forEach { destination ->
        launch {
            sender.sendUpdate(NodeLoad(cluster.hostNode, load), destination)
        }
    }
}
