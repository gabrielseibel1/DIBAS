package dibas

import io.ktor.client.HttpClient
import io.ktor.client.features.websocket.WebSockets
import io.ktor.client.features.websocket.ws
import io.ktor.http.HttpMethod
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.readBytes
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

@KtorExperimentalAPI
class Sender(cluster: Cluster, private val logger: Logger = ThreadAwareLogger()) {

    private val client = HttpClient {
        install(WebSockets)
    }

    private val updateChannels =
        cluster.graph[cluster.hostNode]?.map { Pair(it, Channel<NodeLoad>(Channel.UNLIMITED)) }.orEmpty().toMap()

    fun broadcastUpdate(update: NodeLoad) {
        updateChannels.forEach { (_, chan) ->
            CoroutineScope(Dispatchers.Default).launch {
                chan.send(update)
            }
        }
    }

    fun startWebSockets() {
        updateChannels.forEach { (destination, channel) ->
            CoroutineScope(Dispatchers.Default).launch {
                client.ws(
                    method = HttpMethod.Get,
                    host = destination.ip,
                    port = 8080, path = "/loads"
                ) {
                    for (update in channel) {
                        send(Frame.Binary(true, update.toBytes()))
                    }
                    close() //is this needed?
                }
            }
        }
    }

    suspend fun delegate(
        task: Task,
        node: Node
    ): Result {

        //wait result that websocket will send
        val resultChannel = Channel<Result>(1)

        log("delegating task to $node")
        client.ws(
            method = HttpMethod.Get,
            host = node.ip,
            port = 8080, path = "/tasks"
        ) {

            log("sending task via WS ...")
            send(Frame.Binary(true, task.toBytes()))

            log("waiting for result via WS ...")
            val frame = incoming.receive() as Frame.Binary

            val r = frame.readBytes().to<Result>()
            log("serialized $r")

            log("sending result to aux channel ...")
            resultChannel.send(r)

            close() //this seems to be important
        }

        log("receiving result from aux channel ...")
        val result: Result = resultChannel.receive()

        log("returning $result from WS.")
        return result
    }

    private fun log(s: String) {
        logger.log(s)
    }
}