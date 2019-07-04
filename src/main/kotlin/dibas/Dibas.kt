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
                println("Delegating to ${neighbor.node}")
                return sender.delegate(task, neighbor.node)
            }

            //execute task locally
            println("${cluster.hostNode} calculating result ...")
            localUpdates.send(LocalLoadUpdate.INC)
            val result = task.toResult.invoke()
            localUpdates.send(LocalLoadUpdate.DEC)
            println("${cluster.hostNode} obtained $result")
            return result
        }

        println("Starting server ...")
        receiveTasksAndLoads(::doOrDelegate, loadsFromNeighbors)
        println("Started server.")


        //instantiate N channels (one for each nbr) and pass them to this method
        sender.sendUpdates(mapa de vizinhos pra channels)

        coroutineScope {
            //only one channel will be selected at a time (synchronized)
            while (true) select<Unit> {
                loadsFromNeighbors.onReceive {
                    println("Received $it")
                    cluster.load[it.node] = it.load
                }
                localUpdates.onReceive {
                    println("Received $it")
                    load = it.update(load)

                    //send NodeLoad(cluster.hostNode, load) to each channel that sender is listening to
                    //sender will be listening to each chan and will forward it to N websockets

                }
            }
        }
    }

    @KtorExperimentalAPI
    suspend fun broadcastUpdate(sender: Sender, cluster: Cluster, load: Int) {
        val nodeLoad = NodeLoad(cluster.hostNode, load)
        println("Broadcasting $nodeLoad")
        cluster.neighbors.forEach { destination ->
            sender.sendUpdate(nodeLoad, destination)
        }
        println("Broadcasted $nodeLoad")
    }

    private companion object {
        //threshold of load difference to consider that a delegation is worth doing
        const val threshold = 5
    }

}
