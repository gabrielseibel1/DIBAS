package dibas

import io.ktor.client.HttpClient
import io.ktor.client.features.websocket.WebSockets
import io.ktor.client.features.websocket.ws
import io.ktor.http.HttpMethod
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.readBytes
import io.ktor.http.cio.websocket.readText
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.channels.Channel
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.ObjectOutput
import java.io.ObjectOutputStream

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
        ) {
            send(Frame.Binary(true, update.toBytes()))
        }
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

            send(Frame.Binary(true, task.toBytes()))

            val frame = incoming.receive() as Frame.Binary

            val r = frame.readBytes().to<Result>()

            resultChannel.send(r)
        }

        return result
    }
}