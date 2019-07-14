package dibas

import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.selects.select

class Dibas(private val cluster: Cluster, private val logger: Logger = ThreadAwareLogger()) {

    @KtorExperimentalAPI
    private val sender = Sender(cluster, logger)

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
            //attempt to delegate task
            val neighbor = cluster.lessBusyNeighbor()
            if (neighbor.load + threshold < load) {
                log("delegating to ${neighbor.node}")
                return sender.delegate(task, neighbor.node)
            }

            //execute task locally
            log("${cluster.hostNode} calculating result ...")
            localUpdates.send(LocalLoadUpdate.INC)
            val result = task.toResult.invoke()
            localUpdates.send(LocalLoadUpdate.DEC)
            log("${cluster.hostNode} obtained $result")
            return result
        }

        receiveTasksAndLoads(::doOrDelegate, loadsFromNeighbors)
        log("started server")

        sender.startWebSockets()
        log("started client")

        coroutineScope {
            //only one channel will be selected at a time (synchronized)
            while (true) select<Unit> {
                loadsFromNeighbors.onReceive {
                    cluster.load[it.node] = it.load
                    log("updated $it")
                }
                localUpdates.onReceive {
                    log("broadcasting $it")
                    load = it.update(load)
                    sender.broadcastUpdate(NodeLoad(cluster.hostNode, load))
                }
            }
        }
    }

    private fun log(s: String) {
        logger.log(s)
    }

    private companion object {
        //threshold of load difference to consider that a delegation is worth doing
        const val threshold = 5
    }

}
