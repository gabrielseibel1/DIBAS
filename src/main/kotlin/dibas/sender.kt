package dibas

import io.ktor.client.HttpClient
import io.ktor.client.features.websocket.WebSockets
import io.ktor.client.features.websocket.ws
import io.ktor.http.HttpMethod
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.readText
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.channels.Channel

@KtorExperimentalAPI
class Sender {

    private val client = HttpClient {
        install(WebSockets)
    }

    suspend fun sendUpdate(update: NodeLoad, node: Node) {
        client.ws(
            method = HttpMethod.Get,
            host = node.ip,
            port = 8080, path = "/loads"
        ) { send(Frame.Text(update.toString())) }
    }

    suspend fun delegate(
        task: Task,
        node: Node
    ): Result {

        //wait result that websocket will send
        val resultChannel = Channel<Result>(Channel.RENDEZVOUS)
        val result = resultChannel.receive()

        client.ws(
            method = HttpMethod.Get,
            host = node.ip,
            port = 8080, path = "/tasks"
        ) {
            send(Frame.Text(task.toString()))
            val frame = incoming.receive() as Frame.Text
            resultChannel.send(frame.readText().toResult())
        }

        return result
    }
}