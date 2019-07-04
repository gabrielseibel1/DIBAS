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
class Sender {

    private val client = HttpClient {
        install(WebSockets)
    }

    fun sendUpdates(destinationsChannels: Map<Node, Channel<NodeLoad>>) {
        destinationsChannels.forEach { (destination, channel) ->
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

    suspend fun sendUpdate(update: NodeLoad, node: Node) {
        client.ws(
            method = HttpMethod.Get,
            host = node.ip,
            port = 8080, path = "/loads"
        ) {
            send(Frame.Binary(true, update.toBytes()))
        }
    }

    suspend fun delegate(
        task: Task,
        node: Node
    ): Result {

        //wait result that websocket will send
        val resultChannel = Channel<Result>(1)

        println("Delegating task to $node")
        client.ws(
            method = HttpMethod.Get,
            host = node.ip,
            port = 8080, path = "/tasks"
        ) {

            println("Sending task via WS ...")
            send(Frame.Binary(true, task.toBytes()))

            println("Waiting for result via WS ...")
            val frame = incoming.receive() as Frame.Binary

            val r = frame.readBytes().to<Result>()
            println("Serialized $r")

            println("Sending result to aux channel ...")
            resultChannel.send(r)

            close() //this seems to be important
        }

        println("Receiving result from aux channel ...")
        val result: Result = resultChannel.receive()

        println("Returning $result from WS.")
        return result
    }
}