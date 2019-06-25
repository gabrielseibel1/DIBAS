package dibas

import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.selects.select

class Dibas(private val cluster: Cluster) {

    @KtorExperimentalAPI
    private val sender = Sender()

    @KtorExperimentalAPI
    suspend fun resolve(task: Task): Result {
        return sender.delegate(task, cluster.hostNode)
    }

    @KtorExperimentalAPI
    suspend fun run() {
        val loadsFromNeighbors = Channel<NodeLoad>(Channel.UNLIMITED)
        val localUpdates = Channel<LocalLoadUpdate>(Channel.UNLIMITED)
        var load = 0

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
    private fun CoroutineScope.broadcastUpdate(sender: Sender, cluster: Cluster, load: Int) {
        cluster.neighbors.forEach { destination ->
            launch {
                sender.sendUpdate(NodeLoad(cluster.hostNode, load), destination)
            }
        }
    }

    private companion object {
        //threshold of load difference to consider that a delegation is worth doing
        const val threshold = 0
    }

}
